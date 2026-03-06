package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供订单的基础 CRUD 操作
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
