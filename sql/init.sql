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
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `phone` VARCHAR(20) UNIQUE,
  `email` VARCHAR(100) UNIQUE NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `role` ENUM('TENANT','LANDLORD','ADMIN') DEFAULT 'TENANT',
  `real_name` VARCHAR(50),
  `id_card` VARCHAR(18),
  `avatar` VARCHAR(500),
  `credit_score` INT DEFAULT 100,
  `is_real_name_auth` TINYINT DEFAULT 0 COMMENT '是否实名认证',
  `status` ENUM('ACTIVE','BANNED') DEFAULT 'ACTIVE',
  `gender` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房源表（houses）
-- 存储房源全部信息，包含三类房东类型和五项费用配置
-- owner_type: OWNER（一手房东）/ SUBLEASE（二手房东）/ AGENT（持牌中介）
-- water/electric/gas/property/internet_fee_type: METERED/FIXED/INCLUDED
-- ============================================================
CREATE TABLE IF NOT EXISTS `houses` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `province` VARCHAR(50),
  `city` VARCHAR(50),
  `district` VARCHAR(50),
  `address` VARCHAR(300),
  `price` DECIMAL(10,2) NOT NULL COMMENT '月租金',
  `deposit` DECIMAL(10,2) COMMENT '押金',
  `area` DECIMAL(6,2) COMMENT '面积(平米)',
  `rooms` INT DEFAULT 1 COMMENT '室',
  `halls` INT DEFAULT 1 COMMENT '厅',
  `bathrooms` INT DEFAULT 1 COMMENT '卫',
  `floor` INT COMMENT '楼层',
  `total_floor` INT COMMENT '总楼层',
  `decoration` ENUM('ROUGH','SIMPLE','MEDIUM','FINE','LUXURY') DEFAULT 'SIMPLE',
  `house_type` ENUM('APARTMENT','HOUSE','ROOM','VILLA') DEFAULT 'APARTMENT',
  `owner_type` ENUM('OWNER','SUBLEASE','AGENT') NOT NULL COMMENT '一手房东/二手房东/中介',
  `status` ENUM('PENDING','APPROVED','REJECTED','ONLINE','OFFLINE') DEFAULT 'ONLINE',
  `water_fee` DECIMAL(6,2) COMMENT '水费单价',
  `water_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'METERED',
  `electric_fee` DECIMAL(6,2) COMMENT '电费单价',
  `electric_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'METERED',
  `gas_fee` DECIMAL(6,2) COMMENT '燃气费',
  `gas_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED',
  `property_fee` DECIMAL(8,2) COMMENT '物业费/月',
  `property_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED',
  `internet_fee` DECIMAL(6,2) COMMENT '网络费/月',
  `internet_fee_type` ENUM('METERED','FIXED','INCLUDED') DEFAULT 'FIXED',
  `cover_image` VARCHAR(500),
  `images` JSON COMMENT '图片列表JSON',
  `tags` VARCHAR(500),
  `view_count` INT DEFAULT 0,
  `owner_id` BIGINT NOT NULL COMMENT '房东用户ID',
  `workflow_instance_id` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_owner_id` (`owner_id`),
  CONSTRAINT `fk_houses_owner` FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房源图片表（house_images）
-- 存储房源图片 URL 列表，支持多张图片，sort 字段控制显示顺序
-- ============================================================
CREATE TABLE IF NOT EXISTS `house_images` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `house_id` BIGINT NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_house_id` (`house_id`),
  CONSTRAINT `fk_house_images_house` FOREIGN KEY (`house_id`) REFERENCES `houses`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 订单表（orders）
-- 存储租客发起的预约看房订单
-- order_type: APPOINTMENT（预约看房）/ INTENT（意向订单）/ LEASE（正式租约）
-- status 流转：PENDING → APPROVED/REJECTED/CANCELLED → COMPLETED/SIGNED
-- ============================================================
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `house_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `landlord_id` BIGINT NOT NULL,
  `order_no` VARCHAR(50) UNIQUE NOT NULL COMMENT '订单号',
  `order_type` ENUM('APPOINTMENT','INTENT','LEASE') DEFAULT 'INTENT',
  `status` ENUM('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED','SIGNED') DEFAULT 'PENDING',
  `appointment_time` DATETIME COMMENT '预约看房时间',
  `start_date` DATE COMMENT '租赁开始日期',
  `end_date` DATE COMMENT '租赁结束日期',
  `monthly_rent` DECIMAL(10,2),
  `deposit` DECIMAL(10,2),
  `total_amount` DECIMAL(12,2) COMMENT '总金额',
  `payment_status` ENUM('UNPAID','PAID','REFUNDED') DEFAULT 'UNPAID',
  `remark` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
  `order_id` BIGINT,
  `house_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `landlord_id` BIGINT NOT NULL,
  `content` LONGTEXT COMMENT '合同内容',
  `status` ENUM('DRAFT','PENDING_SIGN','TENANT_SIGNED','LANDLORD_SIGNED','FULLY_SIGNED','CANCELLED') DEFAULT 'DRAFT',
  `risk_level` ENUM('LOW','MEDIUM','HIGH') DEFAULT 'LOW',
  `risk_items` JSON COMMENT '风险条款JSON',
  `tenant_signed` TINYINT DEFAULT 0,
  `landlord_signed` TINYINT DEFAULT 0,
  `tenant_sign_time` DATETIME,
  `landlord_sign_time` DATETIME,
  `start_date` DATE COMMENT '租赁开始日期',
  `end_date` DATE COMMENT '租赁结束日期',
  `monthly_rent` DECIMAL(10,2),
  `deposit` DECIMAL(10,2),
  `pdf_path` VARCHAR(500),
  `sign_time` DATETIME COMMENT '最终签署时间',
  `workflow_instance_id` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `type` ENUM('SYSTEM','ORDER','CONTRACT','APPOINTMENT','REVIEW') DEFAULT 'SYSTEM',
  `related_id` BIGINT COMMENT '关联业务ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  CONSTRAINT `fk_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 用户行为记录表（user_behaviors）
-- 记录用户浏览、收藏、下单等行为，用于个性化推荐算法
-- score 字段：VIEW=1分, COLLECT=3分, ORDER=5分（行为权重）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_behaviors` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `house_id` BIGINT NOT NULL,
  `behavior_type` ENUM('VIEW','COLLECT','ORDER','REVIEW') DEFAULT 'VIEW',
  `score` DECIMAL(3,1) DEFAULT 1.0 COMMENT '行为评分(VIEW=1, COLLECT=3, ORDER=5)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
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
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `house_id` BIGINT NOT NULL,
  `order_id` BIGINT,
  `user_id` BIGINT NOT NULL,
  `rating` INT DEFAULT 5,
  `content` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
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
