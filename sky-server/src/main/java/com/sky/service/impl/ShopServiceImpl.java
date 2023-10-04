package com.sky.service.impl;

import com.sky.service.ShopService;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author xi
 * @create 2023/10/4- 19:32
 */
@Service
public class ShopServiceImpl implements ShopService {
    @Autowired
    RedisTemplate redisTemplate;
    @Override
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set("SHOP_STATUS",status);
    }

    @Override
    public Integer getStatus() {
        return (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
    }
}
