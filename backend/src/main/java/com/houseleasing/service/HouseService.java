package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;

import java.util.List;

public interface HouseService {
    House addHouse(House house, Long ownerId);
    House updateHouse(Long id, House house, Long ownerId);
    House getHouseById(Long id);
    PageResult<House> searchHouses(HouseSearchRequest request);
    void approveHouse(Long id, boolean approved, String reason);
    PageResult<House> listOwnerHouses(Long ownerId, int page, int size);
    void collectHouse(Long userId, Long houseId);
    List<House> getHotHouses();
    void incrementViewCount(Long houseId);
}
