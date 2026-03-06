package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HouseMapper extends BaseMapper<House> {
    IPage<House> selectByCondition(Page<House> page, @Param("req") HouseSearchRequest request);

    @Update("UPDATE houses SET view_count = view_count + 1 WHERE id = #{houseId}")
    void incrementViewCount(Long houseId);
}
