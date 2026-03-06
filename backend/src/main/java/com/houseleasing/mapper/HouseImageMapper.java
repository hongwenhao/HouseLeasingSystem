package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.HouseImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HouseImageMapper extends BaseMapper<HouseImage> {
    @Select("SELECT * FROM house_images WHERE house_id = #{houseId} ORDER BY sort ASC")
    List<HouseImage> selectByHouseId(Long houseId);
}
