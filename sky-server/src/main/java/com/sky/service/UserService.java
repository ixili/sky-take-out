package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserLoginVO;


/**
 * 用户信息(User)表服务接口
 *
 * @author lixi
 * @since 2023-10-05 21:14:25
 */
public interface UserService extends IService<User> {

    UserLoginVO login(UserLoginDTO userLoginDTO);
}

