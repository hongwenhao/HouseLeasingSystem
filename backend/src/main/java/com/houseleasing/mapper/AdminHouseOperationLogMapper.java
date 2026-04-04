package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.AdminHouseOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员房源操作日志 Mapper
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供 admin_house_operation_logs 的基础 CRUD 能力，
 *              供管理员房源管理接口记录与查询时间线使用
 */
@Mapper
public interface AdminHouseOperationLogMapper extends BaseMapper<AdminHouseOperationLog> {
}
