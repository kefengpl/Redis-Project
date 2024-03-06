package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static org.example.utils.SystemConstants.*;

/**
 * @Author 3590
 * @Date 2024/3/6 14:51
 * @Description 用于上传图片到 nginx 服务器
 */
@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {
    /**
     * 实现图片上传功能
     */
    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            String imageName = image.getOriginalFilename();
            image.transferTo(new File(IMAGE_UPLOAD_DIR.toFile(), imageName));
            return Result.ok("/" + imageName);
        } catch (IOException e) {
            throw new RuntimeException("上传图片失败");
        }
    }
}
