package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

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

    /**
     * 统计指定租客在指定房源、指定时间窗口内的取消预约次数。
     *
     * @param tenantId  租客 ID
     * @param houseId   房源 ID
     * @param startTime 时间窗口开始（含）
     * @param endTime   时间窗口结束（不含）
     * @return 取消次数
     */
    @Select("""
            SELECT COUNT(1)
            FROM orders
            WHERE tenant_id = #{tenantId}
              AND house_id = #{houseId}
              AND status = 'CANCELLED'
              AND cancelled_time >= #{startTime}
              AND cancelled_time < #{endTime}
            """)
    Integer countTenantHouseCancelledInRange(@Param("tenantId") Long tenantId,
                                             @Param("houseId") Long houseId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
