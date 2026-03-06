package com.houseleasing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseSearchRequest {
    private String keyword;
    private String city;
    private String district;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String houseType;
    private String ownerType;
    private Integer rooms;
    private String decoration;
    private int page = 1;
    private int size = 10;
    private String sortBy;
}
