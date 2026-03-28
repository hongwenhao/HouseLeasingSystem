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
     /** 别名：与 size 等价的分页大小参数，方便前端使用 pageSize 命名 */
     private Integer pageSize;
     /** 标记 size 是否在请求中被显式传入，用于冲突检测 */
     private boolean sizeProvided;
     /** 标记 pageSize 是否在请求中被显式传入，用于冲突检测 */
     private boolean pageSizeProvided;
     /** 排序字段（如：price、viewCount、createTime） */
     private String sortBy;

     /** 自定义 setter：记录 size 是否被显式传入 */
     public void setSize(int size) {
         this.sizeProvided = true;
         this.size = size;
     }

     /** 自定义 setter：pageSize 为 size 的别名，并记录是否显式传入 */
     public void setPageSize(Integer pageSize) {
         this.pageSizeProvided = pageSize != null;
         this.pageSize = pageSize;
        if (pageSize != null && !this.sizeProvided) {
            this.size = pageSize;
        }
     }

     /** 供控制器检测 size 是否显式传入 */
     public boolean isSizeProvided() {
         return sizeProvided;
     }

     /** 供控制器检测 pageSize 是否显式传入 */
     public boolean isPageSizeProvided() {
         return pageSizeProvided;
     }
}
