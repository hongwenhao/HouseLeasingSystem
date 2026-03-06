package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
