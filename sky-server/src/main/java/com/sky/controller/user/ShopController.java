package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xi
 * @create 2023/10/4- 19:27
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api( tags = "商铺状态")
public class ShopController {

    @Autowired
    ShopService shopService;
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){
        Integer status = shopService.getStatus();
        log.info("获取店铺的营业状态:{}",status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
