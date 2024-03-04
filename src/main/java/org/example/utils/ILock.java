package org.example.utils;

/**
 * @Author 3590
 * @Date 2024/3/4 21:27
 * @Description
 */
public interface ILock {
    /**
     * 尝试一次获取锁
     * @param timeoutSec 锁的超时时间
     * @return 如果获取成功返回 true，失败返回 false
     */
    public boolean tryLock(long timeoutSec);
    public void unlock();
}
