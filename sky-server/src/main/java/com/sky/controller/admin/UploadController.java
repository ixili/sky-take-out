package com.sky.controller.admin;

import com.aliyuncs.exceptions.ClientException;
import com.sky.result.Result;
import com.sky.utils.AliOSSUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author xi
 * @create 2023/10/1- 17:55
 */
@RestController
@RequestMapping("/admin/common/upload")
@Api(tags = "阿里云上传")
public class UploadController {
    @Autowired
    AliOSSUtils aliOSSUtils;
    @PostMapping
    @ApiOperation("上传头像/文件")
    public Result<String> upload(MultipartFile file) throws IOException, ClientException {
        String url = aliOSSUtils.upload(file);
        return Result.success(url);
    }
}
