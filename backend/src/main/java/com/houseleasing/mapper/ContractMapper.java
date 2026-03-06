package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供租赁合同的基础 CRUD 操作
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
