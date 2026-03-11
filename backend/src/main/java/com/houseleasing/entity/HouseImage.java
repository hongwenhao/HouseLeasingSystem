package com.houseleasing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房源图片实体类
 *
 * @author HouseLeasingSystem开发团队
 * @description 对应数据库 house_images 表，存储房源的图片信息，
 *              每个房源可关联多张图片，支持自定义排序
 */
@Data
@TableName("house_images")
public class HouseImage {
    /** 图片主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的房源 ID */
    private Long houseId;
    /** 图片访问 URL */
    private String imageUrl;
    /** 图片排列顺序（数字越小越靠前） */
    private Integer sort;
    /** 上传时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
