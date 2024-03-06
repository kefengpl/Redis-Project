package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.Blog;

import java.util.List;

public interface IBlogService extends IService<Blog> {
    List<Blog> queryHotBlogs(Integer pageNo, Integer pageSize);

    Result saveBlog(Blog blog);

    Result getMyBlogs(Integer current);

    Result getBlogDetail(Long id);

    Result likeBlog(Long blogId);

    Result getBlogLikes(Long blogId);
    Result getBlogsOf(Integer current, Long userId);

    Result getBlogOfFollow(Long lastId, Integer offset);
}
