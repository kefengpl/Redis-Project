package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.Follow;

/**
 * @Author 3590
 * @Date 2024/3/6 20:27
 * @Description
 */
public interface IFollowService extends IService<Follow> {
    /**
     * @param isFollow true 表示你要关注这个用户，false 表示你要取关这个用户
     */
    Result follow(Long userId, Boolean isFollow);

    boolean isFollowing(Long userId);

    Result commonFollowing(Long userId);
}
