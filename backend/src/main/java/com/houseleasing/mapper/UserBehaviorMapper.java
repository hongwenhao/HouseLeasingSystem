package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户行为数据访问层接口
 *
 * @author hongwenhao
 * @description 继承 MyBatis-Plus BaseMapper，提供用户行为记录的基础 CRUD 操作，
 *              并扩展了用于协同过滤推荐算法的数据查询方法
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 查询指定用户有过交互行为的所有房源 ID（去重）
     *
     * @param userId 用户 ID
     * @return 该用户交互过的房源 ID 列表
     */
    @Select("SELECT DISTINCT house_id FROM user_behaviors WHERE user_id = #{userId}")
    List<Long> selectHouseIdsByUserId(Long userId);

    /**
     * 查询与指定房源有过交互行为的所有用户 ID（排除指定用户，去重）
     * 用于协同过滤中找到与当前用户相似的其他用户
     *
     * @param houseId       房源 ID
     * @param excludeUserId 需要排除的用户 ID（通常为当前用户）
     * @return 与该房源交互过的其他用户 ID 列表
     */
    @Select("SELECT DISTINCT user_id FROM user_behaviors WHERE house_id = #{houseId} AND user_id != #{excludeUserId}")
    List<Long> selectUserIdsByHouseId(Long houseId, Long excludeUserId);
}
