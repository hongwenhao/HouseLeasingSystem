package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订单数据访问层接口
 *
 * @author hongwenhao
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
     * 统计交易成功次数：仅统计订单状态为 COMPLETED 的记录。
     * 与产品口径保持一致，避免将仅签约未完成支付/履约的记录计入成交。
     */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'")
    long countCompletedOrders();

    /**
     * 将订单状态从 APPROVED 原子更新为 SIGNED。
     * 仅当当前状态仍为 APPROVED 时才会更新成功（返回 1），
     * 可避免并发场景下“先查后改”带来的竞态覆盖问题。
     */
    @Update("""
            UPDATE orders
            SET status = 'SIGNED',
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{orderId}
              AND status = 'APPROVED'
            """)
    int markOrderSignedIfApproved(@Param("orderId") Long orderId);

}
