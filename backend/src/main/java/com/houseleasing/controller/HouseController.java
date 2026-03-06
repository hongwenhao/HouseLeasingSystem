package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "House", description = "House management")
@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final UserMapper userMapper;

    @Operation(summary = "Search houses (public)")
    @GetMapping("/search")
    public Result<PageResult<House>> searchHouses(HouseSearchRequest request) {
        return Result.success(houseService.searchHouses(request));
    }

    @Operation(summary = "Get all houses (public)")
    @GetMapping
    public Result<PageResult<House>> listHouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        HouseSearchRequest req = new HouseSearchRequest();
        req.setPage(page);
        req.setSize(size);
        return Result.success(houseService.searchHouses(req));
    }

    @Operation(summary = "Get house by ID (public)")
    @GetMapping("/{id}")
    public Result<House> getHouseById(@PathVariable Long id) {
        return Result.success(houseService.getHouseById(id));
    }

    @Operation(summary = "Get hot houses")
    @GetMapping("/hot")
    public Result<List<House>> getHotHouses() {
        return Result.success(houseService.getHotHouses());
    }

    @Operation(summary = "Add new house")
    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<House> addHouse(@RequestBody House house,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.addHouse(house, user.getId()));
    }

    @Operation(summary = "Update house")
    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<House> updateHouse(@PathVariable Long id,
                                      @RequestBody House house,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.updateHouse(id, house, user.getId()));
    }

    @Operation(summary = "List owner's houses")
    @GetMapping("/my")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<PageResult<House>> listMyHouses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(houseService.listOwnerHouses(user.getId(), page, size));
    }

    @Operation(summary = "Collect/favorite a house")
    @PostMapping("/{id}/collect")
    @SecurityRequirement(name = "Bearer Authentication")
    public Result<Void> collectHouse(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        houseService.collectHouse(user.getId(), id);
        return Result.success();
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
