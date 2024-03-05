package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.dto.UserDTO;
import org.example.entity.SeckillVoucher;
import org.example.entity.VoucherOrder;
import org.example.mapper.SeckillVoucherMapper;
import org.example.mapper.VoucherOrderMapper;
import org.example.service.IVoucherOrderService;
import org.example.utils.RedisIdWorker;
import org.example.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.utils.RedisConstants.*;

/**
 * @Description
 * 提示：事务的传播行为。@Transactional 注解的作用在于：会为当前类生成代理对象，@Transactional 的方法在调用时会被特殊对待。
 * 比如通过如下结构调用：
 * try {
 *    connection.setAutoCommit(false);
 *    call your method(); // 或许是通过反射调用的
 *    connection.commit();
 * } catch (Expection e) {
 *    connection.rollback();
 * } finally {
 *    connection.close();
 * }
 * 而当你在一个没有 @Transactional 注解的方法中调用 有  @Transactional 的方法，它在执行外层函数的时候不会走上面的 try catch 结构，
 * 从而，也就不会应用任何事务。所以，下面的类中，你只能在 seckillVoucher 添加事务注解。只在 makeOrder 中添加无效，除非你在本类外直接调用 makeOrder
 * 注意：在任何给定时间点，同一个连接上只能有一个活动事务。
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    VoucherOrderMapper voucherOrderMapper;
    @Autowired
    SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    RedisIdWorker redisIdWorker;
    @Autowired
    RedissonClient redissonClient; // 利用第三方提供的分布式锁 API
    @Autowired
    StringRedisTemplate redisTemplate;
    // 它可以为所有线程共用。因为这是 Service 类本身是单例。
    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 创建线程池，同样，由于 spring 默认是单例模式，所以它会被所有请求和线程共享
    private static final ExecutorService ORDER_EXECUTORS = Executors.newFixedThreadPool(1);
    IVoucherOrderService proxyObject = null;// 获取当前类的代理对象
    // 利用阻塞队列，所以通过队列获取订单信息即可。只开一个线程，处理所有生成订单的请求
    Runnable runnable = () -> {
        while (true) {
            try {
                // 尝试从阻塞队列获取订单信息
                VoucherOrder voucherOrder = orderTasks.take();
                proxyObject.makeOrder(voucherOrder); // 根据 voucherOrder 生成订单
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    };

    {
        ORDER_EXECUTORS.execute(runnable); // 一开始就启动这个线程，或者使用 @PostConstruct
    }

    @Override
    @Deprecated
    public Result seckillVoucher(Long voucherId) {
        // 查询 id
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
        // 判断该券是否在秒杀
        if (!seckillVoucher.isSeckilling()) {
            return Result.fail("不在秒杀期间");
        }
        // 如果该优惠券秒杀，当库存>0，就下单
        if (seckillVoucher.getStock() <= 0) {
            return Result.fail("库存已清空");
        }
        // 注意：用户信息存放在 ThreadLocal 中
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        Long orderId = null;

        /*redisLock = new SimpleRedisLock(redisTemplate, "order:" + userId);
        if (!redisLock.tryLock(1000)) {
            return Result.fail("您已经下过单了，或者您正在下单");
        }*/
        RLock lock = redissonClient.getLock("lock:order" + userId);
        if (!lock.tryLock()) { // 空参表示默认不等待
            return Result.fail("您已经下过单了，或者您正在下单");
        }
        try {
            // 只有下面这三行代码，还是无法解决一人一单问题。因为第一个人判断通过后，尚未写入数据库，第二个人就进来了。
            if (hasMadeOrder(userId, voucherId)) {
                return Result.fail("您已经下过单了");
            }
            // 提示：如果希望上述代码真的保证只有一个是 false，其它都是 true，那么必须保证其它线程执行 if 语句是在数据库写入之后
            // 此外，为了读取提交的数据，锁应该把整个和事务相关的函数锁起来。为此，你需要 Aspectj，AOP 获得代理对象
            // 由于 Aspectj 生成代理对象：如果该类实现了接口，那么代理对象是接口的另一个实现，所以需要在接口处声明 makeOrder(添加事务的函数)
            IVoucherOrderService proxyObject = (IVoucherOrderService) AopContext.currentProxy();// 获取当前类的代理对象
            // 扣减库存，生成订单
            orderId = proxyObject.makeOrder(seckillVoucher, userId); // 这就是添加了事务的 makeOrder，此时，该函数执行完毕必然保证数据库写入且提交
        } finally {
            lock.unlock(); // 释放锁
        }

        if (orderId == null) {
            return Result.fail("库存已清空");
        }
        return Result.ok(orderId);
    }

    /**
     * 实现 redis 版本的优惠券秒杀
     */
    @Override
    public Result redisSeckillVoucher(Long voucherId) {
        // 1. 执行 lua 脚本(note: 秒杀券必定在 redis 数据库中了)：传入参数，执行脚本
        List<String> redisKeys = new LinkedList<>();
        redisKeys.add(SECKILL_STOCK_KEY + voucherId);
        redisKeys.add(SECKILL_ORDER_KEY + voucherId);
        Long userId = UserHolder.getUser().getId();
        Long execute = redisTemplate.execute(VOUCHER_SECKILL_SCRIPT, redisKeys, String.valueOf(userId));
        if (execute == null) {
            return Result.fail("服务器异常，无法进行秒杀");
        }
        if (execute != 0) {
            return execute.equals(2L) ? Result.fail("您已经下过单了") :
                    Result.fail("秒杀券库存不足");
        }
        // 其它情况：需要进行下单了！则把用户 id，order id，优惠券 id 存入阻塞队列
        long orderId = redisIdWorker.nextId(SECKILL_STOCK_KEY);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        if (proxyObject == null) {
            proxyObject = (IVoucherOrderService) AopContext.currentProxy();
            System.out.println("代理对象创建成功");
        }
        // 将订单存入阻塞队列，让它异步更新
        orderTasks.add(voucherOrder);
        return Result.ok(orderId);
    }

    /***
     * 检查一个用户是否已经在这种秒杀优惠券上下过单了
     * @param userId 用户 id
     * @param voucherId 该优惠券的 id
     */
    boolean hasMadeOrder(Long userId, Long voucherId) {
        LambdaQueryWrapper<VoucherOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VoucherOrder::getUserId, userId).eq(VoucherOrder::getVoucherId, voucherId);
        return voucherOrderMapper.exists(queryWrapper);
    }

    /**
     * 生成订单，减少库存。它会 seckillVoucher 方法的事务。(seckillVoucher 运行时就会创建一个事务.)
     * @param seckillVoucher 秒杀券信息
     * @param userId 用户 id
     * @return 生成的订单 id。如果库存减1失败，则删除。
     */
    public Long makeOrder(SeckillVoucher seckillVoucher, Long userId) {
        // 1. 更新库存表，库存 = 库存 - 1
        LambdaUpdateWrapper<SeckillVoucher> updateWrapper = new LambdaUpdateWrapper<>();
        // 乐观锁，更新时判断 stock > 0，比 stock = #{stock} 还要乐观
        updateWrapper.eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId()).gt(SeckillVoucher::getStock, 0);
        updateWrapper.setSql("stock = stock - 1");
        int update = seckillVoucherMapper.update(null, updateWrapper);
        if (update == 0) return null; // 去库存失败，返回 null.
        // 2. 更新订单表，插入一条新订单记录
        // 下单，设置关键信息
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId(SECKILL_STOCK_KEY));
        voucherOrder.setVoucherId(seckillVoucher.getVoucherId());
        voucherOrder.setUserId(userId);
        voucherOrderMapper.insert(voucherOrder);
        return voucherOrder.getId();
    }

    /**
     * 生成订单，减少库存。
     */
    @Transactional // 只有更新数据库需要事务，其它不需要
    public Long makeOrder(VoucherOrder voucherOrder) {
        // 1. 更新库存表，库存 = 库存 - 1
        LambdaUpdateWrapper<SeckillVoucher> updateWrapper = new LambdaUpdateWrapper<>();
        // 乐观锁，更新时判断 stock > 0，比 stock = #{stock} 还要乐观
        updateWrapper.eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId()).gt(SeckillVoucher::getStock, 0);
        updateWrapper.setSql("stock = stock - 1");
        int update = seckillVoucherMapper.update(null, updateWrapper);
        if (update == 0) return null; // 去库存失败，返回 null.
        // 2. 更新订单表，插入一条新订单记录
        voucherOrderMapper.insert(voucherOrder);
        return voucherOrder.getId();
    }
}
