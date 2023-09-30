package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @author xi
 * @create 2023/9/30- 18:46
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut( "execution( * com.sky.service.*(..)) && @annotation(com.sky.annotation.AutoFill) " )
    public void pointcut(){}

    @Before( "pointcut()" )
    public void beforeAutoFill(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill annotation = signature.getMethod().getAnnotation(com.sky.annotation.AutoFill.class);
        OperationType type = annotation.type();

        Object arg = joinPoint.getArgs()[0];

        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 反射调用方法设置 共同字段值
        if(OperationType.INSERT.equals(type)){
            Method setCreateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME);
            Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME);
            Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER);
            Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER);

            setCreateUser.invoke(arg,currentId);
            setUpdateUser.invoke(arg,currentId);
            setCreateTime.invoke(arg,now);
            setUpdateTime.invoke(arg,now);
        }else if(OperationType.UPDATE.equals(type)){
            Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME);
            Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER);

            setUpdateUser.invoke(arg,currentId);
            setUpdateTime.invoke(arg,now);
        }
    }
}
