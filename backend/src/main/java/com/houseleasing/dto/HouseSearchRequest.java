package com.houseleasing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 房源搜索请求数据传输对象
 *
 * @author HouseLeasingSystem开发团队
 * @description 封装房源搜索的筛选条件和分页参数，支持多维度组合查询
 */
@Data
public class HouseSearchRequest {
    /** 关键词搜索（匹配标题、地址等字段） */
    private String keyword;
    /** 城市筛选 */
    private String city;
    /** 区域筛选 */
    private String district;
    /** 最低月租金（元） */
    private BigDecimal minPrice;
    /** 最高月租金（元） */
    private BigDecimal maxPrice;
    /** 房屋类型筛选（如：整租、合租） */
    private String houseType;
    /** 出租方类型筛选（如：个人、中介） */
    private String ownerType;
    /** 房间数量筛选 */
    private Integer rooms;
    /** 装修情况筛选（如：精装、简装、毛坯） */
    private String decoration;
    /** 当前页码，默认第 1 页 */
    private int page = 1;
    /** 每页显示条数，默认 10 条 */
    private int size = 10;
    /** 排序字段（如：price、viewCount、createTime） */
    private String sortBy;
}
