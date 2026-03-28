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
    /** 当前页码 */
    private Integer page;
    /** 每页显示条数（使用包装类型方便检测是否显式传入） */
    private Integer size;
    /** 别名：与 size 等价的分页大小参数，方便前端使用 pageSize 命名 */
    private Integer pageSize;
    /** 排序字段（如：price、viewCount、createTime） */
    private String sortBy;

    /** 返回非空的页码值，若未提供则返回默认值 1 */
    public int getPage() {
        return page != null ? page : 1;
    }

    /** 返回非空的分页大小，若未提供则返回默认值 10 */
    public int getSize() {
        return size != null ? size : 10;
    }

    /**
     * 规范化分页参数，解决 pageSize 与 size 的别名冲突并补全默认值
     * @throws IllegalArgumentException 当 pageSize 与 size 同时提供且值不同
     */
    public void normalizePagination() {
        if (pageSize != null && size != null && !pageSize.equals(size)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid pagination parameters: pageSize=%s, size=%s", pageSize, size));
        }
        size = size != null ? size : (pageSize != null ? pageSize : 10);
        page = page != null ? page : 1;
    }
}
