package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Blog;
import org.example.entity.User;
import org.example.mapper.BlogMapper;
import org.example.mapper.UserMapper;
import org.example.service.IBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    BlogMapper blogMapper;
    @Autowired
    UserMapper userMapper;

    /**
     * 分页查询
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
        });
        return records;
    }
}
