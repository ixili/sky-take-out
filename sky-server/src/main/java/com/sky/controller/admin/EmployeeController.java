package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.service.Tags;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api( tags = "员工操作")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工退出")
    public Result<String> logout() {
        return Result.success();
    }


    @PostMapping
    @ApiOperation("新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO){
        employeeService.addEmployee(employeeDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询员工")
    public Result<PageResult> queryPage(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("分页查询员工：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.queryPage(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * @description:启用禁用员工账号
     * @author: xi
     * @date: 2023/9/29 20:01
     * @paramType: [java.lang.Integer, java.lang.Long]
     * @param: [status, id]
     * @return: com.sky.result.Result
     **/
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用员工账号")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("启用禁用员工账号:{}{}",status,id);
        employeeService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * @description:根据id查询员工
     * @author: xi
     * @date: 2023/9/29 23:45
     * @paramType: [java.lang.Long]
     * @param: [id]
     * @return: com.sky.result.Result<com.sky.entity.Employee>
     **/
    @GetMapping("/{id}")
    @ApiOperation("根据id查询员工")
    public Result<Employee> queryById(@PathVariable("id") Long id){
        Employee employee = employeeService.lambdaQuery().eq(Employee::getId,id).one();
        employee.setPassword("***");
        return Result.success(employee);
    }

    /**
     * @description:编辑员工信息
     * @author: xi
     * @date: 2023/9/29 23:47
     * @paramType: [com.sky.dto.EmployeeDTO]
     * @param: [employeeDTO]
     * @return: com.sky.result.Result
     **/
    @PutMapping
    @ApiOperation("编辑员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        employeeService.updateEmployee(employeeDTO);
        return Result.success();
    }
}
