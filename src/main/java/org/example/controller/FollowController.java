package org.example.controller;

import org.example.dto.Result;
import org.example.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author 3590
 * @Date 2024/3/6 20:24
 * @Description
 */
@RestController
@RequestMapping("follow")
public class FollowController {
    @Autowired
    IFollowService followService;

    /**
     * 用于实现用户的关注功能
     */
    @PutMapping("{userId}/{isFollow}")
    public Result follow(@PathVariable Long userId, @PathVariable Boolean isFollow) {
        return followService.follow(userId, isFollow);
    }

    /**
     * 用于判断该用户是否被登录用户关注了
     */
    @GetMapping("or/not/{userId}")
    public Result isFollowing(@PathVariable Long userId) {
        return Result.ok(followService.isFollowing(userId));
    }

    @GetMapping("common/{userId}")
    public Result commonFollowing(@PathVariable Long userId) {
        return followService.commonFollowing(userId);
    }
}
