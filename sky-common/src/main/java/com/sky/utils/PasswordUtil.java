package com.sky.utils;

import com.sky.constant.PasswordConstant;
import org.springframework.util.DigestUtils;

/**
 * @author xi
 * @create 2023/9/28- 22:23
 */
public class PasswordUtil {
    public static String encode(String password){

        password = DigestUtils.md5DigestAsHex(( password+ PasswordConstant.DEFAULT_SALT).getBytes());

        return password;
    }
}
