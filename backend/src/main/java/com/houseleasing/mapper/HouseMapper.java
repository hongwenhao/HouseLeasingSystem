package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 房源数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供房源的基础 CRUD 操作，
 *              并扩展了按条件分页查询和更新浏览量的方法
 */
@Mapper
public interface HouseMapper extends BaseMapper<House> {

    /**
     * 按多条件分页查询房源列表（对应 XML Mapper 中的自定义 SQL）
     *
     * @param page    MyBatis-Plus 分页对象
     * @param request 包含各种筛选条件的搜索请求
     * @return 符合条件的分页房源数据
     */
    IPage<House> selectByCondition(Page<House> page, @Param("req") HouseSearchRequest request);

    /**
     * 将指定房源的浏览量加 1
     *
     * @param houseId 目标房源的 ID
     */
    @Update("UPDATE houses SET view_count = view_count + 1 WHERE id = #{houseId}")
    void incrementViewCount(Long houseId);

    /**
     * 统计当前在线房源数量（兼容历史 APPROVED 状态）。
     *
     * @return 在线房源数量
     */
    @Select("SELECT COUNT(*) FROM houses WHERE status IN ('ONLINE', 'APPROVED')")
    long countOnlineHouses();

    /**
     * 统计在线房源覆盖的城市数量（去重，兼容历史 APPROVED 状态）。
     *
     * @return 覆盖城市数量
     */
    @Select("SELECT COUNT(DISTINCT city) FROM houses WHERE status IN ('ONLINE', 'APPROVED') AND city IS NOT NULL AND city <> ''")
    long countOnlineCities();
}
