package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Contract;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {
}
