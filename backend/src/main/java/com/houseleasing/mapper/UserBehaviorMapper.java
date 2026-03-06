package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
    @Select("SELECT DISTINCT house_id FROM user_behaviors WHERE user_id = #{userId}")
    List<Long> selectHouseIdsByUserId(Long userId);

    @Select("SELECT DISTINCT user_id FROM user_behaviors WHERE house_id = #{houseId} AND user_id != #{excludeUserId}")
    List<Long> selectUserIdsByHouseId(Long houseId, Long excludeUserId);
}
