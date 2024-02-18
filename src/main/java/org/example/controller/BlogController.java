package org.example.controller;

import java.util.List;

import org.example.dto.Result;
import org.example.entity.Blog;
import org.example.service.IBlogService;
import org.example.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author 3590
 * @Date 2024/1/29 2:59
 * @Description
 */
@RestController
@RequestMapping("blog")
public class BlogController {
     @Autowired
     IBlogService blogService;

     @GetMapping("hot")
     public Result getHotBlogs(@RequestParam(defaultValue = "1") Integer current) {
          return Result.ok(blogService.queryHotBlogs(current, SystemConstants.MAX_PAGE_SIZE));
     }
}
