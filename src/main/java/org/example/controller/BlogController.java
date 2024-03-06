package org.example.controller;

import org.example.dto.Result;
import org.example.entity.Blog;
import org.example.service.IBlogService;
import org.example.utils.SystemConstants;
import org.example.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

     @PostMapping
     public Result saveBlog(@RequestBody Blog blog) {
          return blogService.saveBlog(blog);
     }

     /**
      * 分页查询登录用户的 blog
      */
     @GetMapping("of/me")
     public Result getMyBlogs(@RequestParam(value = "current", defaultValue = "1") Integer current) {
          return blogService.getMyBlogs(current);
     }

     /**
      * 分页查询给定用户的 blog
      */
     @GetMapping("of/user")
     public Result getMyBlogs(@RequestParam(value = "current", defaultValue = "1") Integer current, Long id) {
          return blogService.getBlogsOf(current, id);
     }


     @GetMapping("likes/{id}")
     public Result blogLikes(@PathVariable("id") Long blogId) {
         return blogService.getBlogLikes(blogId);
     }

     @GetMapping("{id}")
     public Result blogDetail(@PathVariable Long id) {
          return blogService.getBlogDetail(id);
     }

     @PutMapping("like/{id}")
     public Result likeBlog(@PathVariable("id") Long blogId) {
          return blogService.likeBlog(blogId);
     }

     /**
      * 解释：第一次查询传入当前时间戳，偏移量默认是0.
      * 随后第一次查询后，你会把该次查询的最小时间戳(从大到小查询)返回给前端，也会把偏移量返回给前端
      * 为什么用到时间戳？因为 zset (收信箱) 的 score 是按照时间戳排序的，倒序遍历 zset 就是由大到小排序
      * 将会使用 ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count] 这个奇怪的命令进行滚动分页查询
      */
     @GetMapping("of/follow")
     public Result blogOfFollow(Long lastId,
                                @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
          return blogService.getBlogOfFollow(lastId, offset);
     }
}
