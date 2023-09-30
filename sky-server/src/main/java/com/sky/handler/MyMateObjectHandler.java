package com.sky.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * @description: 公共字段自动填充
 * @author: xi
 * @date: 2023/9/30 19:20
 * @paramType:
 * @param:
 * @return:
 **/

@Slf4j
@Configuration
public class MyMateObjectHandler implements MetaObjectHandler {
    /**
     * @description:添加时 自动填充
     * @author: xi
     * @date: 2023/9/30 19:22
     * @paramType: [org.apache.ibatis.reflection.MetaObject]
     * @param: [metaObject]
     * @return: void
     **/
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("mybatisplus公共字段填充[insert]...");
        metaObject.setValue(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.now());//创建时间
        metaObject.setValue(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.now());//修改时间
        metaObject.setValue(AutoFillConstant.SET_CREATE_USER,  BaseContext.getCurrentId());//创建人ID
        metaObject.setValue(AutoFillConstant.SET_UPDATE_USER,  BaseContext.getCurrentId());//修改人ID
    }

    /**
     * @description:更新时 自动填充
     * @author: xi 
     * @date: 2023/9/30 19:22
     * @paramType: [org.apache.ibatis.reflection.MetaObject]
     * @param: [metaObject]
     * @return: void
     **/
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("mybatisplus公共字段填充[update]...");
        metaObject.setValue(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.now());//修改时间
        metaObject.setValue(AutoFillConstant.SET_UPDATE_USER, BaseContext.getCurrentId());//修改人ID

    }
}
