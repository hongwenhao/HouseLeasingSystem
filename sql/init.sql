-- ============================================================
-- init.sql —— 数据库初始化脚本
-- 创建 house_leasing 数据库及所有业务表，并插入演示数据
-- 字符集：utf8mb4（支持中文及 emoji）
-- ============================================================

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS house_leasing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE house_leasing;

-- ============================================================
-- 用户表（users）
-- 存储平台所有用户信息，包括租客、房东和管理员
-- role 字段区分用户角色，is_real_name_auth 标识是否完成实名认证
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) UNIQUE NOT NULL COMMENT '手机号',
  `email` VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `role` ENUM('TENANT','LANDLORD','ADMIN') DEFAULT 'TENANT' COMMENT '用户角色，TENANT租客/LANDLORD房东/ADMIN管理员，默认值为TENANT',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `id_card` VARCHAR(18) COMMENT '身份证号码',
  `avatar` VARCHAR(500) COMMENT '头像URL',
  `credit_score` INT DEFAULT 100 COMMENT '信用评分，初始100分',
  `is_real_name_auth` TINYINT DEFAULT 0 COMMENT '是否实名认证，0否1是，默认值为0',
  `status` ENUM('ACTIVE','BANNED') DEFAULT 'ACTIVE' COMMENT '账户状态，ACTIVE正常，BANNED封禁，默认值为ACTIVE',
  `gender` TINYINT DEFAULT 0 COMMENT '性别，0未知1男2女，默认值为0',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房源表（houses）
-- 存储房源全部信息，包含三类房东类型和五项费用配置
-- owner_type: OWNER（一手房东）/ SUBLEASE（二手房东）/ AGENT（持牌中介）
-- water/electric/gas/property/internet_fee_type: METERED/FIXED/INCLUDED
-- ============================================================
CREATE TABLE IF NOT EXISTS `houses` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房源ID',
  `title` VARCHAR(200) NOT NULL COMMENT '房源标题',
  `description` TEXT COMMENT '房源描述',
  `province` VARCHAR(50) NOT NULL COMMENT '省份',
  `city` VARCHAR(50) NOT NULL COMMENT '城市',
  `district` VARCHAR(50) NOT NULL COMMENT '区/县',
  `address` VARCHAR(300) NOT NULL COMMENT '详细地址',
  `price` DECIMAL(10,2) NOT NULL COMMENT '月租金',
  `deposit` DECIMAL(10,2) COMMENT '押金',
  `area` DECIMAL(6,2) COMMENT '面积(平米)',
  `rooms` INT DEFAULT 1 COMMENT '房间数量，默认值为1',
  `halls` INT DEFAULT 1 COMMENT '客厅数量，默认值为1',
  `bathrooms` INT DEFAULT 1 COMMENT '卫生间数量，默认值为1',
  `floor` INT COMMENT '所在楼层',
  `total_floor` INT COMMENT '总楼层数',
  `decoration` ENUM('ROUGH','SIMPLE','MEDIUM','FINE','LUXURY') DEFAULT 'SIMPLE' COMMENT '装修程度，默认值为SIMPLE，ROUGH毛坯/SIMPLE简装/MEDIUM中等/FINE精装/LUXURY豪华',
  `house_type` ENUM('APARTMENT','HOUSE','ROOM','VILLA') DEFAULT 'APARTMENT' COMMENT '房源类型，默认值为APARTMENT，APARTMENT公寓/HOUSE住宅/ROOM单间/VILLA别墅' ,
  `owner_type` ENUM('OWNER','SUBLEASE','AGENT') NOT NULL COMMENT 'OWNER一手房东/SUBLEASE二手房东/AGENT中介',
  `status` ENUM('ONLINE','OFFLINE') DEFAULT 'ONLINE' COMMENT '房源状态，默认值为ONLINE，ONLINE上架/OFFLINE下架',
  `water_fee` DECIMAL(6,2) COMMENT '水费单价',
  `water_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'METERED' COMMENT '水费计费方式，默认值为METERED，METERED按表计费/FIXED固定费用/INCLUDED包含在租金',
  `electric_fee` DECIMAL(6,2) COMMENT '电费单价',
  `electric_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'METERED' COMMENT '电费计费方式，默认值为METERED，METERED按表计费/FIXED固定费用/INCLUDED包含在租金',
  `gas_fee` DECIMAL(6,2) COMMENT '燃气费',
  `gas_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED' COMMENT '燃气费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
  `property_fee` DECIMAL(8,2) COMMENT '物业费/月',
  `property_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED' COMMENT '物业费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
  `internet_fee` DECIMAL(6,2) COMMENT '网络费/月',
  `internet_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED' COMMENT '网络费计费方式，默认值为FIXED，FIXED固定费用/METERED按表计费/INCLUDED包含在租金',
  `cover_image` VARCHAR(500) COMMENT '封面图片URL',
  `images` JSON COMMENT '图片列表JSON',
  `tags` VARCHAR(500) COMMENT '标签列表，用逗号分隔',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `owner_id` BIGINT NOT NULL COMMENT '房东用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_owner_id` (`owner_id`),
  CONSTRAINT `fk_houses_owner` FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房源图片表（house_images）
