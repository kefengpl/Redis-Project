package org.example.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SystemConstants {
    public static final Path IMAGE_UPLOAD_DIR = Paths.get("D:", "study",
            "Git Repo", "Redis", "nginx-1.18.0", "html", "hmdp", "imgs");
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int USER_NICK_NAME_LENGTH = 10;
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
    public static final String VERIFY_CODE_NAME = "verifyCode";
    public static final Long POLLING_INTERVAL = 1000L;
}
