package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供订单的基础 CRUD 操作
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 查询房东订单（兼容历史数据）：
     * - 新数据：orders.landlord_id = 当前房东
     * - 历史数据：orders.house_id 对应的 houses.owner_id = 当前房东
     *
     * 使用 JOIN 让数据库优化器更好利用索引，避免大 IN/子查询带来的性能不确定性。
     */
    @Select("""
            SELECT o.*
            FROM orders o
            WHERE o.landlord_id = #{landlordId}
               OR EXISTS (
                    SELECT 1
                    FROM houses h
                    WHERE h.id = o.house_id
                      AND h.owner_id = #{landlordId}
               )
            ORDER BY o.create_time DESC
            """)
    Page<Order> selectLandlordOrdersPage(Page<Order> page, @Param("landlordId") Long landlordId);
}
