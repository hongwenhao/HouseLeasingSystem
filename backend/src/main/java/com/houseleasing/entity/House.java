package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源实体类
 *
 * @author HouseLeasingSystem开发团队
 * @description 对应数据库 houses 表，存储房源的详细信息，
 *              包括基本属性、费用信息、审核状态和统计数据
 */
@Data
@TableName("houses")
public class House {
    /** 房源主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 房源标题 */
    private String title;
    /** 房源详细描述 */
    private String description;
    /** 房源地址 */
    private String address;
    /** 房源面积（平方米） */
    private BigDecimal area;
    /** 所在省份 */
    private String province;
    /** 所在城市 */
    private String city;
    /** 所在区域 */
    private String district;
    /** 月租金（元） */
    private BigDecimal price;
    /**
     * 押金月数（以月数计，例如 1 表示押一个月租金）。
     * 实际押金金额需在业务层计算：depositMonths × price。
     */
    private BigDecimal deposit;
    /** 房屋类型（如：整租、合租） */
    private String houseType;
    /** 出租方类型（如：个人、中介） */
    private String ownerType;
    /** 状态：ONLINE（已上线）、REJECTED（已拒绝）、OFFLINE（已下架），兼容历史的 PENDING/APPROVED */
    private String status = "ONLINE";
    /** 水费单价 */
    private BigDecimal waterFee;
    /** 电费单价 */
    private BigDecimal electricFee;
    /** 燃气费单价 */
    private BigDecimal gasFee;
    /** 物业费单价 */
    private BigDecimal propertyFee;
    /** 网络费单价 */
    private BigDecimal internetFee;
    /** 水费计费类型（如：按用量、包含在租金内） */
    private String waterFeeType;
    /** 电费计费类型 */
    private String electricFeeType;
    /** 燃气费计费类型 */
    private String gasFeeType;
    /** 物业费计费类型 */
    private String propertyFeeType;
    /** 网络费计费类型 */
    private String internetFeeType;
    /** 封面图片 URL */
    private String coverImage;
    /** 房源标签（如：近地铁、可养宠物），多标签用逗号分隔 */
    private String tags;
    /** 居室数量（几室） */
    private Integer rooms;
    /** 厅的数量（几厅） */
    private Integer halls;
    /** 卫生间数量（几卫） */
    private Integer bathrooms;
    /** 所在楼层 */
    private Integer floor;
    /** 楼栋总层数 */
    private Integer totalFloor;
    /** 装修情况（如：精装、简装、毛坯） */
    private String decoration;
    /** 房源图片列表（JSON 格式） */
    private String images;
    /** 房东用户 ID */
    private Long ownerId;
    /** 浏览次数，默认为 0 */
    private Integer viewCount = 0;
    /** 房东用户信息（非数据库字段，通过查询关联填充） */
    @TableField(exist = false)
    private User landlord;
    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
