package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.HouseImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 房源图片数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供房源图片的基础 CRUD 操作，
 *              并扩展了按房源 ID 查询图片列表的方法
 */
@Mapper
public interface HouseImageMapper extends BaseMapper<HouseImage> {

    /**
     * 根据房源 ID 查询该房源的所有图片，按 sort 字段升序排列
     *
     * @param houseId 房源 ID
     * @return 该房源的图片列表（按排序字段升序）
     */
    @Select("SELECT * FROM house_images WHERE house_id = #{houseId} ORDER BY sort ASC")
    List<HouseImage> selectByHouseId(Long houseId);
}
