CREATE DATABASE IF NOT EXISTS house_leasing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE house_leasing;
/*
 Navicat Premium Data Transfer

 Source Server         : hongwenhao
 Source Server Type    : MySQL
 Source Server Version : 80039
 Source Host           : localhost:3306
 Source Schema         : house_leasing

 Target Server Type    : MySQL
 Target Server Version : 80039
 File Encoding         : 65001

 Date: 11/04/2026 01:12:10
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for contracts
-- ----------------------------
DROP TABLE IF EXISTS `contracts`;
CREATE TABLE `contracts`  (
                              `id` bigint(0) NOT NULL AUTO_INCREMENT,
                              `contract_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '合同编号',
                              `order_id` bigint(0) NOT NULL COMMENT '关联订单ID',
                              `house_id` bigint(0) NOT NULL COMMENT '关联房源ID',
                              `tenant_id` bigint(0) NOT NULL COMMENT '关联租客ID',
                              `landlord_id` bigint(0) NOT NULL COMMENT '关联房东ID',
                              `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '合同内容',
                              `status` enum('DRAFT','PENDING_SIGN','TENANT_SIGNED','LANDLORD_SIGNED','FULLY_SIGNED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'DRAFT' COMMENT '合同状态，默认值为DRAFT，DRAFT草稿/PENDING_SIGN待租客签名/TENANT_SIGNED租客已签名/LANDLORD_SIGNED房东已签名/FULLY_SIGNED /CANCELLED取消',
                              `risk_level` enum('LOW','MEDIUM','HIGH') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'LOW',
                              `risk_items` json NULL COMMENT '风险条款JSON',
                              `tenant_signed` tinyint(0) NULL DEFAULT 0 COMMENT '租客是否已签名，0否1是，默认值为0',
                              `landlord_signed` tinyint(0) NULL DEFAULT 0 COMMENT '房东是否已签名，0否1是，默认值为0',
                              `tenant_sign_time` datetime(0) NULL DEFAULT NULL COMMENT '租客签名时间',
                              `landlord_sign_time` datetime(0) NULL DEFAULT NULL COMMENT '房东签名时间',
                              `start_date` date NULL DEFAULT NULL COMMENT '租赁开始日期',
                              `end_date` date NULL DEFAULT NULL COMMENT '租赁结束日期',
                              `monthly_rent` decimal(10, 2) NULL DEFAULT NULL COMMENT '月租金',
                              `deposit` decimal(10, 2) NULL DEFAULT NULL COMMENT '押金',
                              `pdf_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PDF文件路径',
                              `sign_time` datetime(0) NULL DEFAULT NULL COMMENT '最终签署时间',
                              `workflow_instance_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作流实例ID',
                              `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                              `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `contract_no`(`contract_no`) USING BTREE,
                              INDEX `idx_order_id`(`order_id`) USING BTREE,
                              INDEX `idx_house_id`(`house_id`) USING BTREE,
                              INDEX `idx_tenant_id`(`tenant_id`) USING BTREE,
                              INDEX `idx_landlord_id`(`landlord_id`) USING BTREE,
                              CONSTRAINT `fk_contracts_house` FOREIGN KEY (`house_id`) REFERENCES `houses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `fk_contracts_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `fk_contracts_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                              CONSTRAINT `fk_contracts_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for house_images
-- ----------------------------
DROP TABLE IF EXISTS `house_images`;
CREATE TABLE `house_images`  (
                                 `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '图片ID',
                                 `house_id` bigint(0) NOT NULL COMMENT '关联房源ID',
                                 `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL',
                                 `sort` int(0) NULL DEFAULT 0 COMMENT '图片排序，默认值为0',
                                 `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `idx_house_id`(`house_id`) USING BTREE,
                                 CONSTRAINT `fk_house_images_house` FOREIGN KEY (`house_id`) REFERENCES `houses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of house_images
-- ----------------------------
INSERT INTO `house_images` VALUES (23, 1, '/api/uploads/b6adcd07af6f411f99fab0e14c82cd36.png', 0, NULL);
INSERT INTO `house_images` VALUES (24, 1, '/api/uploads/e1a93bdef4454dab8b46beb8f795e1f8.png', 1, NULL);
INSERT INTO `house_images` VALUES (25, 1, '/api/uploads/a963b4e53dea44d3acd3adf41b037edc.png', 2, NULL);
INSERT INTO `house_images` VALUES (26, 2, '/api/uploads/f149fba8c40b42cea27e19215f71cd2b.png', 0, NULL);
INSERT INTO `house_images` VALUES (27, 3, '/api/uploads/52dbb17d85be4538a0b84b2d7b68b6c0.png', 0, NULL);
INSERT INTO `house_images` VALUES (28, 4, '/api/uploads/890339e505664afe828f03a84b7cb470.png', 0, NULL);
INSERT INTO `house_images` VALUES (29, 4, '/api/uploads/5685dfb68307422ab8be1c84f5582e89.png', 1, NULL);
INSERT INTO `house_images` VALUES (30, 4, '/api/uploads/be967705237441659f82dc98870f1fb0.png', 2, NULL);
INSERT INTO `house_images` VALUES (31, 5, '/api/uploads/b8034c6103d44f86983667e43daa1dba.png', 0, NULL);
INSERT INTO `house_images` VALUES (32, 5, '/api/uploads/200de47fd57b4214aec7371a99e957db.png', 1, NULL);
INSERT INTO `house_images` VALUES (34, 6, '/api/uploads/a2700c6354d643c0a9b5b2d95340eb8d.png', 0, NULL);

-- ----------------------------
-- Table structure for houses
-- ----------------------------
DROP TABLE IF EXISTS `houses`;
CREATE TABLE `houses`  (
                           `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '房源ID',
                           `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房源标题',
                           `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '房源描述',
                           `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '省份',
                           `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市',
                           `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区/县',
                           `address` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
                           `price` decimal(10, 2) NOT NULL COMMENT '月租金',
                           `deposit` decimal(10, 2) NULL DEFAULT NULL COMMENT '押金',
                           `area` decimal(6, 2) NULL DEFAULT NULL COMMENT '面积(平米)',
                           `rooms` int(0) NULL DEFAULT 1 COMMENT '房间数量，默认值为1',
                           `halls` int(0) NULL DEFAULT 1 COMMENT '客厅数量，默认值为1',
                           `bathrooms` int(0) NULL DEFAULT 1 COMMENT '卫生间数量，默认值为1',
                           `floor` int(0) NULL DEFAULT NULL COMMENT '所在楼层',
                           `total_floor` int(0) NULL DEFAULT NULL COMMENT '总楼层数',
                           `decoration` enum('ROUGH','SIMPLE','MEDIUM','FINE','LUXURY') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'SIMPLE' COMMENT '装修程度，默认值为SIMPLE，ROUGH毛坯/SIMPLE简装/MEDIUM中等/FINE精装/LUXURY豪华',
                           `house_type` enum('APARTMENT','HOUSE','ROOM','VILLA') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'APARTMENT' COMMENT '房源类型，默认值为APARTMENT，APARTMENT公寓/HOUSE住宅/ROOM单间/VILLA别墅',
                           `owner_type` enum('OWNER','SUBLEASE','AGENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'OWNER一手房东/SUBLEASE二手房东/AGENT中介',
                           `status` enum('ONLINE','OFFLINE') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ONLINE' COMMENT '房源状态，默认值为ONLINE，ONLINE上架/OFFLINE下架',
                           `water_fee` decimal(6, 2) NULL DEFAULT NULL COMMENT '水费单价',
                           `water_fee_type` enum('METERED','FIXED','INCLUDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'METERED' COMMENT '水费计费方式，默认值为METERED，METERED按表计费/FIXED固定费用/INCLUDED包含在租金',
                           `electric_fee` decimal(6, 2) NULL DEFAULT NULL COMMENT '电费单价',
                           `electric_fee_type` enum('METERED','FIXED','INCLUDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'METERED' COMMENT '电费计费方式，默认值为METERED，METERED按表计费/FIXED固定费用/INCLUDED包含在租金',
                           `gas_fee` decimal(6, 2) NULL DEFAULT NULL COMMENT '燃气费',
                           `gas_fee_type` enum('METERED','FIXED','INCLUDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'FIXED' COMMENT '燃气费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
                           `property_fee` decimal(8, 2) NULL DEFAULT NULL COMMENT '物业费/月',
                           `property_fee_type` enum('METERED','FIXED','INCLUDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'FIXED' COMMENT '物业费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
                           `internet_fee` decimal(6, 2) NULL DEFAULT NULL COMMENT '网络费/月',
                           `internet_fee_type` enum('METERED','FIXED','INCLUDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'FIXED' COMMENT '网络费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
                           `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图片URL',
                           `images` json NULL COMMENT '图片列表JSON',
                           `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签列表，用逗号分隔',
                           `view_count` int(0) NULL DEFAULT 0 COMMENT '浏览次数',
                           `owner_id` bigint(0) NOT NULL COMMENT '房东用户ID',
                           `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                           `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           INDEX `idx_owner_id`(`owner_id`) USING BTREE,
                           CONSTRAINT `fk_houses_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of houses
-- ----------------------------
INSERT INTO `houses` VALUES (1, '阳光花园精装两室一厅', '南北通透，采光极好，近地铁2号线', '北京市', '北京市', '朝阳区', '北京市朝阳区阳光路100号阳光花园1层501室', 5500.00, 1.00, 75.50, 4, 3, 3, 1, 3, 'LUXURY', 'VILLA', 'OWNER', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 200.00, 'FIXED', 80.00, 'FIXED', NULL, '[\"/api/uploads/b6adcd07af6f411f99fab0e14c82cd36.png\", \"/api/uploads/e1a93bdef4454dab8b46beb8f795e1f8.png\", \"/api/uploads/a963b4e53dea44d3acd3adf41b037edc.png\"]', '拎包入住,洗衣机,空调,热水器,床,衣柜,沙发,电视,微波炉,冰箱,天然气,WiFi,暖气,停车位,近地铁,电梯,可养宠物', 0, 2, '2026-04-10 23:52:48', '2026-04-11 01:03:18');
INSERT INTO `houses` VALUES (2, '国贸CBD整租一居室', '精装修，拎包入住，距国贸地铁站500米', '河南省', '郑州市', '中原区', '郑州市郑州区西88号', 1000.00, 2.00, 55.00, 1, 1, 1, 12, 28, 'FINE', 'ROOM', 'AGENT', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 350.00, 'FIXED', 0.00, 'INCLUDED', NULL, '[\"/api/uploads/f149fba8c40b42cea27e19215f71cd2b.png\"]', '床,近地铁,电梯', 0, 2, '2026-04-10 23:52:48', '2026-04-11 01:03:29');
INSERT INTO `houses` VALUES (3, '通州次卧出租（二房东）', '整套房次卧，可做饭，包网络', '北京市', '北京市', '通州区', '北京市通州区运河东大街88号', 2200.00, 1.00, 15.00, 1, 1, 1, 3, 6, 'SIMPLE', 'ROOM', 'SUBLEASE', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 0.00, 'INCLUDED', 100.00, 'FIXED', 0.00, 'INCLUDED', NULL, '[\"/api/uploads/52dbb17d85be4538a0b84b2d7b68b6c0.png\"]', '床,衣柜', 0, 2, '2026-04-10 23:52:48', '2026-04-11 01:03:46');
INSERT INTO `houses` VALUES (4, '浦东新区精品公寓', '近陆家嘴，全套家电，商务出行便利', '上海市', '上海市', '浦东新区', '上海市浦东新区张杨路500号', 7500.00, 2.00, 80.00, 2, 2, 1, 8, 32, 'FINE', 'APARTMENT', 'OWNER', 'ONLINE', 4.00, 'METERED', 0.62, 'METERED', 60.00, 'FIXED', 500.00, 'FIXED', 120.00, 'FIXED', NULL, '[\"/api/uploads/890339e505664afe828f03a84b7cb470.png\", \"/api/uploads/5685dfb68307422ab8be1c84f5582e89.png\", \"/api/uploads/be967705237441659f82dc98870f1fb0.png\"]', '拎包入住,洗衣机,空调,热水器,床,衣柜,沙发', 0, 2, '2026-04-10 23:52:48', '2026-04-11 01:04:07');
INSERT INTO `houses` VALUES (5, '天河区大三室整租', '广州天河，近珠江新城，交通便利', '广东省', '广州市', '天河区', '广州市天河区天河路385号', 4500.00, 1.00, 90.00, 3, 2, 2, 6, 15, 'MEDIUM', 'APARTMENT', 'OWNER', 'ONLINE', 2.50, 'METERED', 0.60, 'METERED', 40.00, 'FIXED', 280.00, 'FIXED', 80.00, 'FIXED', NULL, '[\"/api/uploads/b8034c6103d44f86983667e43daa1dba.png\", \"/api/uploads/200de47fd57b4214aec7371a99e957db.png\"]', '拎包入住,洗衣机,空调,热水器,床,衣柜,沙发', 0, 2, '2026-04-10 23:52:48', '2026-04-11 01:04:15');
INSERT INTO `houses` VALUES (6, '北京四合院', '99新，欢迎入住', '北京市', '北京市', '石景山区', '西街公园26号', 10000.00, 2.00, 50.00, 2, 1, 1, 1, 1, 'LUXURY', 'VILLA', 'OWNER', 'ONLINE', 3.50, 'METERED', 0.60, 'METERED', 2.50, 'METERED', 200.00, 'FIXED', 100.00, 'FIXED', NULL, '[\"/api/uploads/a2700c6354d643c0a9b5b2d95340eb8d.png\"]', '拎包入住,洗衣机,空调,热水器,床,衣柜,沙发', 0, 2, '2026-04-11 01:05:48', '2026-04-11 01:06:26');

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
                             `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                             `user_id` bigint(0) NOT NULL COMMENT '接收消息的用户ID',
                             `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息标题',
                             `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
                             `type` enum('SYSTEM','ORDER','CONTRACT','APPOINTMENT','REVIEW') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'SYSTEM' COMMENT '消息类型，默认值为SYSTEM，SYSTEM系统通知/ORDER订单相关/CONTRACT合同相关/APPOINTMENT预约相关/REVIEW评价相关',
                             `is_read` tinyint(0) NULL DEFAULT 0 COMMENT '是否已读，默认值为0，0未读1已读',
                             `related_id` bigint(0) NULL DEFAULT NULL COMMENT '关联业务ID',
                             `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                             PRIMARY KEY (`id`) USING BTREE,
                             INDEX `idx_user_id`(`user_id`) USING BTREE,
                             CONSTRAINT `fk_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES (1, 3, '登录提醒', '今日登录成功，信用分+1（每日仅首次登录生效）', 'SYSTEM', 0, NULL, '2026-04-10 23:54:00');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
                           `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                           `house_id` bigint(0) NOT NULL COMMENT '关联房源ID',
                           `tenant_id` bigint(0) NOT NULL COMMENT '关联租客ID',
                           `landlord_id` bigint(0) NOT NULL COMMENT '关联房东ID',
                           `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
                           `status` enum('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED','SIGNED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '订单状态，默认值为PENDING，PENDING待处理/APPROVED审核通过/REJECTED审核拒绝/CANCELLED取消/COMPLETED完成',
                           `appointment_time` datetime(0) NULL DEFAULT NULL COMMENT '预约看房时间',
                           `start_date` date NULL DEFAULT NULL COMMENT '租赁开始日期',
                           `end_date` date NULL DEFAULT NULL COMMENT '租赁结束日期',
                           `monthly_rent` decimal(10, 2) NULL DEFAULT NULL COMMENT '月租金',
                           `deposit` decimal(10, 2) NULL DEFAULT NULL COMMENT '押金',
                           `total_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '总金额',
                           `payment_status` enum('UNPAID','PAID','REFUNDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'UNPAID' COMMENT '支付状态，默认值为UNPAID，UNPAID未支付/PAID已支付/REFUNDED已退款',
                           `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
                           `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                           `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           UNIQUE INDEX `order_no`(`order_no`) USING BTREE,
                           INDEX `idx_house_id`(`house_id`) USING BTREE,
                           INDEX `idx_tenant_id`(`tenant_id`) USING BTREE,
                           INDEX `idx_landlord_id`(`landlord_id`) USING BTREE,
                           CONSTRAINT `fk_orders_house` FOREIGN KEY (`house_id`) REFERENCES `houses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `fk_orders_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
                           CONSTRAINT `fk_orders_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reviews
-- ----------------------------
DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews`  (
                            `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '评价ID',
                            `house_id` bigint(0) NOT NULL COMMENT '关联房源ID',
                            `order_id` bigint(0) NOT NULL COMMENT '关联订单ID',
                            `user_id` bigint(0) NOT NULL COMMENT '评价用户ID',
                            `rating` int(0) NULL DEFAULT 5 COMMENT '评价星级，默认值为5星',
                            `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评价内容',
                            `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_house_id`(`house_id`) USING BTREE,
                            INDEX `idx_order_id`(`order_id`) USING BTREE,
                            INDEX `idx_user_id`(`user_id`) USING BTREE,
                            CONSTRAINT `fk_reviews_house` FOREIGN KEY (`house_id`) REFERENCES `houses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                            CONSTRAINT `fk_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                            CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_behaviors
-- ----------------------------
DROP TABLE IF EXISTS `user_behaviors`;
CREATE TABLE `user_behaviors`  (
                                   `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '行为ID',
                                   `user_id` bigint(0) NOT NULL COMMENT '用户ID',
                                   `house_id` bigint(0) NOT NULL COMMENT '房源ID',
                                   `behavior_type` enum('VIEW','COLLECT','ORDER') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'VIEW' COMMENT '行为类型，默认值为VIEW，VIEW浏览/COLLECT收藏/ORDER下单',
                                   `score` decimal(3, 1) NULL DEFAULT 1.0 COMMENT '行为评分(VIEW=1, COLLECT=3, ORDER=5)',
                                   `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `idx_user_id`(`user_id`) USING BTREE,
                                   INDEX `idx_house_id`(`house_id`) USING BTREE,
                                   CONSTRAINT `fk_behaviors_house` FOREIGN KEY (`house_id`) REFERENCES `houses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                   CONSTRAINT `fk_behaviors_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
                          `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                          `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
                          `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
                          `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
                          `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
                          `role` enum('TENANT','LANDLORD','ADMIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'TENANT' COMMENT '用户角色，TENANT租客/LANDLORD房东/ADMIN管理员，默认值为TENANT',
                          `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
                          `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号码（密文存储，应用层加密后写入）',
                          `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
                          `credit_score` int(0) NULL DEFAULT 100 COMMENT '信用评分，初始100分',
                          `is_real_name_auth` tinyint(0) NULL DEFAULT 0 COMMENT '是否实名认证，0否1是，默认值为0',
                          `status` enum('ACTIVE','BANNED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ACTIVE' COMMENT '账户状态，ACTIVE正常，BANNED封禁，默认值为ACTIVE',
                          `gender` tinyint(0) NULL DEFAULT 0 COMMENT '性别，0未知1男2女，默认值为0',
                          `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
                          `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
                          PRIMARY KEY (`id`) USING BTREE,
                          UNIQUE INDEX `username`(`username`) USING BTREE,
                          UNIQUE INDEX `phone`(`phone`) USING BTREE,
                          UNIQUE INDEX `email`(`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '13800000000', 'admin@houseleasing.com', '$2a$10$qPKC3gexeduYGaWGlUo0B.SKZ/d4ymEqUCjeaNzT3Go1PsvVfhUBu', 'ADMIN', '系统管理员', NULL, NULL, 100, 1, 'ACTIVE', 0, '2026-04-10 23:52:48', '2026-04-10 23:52:48');
INSERT INTO `users` VALUES (2, '企鹅房东', '13811111111', 'landlord1@example.com', '$2a$10$qPKC3gexeduYGaWGlUo0B.SKZ/d4ymEqUCjeaNzT3Go1PsvVfhUBu', 'LANDLORD', '洪文豪', 'ENC$/ZXzpW9RwKgwYXQbYE7H5Wm77gVNxcvFk6q28uabvejMMz/elW9fxIUvxYsC7Q==', '/api/uploads/b888ac8b985c4681845cc3d26eb558b0.png', 100, 1, 'ACTIVE', 1, '2026-04-10 23:52:48', '2026-04-10 23:54:32');
INSERT INTO `users` VALUES (3, '羊羊租客', '13822222222', 'tenant1@example.com', '$2a$10$qPKC3gexeduYGaWGlUo0B.SKZ/d4ymEqUCjeaNzT3Go1PsvVfhUBu', 'TENANT', '刘仲朝', 'ENC$TEs/0CI8sECGXRPGpHlY2sr5Bii0kOoayMmOcEqE2V+a1VG4+OvvzL2w5oTm0w==', '/api/uploads/a59dbb0d7d5f4069be11dc61fcfcb1fa.jpg', 101, 1, 'ACTIVE', 1, '2026-04-10 23:52:48', '2026-04-10 23:55:42');

SET FOREIGN_KEY_CHECKS = 1;
