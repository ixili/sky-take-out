package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;


/**
 * 用户信息(User)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-05 21:14:24
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

}

