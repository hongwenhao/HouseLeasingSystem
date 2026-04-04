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
 * 2) 房东端“收到的评价”查询不能只依赖 reviews.house_id -> houses.owner_id，
 *    因为历史数据可能存在房源归属变更，若只按当前 owner_id 过滤，会把本应属于该房东历史订单的评价过滤掉；
 * 3) 因此提供按订单归属（orders.landlord_id）查询的分页接口，并兼容历史脏数据回退到 houses.owner_id。
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询某房东“收到的评价”。
     *
     * 查询口径（按优先级）：
     * - 主口径：评价关联订单的 landlord_id = 当前房东；
     * - 兼容口径：若历史订单缺失，或订单存在但 landlord_id 为空，再回退到评价关联房源的 owner_id = 当前房东。
     *
     * 这样可同时覆盖：
     * - 正常新数据（订单已固定房东归属）；
     * - 历史数据或迁移数据（订单字段不完整但房源仍可关联）。
     */
    @Select("""
            SELECT r.*
            FROM reviews r
            LEFT JOIN orders o ON o.id = r.order_id
            LEFT JOIN houses h ON h.id = r.house_id
            WHERE o.landlord_id = #{landlordId}
               OR ((o.id IS NULL OR o.landlord_id IS NULL) AND h.owner_id = #{landlordId})
            ORDER BY r.create_time DESC
            """)
    Page<Review> selectLandlordReviewPage(Page<Review> page, @Param("landlordId") Long landlordId);
}
