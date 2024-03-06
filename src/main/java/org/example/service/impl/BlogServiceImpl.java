package org.example.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.example.dto.Result;
import org.example.dto.UserDTO;
import org.example.entity.Blog;
import org.example.entity.Follow;
import org.example.entity.User;
import org.example.mapper.BlogMapper;
import org.example.mapper.FollowMapper;
import org.example.mapper.UserMapper;
import org.example.service.IBlogService;
import org.example.utils.ScrollResult;
import org.example.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.utils.RedisConstants.*;
import static org.example.utils.SystemConstants.*;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    BlogMapper blogMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    FollowMapper followMapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 分页查询，查询所有热门 blog，如果用户已经登录，则显示它是否已经点赞
     * */
    @Override
    public List<Blog> queryHotBlogs(Integer pageNo, Integer pageSize) {
        Page<Blog> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Blog::getLiked);
        blogMapper.selectPage(page, wrapper);
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            Long userId = blog.getUserId();
            User user = userMapper.selectById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            blog.setIsLike(isLikedByCurrentUser(blog.getId()));
        });
        return records;
    }

    public boolean isLikedByCurrentUser(Long blogId) {
        UserDTO currentUser = UserHolder.getUser();
        Long currentUserId;
        if (currentUser != null) { // 用户已经登录
            currentUserId = currentUser.getId();
        } else {
            currentUserId = null;
        }
        if (currentUserId == null) {
            return false;
        }
        // 注意：zset 使用前需要先检查是否是空！
        if (Boolean.FALSE.equals(redisTemplate.hasKey(BLOKE_LIKED_KEY + blogId))) {
            return false;
        }
        return redisTemplate.opsForZSet()
                .score(BLOKE_LIKED_KEY + blogId, String.valueOf(currentUserId)) != null;
    }

    /**
     * 保存博客：存入数据库的同时，也要放入关注者的信箱
     */
    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        blogMapper.insert(blog);
        List<Long> followers = getFollowersOf(userId);
        if (followers == null || followers.isEmpty()) {
            return Result.ok(blog.getId());
        }
        // 将信息推送到关注者的信箱。 记住获取当前时间戳的方式！
        for (Long followerId : followers) {
            redisTemplate.opsForZSet().add(RECEIVE_MAIL_KEY + followerId,
                    String.valueOf(blog.getId()), System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    /**
     * 获取某个用户的所有关注者 id
     * @return 关注者 id 列表；如果 userId 是空，则返回 null
     */
    public List<Long> getFollowersOf(Long userId) {
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId, userId);
        List<Follow> follows = followMapper.selectList(queryWrapper);
        return follows.stream().map(Follow::getUserId).toList();
    }

    /**
     * 获取当前登录用户的所有博客
     */
    @Override
    public Result getMyBlogs(Integer current) {
        Long userId = UserHolder.getUser().getId();
        return getBlogsOf(current, userId);
    }

    /**
     * 获取某个用户的所有博客
     */
    @Override
    public Result getBlogsOf(Integer current, Long userId) {
        if (userId == null) {
            return Result.fail("用户 id 是空");
        }
        IPage<Blog> page = new Page<>(current, DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Blog::getUserId, userId);
        IPage<Blog> myBlogsPage = blogMapper.selectPage(page, queryWrapper);
        return Result.ok(myBlogsPage.getRecords());
    }

    /**
     * 实现对关注大V的滚动分页查询
     */
    @Override
    public Result getBlogOfFollow(Long lastId, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        // 当前用户的收件箱的 zset 对应的 key，它内部存储着它关注用户发布的推文的 blog id
        String receiveMail = RECEIVE_MAIL_KEY + userId;
        // 返回由大到小排列的数组
        Set<String> stringSet = redisTemplate.opsForZSet()
                .reverseRangeByScore(receiveMail, 0, lastId, offset, FEED_PAGE_SIZE);
        if (stringSet == null || stringSet.isEmpty()) { // 它关注的博主没有任何推文
            return Result.ok();
        }
        List<Long> blogIds = stringSet.stream().map(Long::parseLong).toList();
        List<Blog> blogList = blogIds.stream().map(blogId -> (Blog) getBlogDetail(blogId).getData()).toList();
        // 随后，需要我们记录当前查询到的最后一条记录的 score，并作为 lastId 返回给前端；有多少个推文的时间戳一样，同样返回给前端，作为 offset
        Long finalScore = redisTemplate.opsForZSet()
                .score(receiveMail, String.valueOf(blogIds.get(blogIds.size() - 1))).longValue();
        int equalsToFinalCount = 0;
        for (int i = blogIds.size() - 1; i >= 0; --i) { // 倒序查找有多少个记录的 score 是 finalScore
            // bug 记录：blogIds.size() - 1 是错的，导致你的偏移量计算不对，应该是 blogIds.get(i)
            Long score = redisTemplate.opsForZSet()
                    .score(receiveMail, String.valueOf(blogIds.get(i))).longValue();
            if (finalScore.equals(score)) {
                ++equalsToFinalCount;
            }
        }
        return Result.ok(new ScrollResult(blogList, finalScore, equalsToFinalCount));
    }

    /**
     * 连接查询一个推文的详细信息，包括它作者的昵称，作者图标，是否被当前用户点赞等
     */
    @Override
    public Result getBlogDetail(Long id) {
        Blog blog = blogMapper.queryBlogDetail(id);
        blog.setIsLike(isLikedByCurrentUser((id)));
        return Result.ok(blog);
    }

    /**
     * 注意：一个用户只能点赞一次，所以应该先检测该用户是否点过赞
     */
    @Override
    public Result likeBlog(Long blogId) {
        Long userId = UserHolder.getUser().getId();
        Long isLiked = redisTemplate.execute(BLOG_LIKED_SCRIPT, new LinkedList<>(),
                BLOKE_LIKED_KEY, String.valueOf(blogId), String.valueOf(userId));
        if (isLiked == null) {
            throw new RuntimeException("查询Redis失败");
        }
        LambdaUpdateWrapper<Blog> queryWrapper = new LambdaUpdateWrapper<>();
        if (isLiked == -1) {
            // 用户已经点过赞，则需要取消点赞，即点赞数 - 1
            queryWrapper.setSql("liked = liked - 1");
        } else {
            // 用户尚未点赞，则点赞，点赞数 + 1
            queryWrapper.setSql("liked = liked + 1");
        }
        queryWrapper.eq(Blog::getId, blogId);
        blogMapper.update(null, queryWrapper);
        return Result.ok();
    }

    @Override
    public Result getBlogLikes(Long blogId) {
        // 这可能是 TreeSet，所以是有序的
        Set<String> userIds = redisTemplate.opsForZSet().range(BLOKE_LIKED_KEY + blogId, 0, 4);
        if (userIds == null || userIds.isEmpty()) {
            return Result.ok();
        }
        CharSequence idSequence = StringUtil.join(",", userIds);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 为了保证查询顺序一致性，需要使用 order by field(id, 5, 1); 这种东西
        wrapper.in(User::getId, userIds);
        wrapper.last("order by field(id, " + idSequence + ")");
        List<User> users = userMapper.selectList(wrapper);
        List<UserDTO> collect = users // 注意这里 stream API 的使用
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(collect);
    }
}
