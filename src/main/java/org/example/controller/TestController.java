package org.example.controller;

import org.example.dto.Result;
import org.example.utils.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author 3590
 * @Date 2024/2/28 1:18
 * @Description
 */
@RestController
@RequestMapping("test")
public class TestController {
    @Autowired
    RedisIdWorker idWorker;
    @GetMapping("redisIdTest")
    public Result test1() { // 提示：线程池使用
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        Runnable runnable = () -> System.out.println(idWorker.nextId("load"));
        for (int i = 0; i < 500; ++i) {
            executorService.execute(runnable);
        }
        return Result.ok();
    }


}
