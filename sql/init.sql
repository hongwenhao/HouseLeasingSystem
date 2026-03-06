-- Create database
CREATE DATABASE IF NOT EXISTS house_leasing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE house_leasing;

-- Users table
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `phone` VARCHAR(20) UNIQUE,
  `email` VARCHAR(100) UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `role` ENUM('TENANT','LANDLORD','ADMIN') DEFAULT 'TENANT',
  `real_name` VARCHAR(50),
  `id_card` VARCHAR(18),
  `avatar` VARCHAR(500),
  `credit_score` INT DEFAULT 100,
  `is_real_name_auth` TINYINT DEFAULT 0 COMMENT '是否实名认证',
  `status` ENUM('ACTIVE','DISABLED') DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Houses table (三类房东 + 五项费用)
CREATE TABLE IF NOT EXISTS `houses` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
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
  `status` ENUM('PENDING','APPROVED','REJECTED','ONLINE','OFFLINE') DEFAULT 'PENDING',
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
  `images` TEXT COMMENT '图片列表JSON',
  `tags` VARCHAR(500),
  `view_count` INT DEFAULT 0,
  `owner_id` BIGINT,
  `workflow_instance_id` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- House images
CREATE TABLE IF NOT EXISTS `house_images` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `house_id` BIGINT NOT NULL,
  `image_url` VARCHAR(500) NOT NULL,
  `sort` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Orders (rental intents + appointments + orders)
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
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Contracts
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
  `risk_items` TEXT COMMENT '风险条款JSON',
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
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Messages / notifications
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT,
  `type` ENUM('SYSTEM','ORDER','CONTRACT','APPOINTMENT','REVIEW') DEFAULT 'SYSTEM',
  `is_read` TINYINT DEFAULT 0,
  `related_id` BIGINT COMMENT '关联业务ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User behaviors for recommendation
CREATE TABLE IF NOT EXISTS `user_behaviors` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `house_id` BIGINT NOT NULL,
  `behavior_type` ENUM('VIEW','COLLECT','ORDER','REVIEW') DEFAULT 'VIEW',
  `score` DECIMAL(3,1) DEFAULT 1.0 COMMENT '行为评分(VIEW=1, COLLECT=3, ORDER=5)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_house_id` (`house_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reviews
CREATE TABLE IF NOT EXISTS `reviews` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `house_id` BIGINT NOT NULL,
  `order_id` BIGINT,
  `user_id` BIGINT NOT NULL,
  `rating` INT DEFAULT 5,
  `content` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert demo data
-- Admin user (password: admin123)
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('admin', '13800000000', 'admin@houseleasing.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'ADMIN', '系统管理员', 1, 100);

-- Landlord user (password: landlord123)
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('landlord1', '13811111111', 'landlord1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'LANDLORD', '张房东', 1, 95);

-- Tenant user (password: tenant123)
INSERT INTO `users` (username, phone, email, password, role, real_name, is_real_name_auth, credit_score) VALUES
('tenant1', '13822222222', 'tenant1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTK/xOOXVta', 'TENANT', '李租客', 1, 90);

-- Sample houses
INSERT INTO `houses` (title, description, city, district, address, price, deposit, area, rooms, halls, bathrooms, floor, total_floor, decoration, house_type, owner_type, status, water_fee, water_fee_type, electric_fee, electric_fee_type, gas_fee, gas_fee_type, property_fee, property_fee_type, internet_fee, internet_fee_type, owner_id) VALUES
('阳光花园精装两室一厅', '南北通透，采光极好，近地铁2号线', '北京', '朝阳区', '北京市朝阳区阳光路100号阳光花园3栋5层501室', 5500.00, 11000.00, 75.5, 2, 1, 1, 5, 18, 'FINE', 'APARTMENT', 'OWNER', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 200.00, 'FIXED', 80.00, 'FIXED', 2),
('国贸CBD整租一居室', '精装修，拎包入住，距国贸地铁站500米', '北京', '朝阳区', '北京市朝阳区建国路88号', 6800.00, 13600.00, 55.0, 1, 1, 1, 12, 28, 'FINE', 'APARTMENT', 'AGENT', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 50.00, 'FIXED', 350.00, 'FIXED', 100.00, 'INCLUDED', 2),
('通州次卧出租（二房东）', '整套房次卧，可做饭，包网络', '北京', '通州区', '北京市通州区运河东大街88号', 2200.00, 2200.00, 15.0, 1, 0, 1, 3, 6, 'SIMPLE', 'ROOM', 'SUBLEASE', 'ONLINE', 3.50, 'METERED', 0.52, 'METERED', 30.00, 'INCLUDED', 100.00, 'FIXED', 0.00, 'INCLUDED', 2),
('浦东新区精品公寓', '近陆家嘴，全套家电，商务出行便利', '上海', '浦东新区', '上海市浦东新区张杨路500号', 7500.00, 15000.00, 80.0, 2, 2, 1, 8, 32, 'LUXURY', 'APARTMENT', 'OWNER', 'ONLINE', 4.00, 'METERED', 0.617, 'METERED', 60.00, 'FIXED', 500.00, 'FIXED', 120.00, 'FIXED', 2),
('天河区大三室整租', '广州天河，近珠江新城，交通便利', '广州', '天河区', '广州市天河区天河路385号', 4500.00, 9000.00, 90.0, 3, 2, 2, 6, 15, 'MEDIUM', 'APARTMENT', 'OWNER', 'ONLINE', 2.50, 'METERED', 0.60, 'METERED', 40.00, 'FIXED', 280.00, 'FIXED', 80.00, 'FIXED', 2);
