package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Blog;

import java.util.List;

public interface IBlogService extends IService<Blog> {
    List<Blog> queryHotBlogs(Integer pageNo, Integer pageSize);

}
