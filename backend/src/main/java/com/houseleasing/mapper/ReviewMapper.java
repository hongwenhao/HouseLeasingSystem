package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评价数据访问层
 *
 * 说明：
 * 1) 基础 CRUD 由 MyBatis-Plus BaseMapper 提供；
 * 2) 房东端“收到的评价”核心诉求是“看到自己房源上的评价”，
 *    因此必须基于 reviews.house_id 关联 houses.owner_id 做主过滤；
 * 3) 同时兼容部分历史数据按订单归属（orders.landlord_id）命中，避免老数据口径不一致。
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询某房东“收到的评价”。
     *
     * 查询口径：
     * - 主口径：reviews.house_id -> houses.owner_id = 当前房东（满足“看自己房源评价”业务定义）；
     * - 兼容口径：orders.landlord_id = 当前房东（兼容历史订单归属数据）。
     * 两个口径使用 OR 合并，只要命中其一即可返回，避免因单一口径导致“有评价却看不到”。
     */
    @Select("""
            SELECT r.*
            FROM reviews r
            LEFT JOIN orders o ON o.id = r.order_id
            LEFT JOIN houses h ON h.id = r.house_id
            WHERE h.owner_id = #{landlordId}
               OR o.landlord_id = #{landlordId}
            ORDER BY r.create_time DESC
            """)
    Page<Review> selectLandlordReviewPage(Page<Review> page, @Param("landlordId") Long landlordId);
}
