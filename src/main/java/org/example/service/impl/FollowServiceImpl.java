package org.example.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.dto.UserDTO;
import org.example.entity.Follow;
import org.example.entity.User;
import org.example.mapper.FollowMapper;
import org.example.mapper.UserMapper;
import org.example.service.IFollowService;
import org.example.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.utils.RedisConstants.*;

/**
 * @Author 3590
 * @Date 2024/3/6 20:28
 * @Description
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    FollowMapper followMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 关注：就是添加一条记录到 follow 表中
     * 为了防止恶意行为，应该使用分布式锁，防止一个用户的关注记录过多。
     */
    @Override
    public Result follow(Long userId, Boolean isFollow) {
        boolean following = isFollowing(userId);
        if ((following && isFollow) || (!following && !isFollow)) {
            return Result.ok();
        }
        Long loginUserId = UserHolder.getUser().getId();
        if (!isFollow) { // 取关操作
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId, loginUserId).eq(Follow::getFollowUserId, userId);
            followMapper.delete(queryWrapper);
            // 先删除数据库，再删除缓存
            redisTemplate.opsForSet().remove(USER_FOLLOWING_KEY + loginUserId, String.valueOf(userId));
            return Result.ok();
        }
        Follow follow = new Follow(); // 关注操作
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(userId);
        followMapper.insert(follow);
        // 写入数据库后再写入 redis 缓存
        redisTemplate.opsForSet().add(USER_FOLLOWING_KEY + loginUserId, String.valueOf(userId));
        return Result.ok();
    }

    @Override
    public boolean isFollowing(Long userId) {
        Long loginUserId = UserHolder.getUser().getId();
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getUserId, loginUserId).eq(Follow::getFollowUserId, userId);
        return followMapper.exists(queryWrapper);
    }

    /**
     * 获取共同关注：redis 的两个集合取交集
     */
    @Override
    public Result commonFollowing(Long userId) {
        String loginUserKey = USER_FOLLOWING_KEY + UserHolder.getUser().getId();
        String otherUserKey = USER_FOLLOWING_KEY + userId;
        Set<String> intersect = redisTemplate.opsForSet().intersect(loginUserKey, otherUserKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok();
        }
        List<Long> userIds = intersect.stream().map(Long::parseLong).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        List<UserDTO> collect = users.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(collect);
    }

}
