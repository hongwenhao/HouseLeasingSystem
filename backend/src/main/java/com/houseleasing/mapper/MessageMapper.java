package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供站内消息的基础 CRUD 操作
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
