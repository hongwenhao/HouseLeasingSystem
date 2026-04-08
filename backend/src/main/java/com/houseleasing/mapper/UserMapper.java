package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问层接口
 *
 * @author hongwenhao
 * @description 继承 MyBatis-Plus BaseMapper，提供用户的基础 CRUD 操作，
 *              并扩展了按用户名和手机号查询的方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> { // 用户数据访问接口（登录与注册查询）

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username); // 按用户名查询用户

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号码
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User selectByPhone(String phone); // 按手机号查询用户

    /**
     * 根据用户名或手机号查询用户（用于登录统一入口）
     *
     * @param identifier 用户名或手机号
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{identifier} OR phone = #{identifier}")
    User selectByUsernameOrPhone(String identifier); // 按用户名或手机号查询用户

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email); // 按邮箱查询用户

}
