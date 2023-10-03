package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;


/**
 * 套餐菜品关系(SetmealDish)表数据库访问层
 *
 * @author lixi
 * @since 2023-10-03 20:12:43
 */
@Mapper
public interface SetmealDishMapper extends BaseMapper<SetmealDish> {

}