-- 存储房源图片 URL 列表，支持多张图片，sort 字段控制显示顺序
-- ============================================================
CREATE TABLE IF NOT EXISTS `house_images` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
  `house_id` BIGINT NOT NULL COMMENT '关联房源ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `sort` INT DEFAULT 0 COMMENT '图片排序，默认值为0',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_house_id` (`house_id`),
  CONSTRAINT `fk_house_images_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 订单表（orders）
-- 存储租客发起的预约看房订单
-- status 流转：PENDING → APPROVED/REJECTED/CANCELLED → COMPLETED/SIGNED
-- ============================================================
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  `house_id` BIGINT NOT NULL COMMENT '关联房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '关联租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '关联房东ID',
  `order_no` VARCHAR(50) UNIQUE NOT NULL COMMENT '订单号',
  `status` ENUM('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED','SIGNED') DEFAULT 'PENDING' COMMENT '订单状态，默认值为PENDING，PENDING待处理/APPROVED审核通过/REJECTED审核拒绝/CANCELLED取消/COMPLETED完成' ,
  `appointment_time` DATETIME COMMENT '预约看房时间',
  `start_date` DATE COMMENT '租赁开始日期',
  `end_date` DATE COMMENT '租赁结束日期',
  `monthly_rent` DECIMAL(10,2) COMMENT '月租金',
  `deposit` DECIMAL(10,2) COMMENT '押金',
  `total_amount` DECIMAL(12,2) COMMENT '总金额',
  `payment_status` ENUM('UNPAID','PAID','REFUNDED') DEFAULT 'UNPAID' COMMENT '支付状态，默认值为UNPAID，UNPAID未支付/PAID已支付/REFUNDED已退款',
  `remark` TEXT COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_house_id` (`house_id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_landlord_id` (`landlord_id`),
  CONSTRAINT `fk_orders_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 合同表（contracts）
-- 存储房屋租赁合同信息，支持电子签署（双方分别签名）
-- status 流转：DRAFT → PENDING_SIGN → TENANT_SIGNED/LANDLORD_SIGNED → FULLY_SIGNED/CANCELLED
-- risk_level 由 AI 智能检测合同条款风险后写入
-- ============================================================
CREATE TABLE IF NOT EXISTS `contracts` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `contract_no` VARCHAR(50) UNIQUE NOT NULL COMMENT '合同编号',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `house_id` BIGINT NOT NULL COMMENT '关联房源ID',
  `tenant_id` BIGINT NOT NULL COMMENT '关联租客ID',
  `landlord_id` BIGINT NOT NULL COMMENT '关联房东ID',
  `content` LONGTEXT COMMENT '合同内容',
  `status` ENUM('DRAFT','PENDING_SIGN','TENANT_SIGNED','LANDLORD_SIGNED','FULLY_SIGNED','CANCELLED') DEFAULT 'DRAFT' COMMENT '合同状态，默认值为DRAFT，DRAFT草稿/PENDING_SIGN待租客签名/TENANT_SIGNED租客已签名/LANDLORD_SIGNED房东已签名/FULLY_SIGNED /CANCELLED取消',
  `risk_level` ENUM('LOW','MEDIUM','HIGH') DEFAULT 'LOW',
  `risk_items` JSON COMMENT '风险条款JSON',
  `tenant_signed` TINYINT DEFAULT 0 COMMENT '租客是否已签名，0否1是，默认值为0',
  `landlord_signed` TINYINT DEFAULT 0 COMMENT '房东是否已签名，0否1是，默认值为0',
  `tenant_sign_time` DATETIME COMMENT '租客签名时间',
  `landlord_sign_time` DATETIME COMMENT '房东签名时间',
  `start_date` DATE COMMENT '租赁开始日期',
  `end_date` DATE COMMENT '租赁结束日期',
  `monthly_rent` DECIMAL(10,2) COMMENT '月租金',
  `deposit` DECIMAL(10,2) COMMENT '押金',
  `pdf_path` VARCHAR(500) COMMENT 'PDF文件路径',
  `sign_time` DATETIME COMMENT '最终签署时间',
  `workflow_instance_id` VARCHAR(100) COMMENT '工作流实例ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_house_id` (`house_id`),
  INDEX `idx_tenant_id` (`tenant_id`),
  INDEX `idx_landlord_id` (`landlord_id`),
  CONSTRAINT `fk_contracts_order` FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_contracts_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_contracts_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_contracts_landlord` FOREIGN KEY (`landlord_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 站内消息表（messages）
-- 存储系统通知、订单状态变更、合同提醒等消息
-- related_id 关联具体业务记录 ID
-- ============================================================
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  `user_id` BIGINT NOT NULL COMMENT '接收消息的用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `type` ENUM('SYSTEM','ORDER','CONTRACT','APPOINTMENT','REVIEW') DEFAULT 'SYSTEM' COMMENT '消息类型，默认值为SYSTEM，SYSTEM系统通知/ORDER订单相关/CONTRACT合同相关/APPOINTMENT预约相关/REVIEW评价相关',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读，默认值为0，0未读1已读',
  `related_id` BIGINT COMMENT '关联业务ID' ,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 用户行为记录表（user_behaviors）
-- 记录用户浏览、收藏、下单等行为，用于个性化推荐算法
-- score 字段：VIEW=1分, COLLECT=3分, ORDER=5分（行为权重）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_behaviors` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行为ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `house_id` BIGINT NOT NULL COMMENT '房源ID',
  `behavior_type` ENUM('VIEW','COLLECT','ORDER','REVIEW') DEFAULT 'VIEW' COMMENT '行为类型，默认值为VIEW，VIEW浏览/COLLECT收藏/ORDER下单',
  `score` DECIMAL(3,1) DEFAULT 1.0 COMMENT '行为评分(VIEW=1, COLLECT=3, ORDER=5)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_house_id` (`house_id`),
  CONSTRAINT `fk_behaviors_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_behaviors_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房源评价表（reviews）
-- 租客在租期结束后对房源进行评价，rating 为 1-5 星
-- ============================================================
CREATE TABLE IF NOT EXISTS `reviews` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
  `house_id` BIGINT NOT NULL COMMENT '关联房源ID',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
  `rating` INT DEFAULT 5 COMMENT '评价星级，默认值为5星',
  `content` TEXT COMMENT '评价内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_house_id` (`house_id`),
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_reviews_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 演示数据初始化
-- 以下 INSERT 语句插入开发/测试用的示例数据
-- 所有密码均为 BCrypt 加密后的 "admin123" 哈希值
-- ============================================================

-- 管理员账号（账号：admin，密码：admin123）
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('admin', '13800000000', 'admin@houseleasing.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'ADMIN', '系统管理员', 1, 100);

-- 房东示例账号（账号：landlord1，密码：landlord123）
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('landlord1', '13811111111', 'landlord1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'LANDLORD', '张房东', 1, 95);

-- 租客示例账号（账号：tenant1，密码：tenant123）
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('tenant1', '13822222222', 'tenant1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'TENANT', '李租客', 1, 90);

-- 示例房源数据（北京/上海/广州各城市代表性房源，覆盖三种房东类型）
INSERT INTO `houses` (title, description, province, city, district, address, price, deposit, area, rooms, halls, bathrooms, floor, total_floor, decoration, house_type, owner_type, status, water_fee, water_fee_type, electric_fee, electric_fee_type, gas_fee, gas_fee_type, property_fee, property_fee_type, internet_fee, internet_fee_type, owner_id) VALUES
('阳光花园精装两室一厅', '南北通透，采光极好，近地铁2号线', '北京市', '北京', '朝阳区', '北京市朝阳区阳光路100号阳光花园3栋5层501室', 5500.00, 11000.00, 75.5, 2, 1, 1, 5, 18, 'FINE', 'APARTMENT', 'OWNER', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 200.00, 'FIXED', 80.00, 'FIXED', 2),
('国贸CBD整租一居室', '精装修，拎包入住，距国贸地铁站500米', '北京市', '北京', '朝阳区', '北京市朝阳区建国路88号', 6800.00, 13600.00, 55.0, 1, 1, 1, 12, 28, 'FINE', 'APARTMENT', 'AGENT', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 350.00, 'FIXED', 100.00, 'INCLUDED', 2),
('通州次卧出租（二房东）', '整套房次卧，可做饭，包网络', '北京市', '北京', '通州区', '北京市通州区运河东大街88号', 2200.00, 2200.00, 15.0, 1, 0, 1, 3, 6, 'SIMPLE', 'ROOM', 'SUBLEASE', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 30.00, 'INCLUDED', 100.00, 'FIXED', 0.00, 'INCLUDED', 2),
('浦东新区精品公寓', '近陆家嘴，全套家电，商务出行便利', '上海市', '上海', '浦东新区', '上海市浦东新区张杨路500号', 7500.00, 15000.00, 80.0, 2, 2, 1, 8, 32, 'LUXURY', 'APARTMENT', 'OWNER', 'ONLINE', 4.00, 'METERED', 0.617, 'METERED', 60.00, 'FIXED', 500.00, 'FIXED', 120.00, 'FIXED', 2),
('天河区大三室整租', '广州天河，近珠江新城，交通便利', '广东省', '广州', '天河区', '广州市天河区天河路385号', 4500.00, 9000.00, 90.0, 3, 2, 2, 6, 15, 'MEDIUM', 'APARTMENT', 'OWNER', 'ONLINE', 2.50, 'METERED', 0.60, 'METERED', 40.00, 'FIXED', 280.00, 'FIXED', 80.00, 'FIXED', 2);
