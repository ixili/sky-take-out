package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xi
 * @create 2023/10/4- 19:27
 */
@RestController
@RequestMapping("/admin/shop")
@Slf4j
@Api( tags = "商铺状态")
public class ShopController {
    @Autowired
    ShopService shopService;
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable("status") Integer status){
        log.info("设置店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        shopService.setStatus(status);
        return Result.success();

    }

    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = shopService.getStatus();
        log.info("获取店铺的营业状态:{}",status);
        return Result.success(status);
    }
}
