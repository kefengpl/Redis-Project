package TestClass;

import cn.hutool.core.lang.UUID;
import org.example.Main;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.example.utils.LocalDateTimeAdapter;
import org.example.utils.RedisIdWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author 3590
 * @Date 2024/2/20 23:33
 * @Description 如果 mapper 无法注入，请使用下列代码，以在测试框架中启动 SpringBoot
 */
@SpringBootTest(classes = {Main.class})
@RunWith(SpringRunner.class)
public class MyTest {
    @Autowired
    ShopMapper shopMapper;
    @Autowired
    RedisIdWorker idWorker;

    @Test
    public void test() {
        Shop shop = shopMapper.selectById(2);
        System.out.println(shop);
    }

    @Test
    public void test1() {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        // 这一行将LocalDateTime对象转换为自1970年1月1日0时0分0秒（UTC）以来的秒数
        long epochSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
        System.out.println("epochSecond = " + epochSecond); // 返回的是
    }

    @Test
    public void test2() {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        Runnable runnable = () -> System.out.println(idWorker.nextId("load"));
        for (int i = 0; i < 500; ++i)
            executorService.execute(runnable);
    }

    @Test
    public void test3() {
        LocalDateTime time1 = LocalDateTime.of(2024, 3, 2, 15, 20, 35);
        LocalDateTime time2 = LocalDateTime.of(2024, 4, 2, 15, 20, 35);
        boolean b = time1.compareTo(LocalDateTime.now()) <= 0 && time2.compareTo(LocalDateTime.now()) >= 0;
        System.out.println(b);
    }

    @Test
    public void test4() {
        UUID.randomUUID().toString(true);
    }
}
