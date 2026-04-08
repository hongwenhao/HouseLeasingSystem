package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.HouseImage;

import java.util.List;

/**
 * 房源服务接口
 *
 * @author hongwenhao
 * @description 定义房源相关的业务操作，包括发布、更新、搜索、审核、收藏和统计
 */
public interface HouseService { // 房源主流程抽象：发布、查询、收藏、上下架、删除等

    /**
     * 房东发布新房源
     *
     * @param house   房源信息对象
     * @param ownerId 发布房源的房东用户 ID
     * @return 创建成功的房源对象
     */
    House addHouse(House house, Long ownerId); // 房东发布新房源

    /**
     * 更新房源信息
     *
     * @param id      要更新的房源 ID
     * @param house   包含更新内容的房源对象
     * @param ownerId 操作人（必须是该房源的房东）ID
     * @return 更新后的房源对象
     */
    House updateHouse(Long id, House house, Long ownerId); // 房东修改自己房源

    /**
     * 根据房源 ID 查询房源详情（同时更新浏览量）
     *
     * @param id 房源 ID
     * @return 房源详情对象
     */
    House getHouseById(Long id); // 查询房源详情（含浏览量统计）

    /**
     * 按条件分页搜索房源
     *
     * @param request 包含筛选条件和分页参数的搜索请求
     * @return 符合条件的分页房源列表
     */
    PageResult<House> searchHouses(HouseSearchRequest request); // 多条件分页搜索房源

    /**
     * 管理员审核房源（上线或拒绝）
     *
     * @param id       要审核的房源 ID
     * @param approved true 表示审核通过上线，false 表示拒绝
     * @param reason   审核意见
     */
//    void approveHouse(Long id, boolean approved, String reason);

    /**
     * 查询指定房东发布的房源列表（分页）
     *
     * @param ownerId 房东用户 ID
     * @param page    当前页码
     * @param size    每页大小
     * @return 该房东的分页房源列表
     */
    PageResult<House> listOwnerHouses(Long ownerId, int page, int size); // 查询某房东发布的房源

    /**
     * 查询当前用户收藏的房源列表（分页）
     *
     * @param userId 用户 ID
     * @param page   当前页码
     * @param size   每页大小
     * @return 收藏房源的分页列表
     */
    PageResult<House> listCollectedHouses(Long userId, int page, int size); // 查询用户收藏的房源

    /**
     * 收藏指定房源（若已收藏则忽略）
     *
     * @param userId  收藏操作的用户 ID
     * @param houseId 要收藏的房源 ID
     */
    void collectHouse(Long userId, Long houseId); // 收藏房源

    /**
     * 取消收藏指定房源（若未收藏则忽略）
     *
     * @param userId 取消收藏的用户 ID
     * @param houseId 要取消收藏的房源 ID
     */
    void cancelCollectHouse(Long userId, Long houseId); // 取消收藏房源

    /**
     * 获取热门房源列表（按浏览量降序，最多 10 条，使用缓存）
     *
     * @return 热门房源列表
     */
    List<House> getHotHouses(); // 获取热门房源（一般用于首页推荐）

    /**
     * 增加房源浏览量
     *
     * @param houseId 要增加浏览量的房源 ID
     */
    void incrementViewCount(Long houseId); // 增加房源浏览量

    /**
     * 删除房源（仅房源所有者可操作）
     * <p>同时清理关联的图片明细记录和用户收藏行为记录，并清除热门房源缓存。</p>
     *
     * @param id      要删除的房源 ID
     * @param ownerId 操作人用户 ID（必须是该房源的房东，否则抛出 403 业务异常）
     */
    void deleteHouse(Long id, Long ownerId); // 删除房源及关联数据

    /**
     * 房东主动上架自己的房源。
     * <p>仅允许房源所有者操作，成功后房源状态会更新为 ONLINE。</p>
     *
     * @param id      房源 ID
     * @param ownerId 当前操作房东 ID（必须与房源 ownerId 一致）
     */
    void putHouseOnline(Long id, Long ownerId); // 房东主动上架房源

    /**
     * 房东主动下架自己的房源。
     * <p>仅允许房源所有者操作，成功后房源状态会更新为 OFFLINE。</p>
     *
     * @param id      房源 ID
     * @param ownerId 当前操作房东 ID（必须与房源 ownerId 一致）
     */
    void putHouseOffline(Long id, Long ownerId); // 房东主动下架房源

    /**
     * 查询指定房源的图片列表（从 house_images 明细表读取，按 sort 升序）
     *
     * @param houseId 房源 ID
     * @return 该房源的图片列表
     */
    List<HouseImage> getHouseImages(Long houseId); // 查询房源图片明细
}
