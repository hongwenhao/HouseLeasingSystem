package com.houseleasing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.houseleasing.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 继承 MyBatis-Plus BaseMapper，提供用户的基础 CRUD 操作，
 *              并扩展了按用户名和手机号查询的方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号码
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User selectByPhone(String phone);

    /**
     * 根据用户名或手机号查询用户（用于登录统一入口）
     *
     * @param identifier 用户名或手机号
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{identifier} OR phone = #{identifier}")
    User selectByUsernameOrPhone(String identifier);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 匹配的用户对象，不存在时返回 null
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);

    /**
     * 按用户 ID 查询并加行级锁（用于并发敏感业务）。
     *
     * @param userId 用户 ID
     * @return 用户对象
     */
    @Select("SELECT * FROM users WHERE id = #{userId} FOR UPDATE")
    User selectByIdForUpdate(@Param("userId") Long userId);

    /**
     * 每日登录信用分原子加分：
     * 仅当当天尚未加分时更新，避免并发登录导致重复加分。
     *
     * @param userId 用户 ID
     * @return 受影响行数（1=成功加分，0=当天已加分或用户不存在）
     */
    @Update("""
            UPDATE users
            SET credit_score = CASE WHEN IFNULL(credit_score, 0) + 1 > 200 THEN 200 ELSE IFNULL(credit_score, 0) + 1 END,
                last_credit_add_date = CURRENT_DATE,
                update_time = NOW()
            WHERE id = #{userId}
              AND (last_credit_add_date IS NULL OR last_credit_add_date <> CURRENT_DATE)
            """)
    int addLoginCreditIfNotToday(@Param("userId") Long userId);
}
