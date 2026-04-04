package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 合同数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供租赁合同的基础 CRUD 操作
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 统计已完整签署（成交）的合同数量。
     *
     * @return 成交数量
     */
    @Select("SELECT COUNT(*) FROM contracts WHERE status = 'FULLY_SIGNED'")
    long countFullySignedContracts();
}
