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

-- ============================================================
-- 扩展示例房源与图片数据（多城市、多类型、每套多图）
-- ============================================================
-- 说明：
-- 1) 下面额外补充 30 条房源数据（ID 6~35），覆盖公寓/住宅/单间/别墅与三类房东身份。
-- 2) 所有图片文件统一放在 backend/uploads 目录，数据库只保存相对路径（不含域名）。
-- 3) 路径格式统一为 /api/uploads/xxx.svg，对应后端静态资源映射，前后端环境可复用。
-- 4) 每个房源均配置 3 张图片：houses.cover_image（封面）+ houses.images（JSON 列表）+ house_images 明细。
-- 5) 若后续新增房源，请保持三处图片数据同步，避免列表页和详情页图片不一致。

-- 先为原有 5 条房源补齐封面图与多图 JSON（使用相对路径）
UPDATE `houses` SET `cover_image` = '/api/uploads/seed_house_01.svg', `images` = '["/api/uploads/seed_house_01.svg", "/api/uploads/seed_house_02.svg", "/api/uploads/seed_house_03.svg"]' WHERE `id` = 1;
UPDATE `houses` SET `cover_image` = '/api/uploads/seed_house_04.svg', `images` = '["/api/uploads/seed_house_04.svg", "/api/uploads/seed_house_05.svg", "/api/uploads/seed_house_06.svg"]' WHERE `id` = 2;
UPDATE `houses` SET `cover_image` = '/api/uploads/seed_house_07.svg', `images` = '["/api/uploads/seed_house_07.svg", "/api/uploads/seed_house_08.svg", "/api/uploads/seed_house_09.svg"]' WHERE `id` = 3;
UPDATE `houses` SET `cover_image` = '/api/uploads/seed_house_10.svg', `images` = '["/api/uploads/seed_house_10.svg", "/api/uploads/seed_house_11.svg", "/api/uploads/seed_house_12.svg"]' WHERE `id` = 4;
UPDATE `houses` SET `cover_image` = '/api/uploads/seed_house_13.svg', `images` = '["/api/uploads/seed_house_13.svg", "/api/uploads/seed_house_14.svg", "/api/uploads/seed_house_15.svg"]' WHERE `id` = 5;

-- 新增 30 条扩展房源数据（每条都带封面图与多图 JSON 字段）
INSERT INTO `houses` (`id`, `title`, `description`, `province`, `city`, `district`, `address`, `price`, `deposit`, `area`, `rooms`, `halls`, `bathrooms`, `floor`, `total_floor`, `decoration`, `house_type`, `owner_type`, `status`, `water_fee`, `water_fee_type`, `electric_fee`, `electric_fee_type`, `gas_fee`, `gas_fee_type`, `property_fee`, `property_fee_type`, `internet_fee`, `internet_fee_type`, `cover_image`, `images`, `tags`, `owner_id`) VALUES
(6, '北京海淀区优选公寓 01号', '北京海淀区核心生活圈，房型规整，适合上班族和学生', '北京市', '北京', '海淀区', '北京市北京海淀区示范路120号1栋101室', 1800.00, 1800.00, 28.0, 1, 1, 1, 2, 6, 'SIMPLE', 'APARTMENT', 'OWNER', 'ONLINE', 2.80, 'METERED', 0.550, 'FIXED', 35.00, 'INCLUDED', 120.00, 'INCLUDED', 40.00, 'FIXED', '/api/uploads/seed_house_01.svg', '["/api/uploads/seed_house_01.svg", "/api/uploads/seed_house_06.svg", "/api/uploads/seed_house_12.svg"]', '近地铁,可看房,配套齐全', 2),
(7, '北京朝阳区优选住宅 02号', '北京朝阳区核心生活圈，房型规整，适合上班族和学生', '北京市', '北京', '朝阳区', '北京市北京朝阳区示范路121号2栋202室', 2030.00, 4060.00, 30.7, 2, 2, 2, 3, 7, 'MEDIUM', 'HOUSE', 'SUBLEASE', 'ONLINE', 3.10, 'FIXED', 0.630, 'INCLUDED', 43.00, 'FIXED', 155.00, 'METERED', 60.00, 'INCLUDED', '/api/uploads/seed_house_02.svg', '["/api/uploads/seed_house_02.svg", "/api/uploads/seed_house_07.svg", "/api/uploads/seed_house_13.svg"]', '拎包入住,采光好,交通便利', 2),
(8, '上海闵行区优选单间 03号', '上海闵行区核心生活圈，房型规整，适合家庭长期居住', '上海市', '上海', '闵行区', '上海市上海闵行区示范路122号3栋303室', 2260.00, 4520.00, 33.4, 3, 0, 1, 4, 8, 'FINE', 'ROOM', 'AGENT', 'ONLINE', 3.40, 'INCLUDED', 0.710, 'METERED', 51.00, 'METERED', 190.00, 'FIXED', 80.00, 'METERED', '/api/uploads/seed_house_03.svg', '["/api/uploads/seed_house_03.svg", "/api/uploads/seed_house_08.svg", "/api/uploads/seed_house_14.svg"]', '近地铁,可看房,配套齐全', 2),
(9, '上海徐汇区优选别墅 04号', '上海徐汇区核心生活圈，房型规整，适合家庭长期居住', '上海市', '上海', '徐汇区', '上海市上海徐汇区示范路123号4栋404室', 2490.00, 2490.00, 36.1, 4, 2, 2, 5, 9, 'LUXURY', 'VILLA', 'OWNER', 'ONLINE', 3.70, 'METERED', 0.790, 'FIXED', 59.00, 'INCLUDED', 225.00, 'INCLUDED', 100.00, 'FIXED', '/api/uploads/seed_house_04.svg', '["/api/uploads/seed_house_04.svg", "/api/uploads/seed_house_09.svg", "/api/uploads/seed_house_15.svg"]', '拎包入住,采光好,交通便利', 2),
(10, '广州天河区优选公寓 05号', '广州天河区核心生活圈，房型规整，适合上班族和学生', '广东省', '广州', '天河区', '广东省广州天河区示范路124号5栋505室', 2720.00, 5440.00, 38.8, 1, 1, 1, 6, 10, 'SIMPLE', 'APARTMENT', 'SUBLEASE', 'ONLINE', 4.00, 'FIXED', 0.550, 'INCLUDED', 67.00, 'FIXED', 260.00, 'METERED', 120.00, 'INCLUDED', '/api/uploads/seed_house_05.svg', '["/api/uploads/seed_house_05.svg", "/api/uploads/seed_house_10.svg", "/api/uploads/seed_house_16.svg"]', '近地铁,可看房,配套齐全', 2),
(11, '深圳南山区优选住宅 06号', '深圳南山区核心生活圈，房型规整，适合上班族和学生', '广东省', '深圳', '南山区', '广东省深圳南山区示范路125号6栋606室', 2950.00, 5900.00, 41.5, 2, 2, 2, 7, 11, 'MEDIUM', 'HOUSE', 'AGENT', 'ONLINE', 2.80, 'INCLUDED', 0.630, 'METERED', 75.00, 'METERED', 295.00, 'FIXED', 40.00, 'METERED', '/api/uploads/seed_house_06.svg', '["/api/uploads/seed_house_06.svg", "/api/uploads/seed_house_11.svg", "/api/uploads/seed_house_17.svg"]', '拎包入住,采光好,交通便利', 2),
(12, '杭州西湖区优选单间 07号', '杭州西湖区核心生活圈，房型规整，适合家庭长期居住', '浙江省', '杭州', '西湖区', '浙江省杭州西湖区示范路126号7栋707室', 3180.00, 3180.00, 44.2, 3, 0, 1, 8, 12, 'FINE', 'ROOM', 'OWNER', 'ONLINE', 3.10, 'METERED', 0.710, 'FIXED', 35.00, 'INCLUDED', 330.00, 'INCLUDED', 60.00, 'FIXED', '/api/uploads/seed_house_07.svg', '["/api/uploads/seed_house_07.svg", "/api/uploads/seed_house_12.svg", "/api/uploads/seed_house_18.svg"]', '近地铁,可看房,配套齐全', 2),
(13, '南京鼓楼区优选别墅 08号', '南京鼓楼区核心生活圈，房型规整，适合家庭长期居住', '江苏省', '南京', '鼓楼区', '江苏省南京鼓楼区示范路127号8栋808室', 3410.00, 6820.00, 46.9, 4, 2, 2, 9, 13, 'LUXURY', 'VILLA', 'SUBLEASE', 'ONLINE', 3.40, 'FIXED', 0.790, 'INCLUDED', 43.00, 'FIXED', 365.00, 'METERED', 80.00, 'INCLUDED', '/api/uploads/seed_house_08.svg', '["/api/uploads/seed_house_08.svg", "/api/uploads/seed_house_13.svg", "/api/uploads/seed_house_19.svg"]', '拎包入住,采光好,交通便利', 2),
(14, '成都高新区优选公寓 09号', '成都高新区核心生活圈，房型规整，适合上班族和学生', '四川省', '成都', '高新区', '四川省成都高新区示范路128号9栋909室', 3640.00, 7280.00, 49.6, 1, 1, 1, 10, 14, 'SIMPLE', 'APARTMENT', 'AGENT', 'ONLINE', 3.70, 'INCLUDED', 0.550, 'METERED', 51.00, 'METERED', 120.00, 'FIXED', 100.00, 'METERED', '/api/uploads/seed_house_09.svg', '["/api/uploads/seed_house_09.svg", "/api/uploads/seed_house_14.svg", "/api/uploads/seed_house_20.svg"]', '近地铁,可看房,配套齐全', 2),
(15, '武汉洪山区优选住宅 10号', '武汉洪山区核心生活圈，房型规整，适合上班族和学生', '湖北省', '武汉', '洪山区', '湖北省武汉洪山区示范路129号1栋1001室', 3870.00, 3870.00, 52.3, 2, 2, 2, 11, 15, 'MEDIUM', 'HOUSE', 'OWNER', 'ONLINE', 4.00, 'METERED', 0.630, 'FIXED', 59.00, 'INCLUDED', 155.00, 'INCLUDED', 120.00, 'FIXED', '/api/uploads/seed_house_10.svg', '["/api/uploads/seed_house_10.svg", "/api/uploads/seed_house_15.svg", "/api/uploads/seed_house_21.svg"]', '拎包入住,采光好,交通便利', 2),
(16, '西安雁塔区优选单间 11号', '西安雁塔区核心生活圈，房型规整，适合家庭长期居住', '陕西省', '西安', '雁塔区', '陕西省西安雁塔区示范路130号2栋1102室', 4100.00, 8200.00, 55.0, 3, 0, 1, 12, 16, 'FINE', 'ROOM', 'SUBLEASE', 'ONLINE', 2.80, 'FIXED', 0.710, 'INCLUDED', 67.00, 'FIXED', 190.00, 'METERED', 40.00, 'INCLUDED', '/api/uploads/seed_house_11.svg', '["/api/uploads/seed_house_11.svg", "/api/uploads/seed_house_16.svg", "/api/uploads/seed_house_22.svg"]', '近地铁,可看房,配套齐全', 2),
(17, '重庆渝中区优选别墅 12号', '重庆渝中区核心生活圈，房型规整，适合家庭长期居住', '重庆市', '重庆', '渝中区', '重庆市重庆渝中区示范路131号3栋1203室', 4330.00, 8660.00, 57.7, 4, 2, 2, 13, 17, 'LUXURY', 'VILLA', 'AGENT', 'ONLINE', 3.10, 'INCLUDED', 0.790, 'METERED', 75.00, 'METERED', 225.00, 'FIXED', 60.00, 'METERED', '/api/uploads/seed_house_12.svg', '["/api/uploads/seed_house_12.svg", "/api/uploads/seed_house_17.svg", "/api/uploads/seed_house_23.svg"]', '拎包入住,采光好,交通便利', 2),
(18, '天津和平区优选公寓 13号', '天津和平区核心生活圈，房型规整，适合上班族和学生', '天津市', '天津', '和平区', '天津市天津和平区示范路132号4栋1304室', 4560.00, 4560.00, 60.4, 1, 1, 1, 14, 18, 'SIMPLE', 'APARTMENT', 'OWNER', 'ONLINE', 3.40, 'METERED', 0.550, 'FIXED', 35.00, 'INCLUDED', 260.00, 'INCLUDED', 80.00, 'FIXED', '/api/uploads/seed_house_13.svg', '["/api/uploads/seed_house_13.svg", "/api/uploads/seed_house_18.svg", "/api/uploads/seed_house_24.svg"]', '近地铁,可看房,配套齐全', 2),
(19, '青岛市南区优选住宅 14号', '青岛市南区核心生活圈，房型规整，适合上班族和学生', '山东省', '青岛', '市南区', '山东省青岛市南区示范路133号5栋1405室', 4790.00, 9580.00, 63.1, 2, 2, 2, 15, 19, 'MEDIUM', 'HOUSE', 'SUBLEASE', 'ONLINE', 3.70, 'FIXED', 0.630, 'INCLUDED', 43.00, 'FIXED', 295.00, 'METERED', 100.00, 'INCLUDED', '/api/uploads/seed_house_14.svg', '["/api/uploads/seed_house_14.svg", "/api/uploads/seed_house_19.svg", "/api/uploads/seed_house_01.svg"]', '拎包入住,采光好,交通便利', 2),
(20, '厦门思明区优选单间 15号', '厦门思明区核心生活圈，房型规整，适合家庭长期居住', '福建省', '厦门', '思明区', '福建省厦门思明区示范路134号6栋1506室', 5020.00, 10040.00, 65.8, 3, 0, 1, 16, 20, 'FINE', 'ROOM', 'AGENT', 'ONLINE', 4.00, 'INCLUDED', 0.710, 'METERED', 51.00, 'METERED', 330.00, 'FIXED', 120.00, 'METERED', '/api/uploads/seed_house_15.svg', '["/api/uploads/seed_house_15.svg", "/api/uploads/seed_house_20.svg", "/api/uploads/seed_house_02.svg"]', '近地铁,可看房,配套齐全', 2),
(21, '北京海淀区优选别墅 16号', '北京海淀区核心生活圈，房型规整，适合家庭长期居住', '北京市', '北京', '海淀区', '北京市北京海淀区示范路135号7栋1607室', 5250.00, 5250.00, 68.5, 4, 2, 2, 17, 21, 'LUXURY', 'VILLA', 'OWNER', 'ONLINE', 2.80, 'METERED', 0.790, 'FIXED', 59.00, 'INCLUDED', 365.00, 'INCLUDED', 40.00, 'FIXED', '/api/uploads/seed_house_16.svg', '["/api/uploads/seed_house_16.svg", "/api/uploads/seed_house_21.svg", "/api/uploads/seed_house_03.svg"]', '拎包入住,采光好,交通便利', 2),
(22, '北京朝阳区优选公寓 17号', '北京朝阳区核心生活圈，房型规整，适合上班族和学生', '北京市', '北京', '朝阳区', '北京市北京朝阳区示范路136号8栋1708室', 5480.00, 10960.00, 71.2, 1, 1, 1, 18, 22, 'SIMPLE', 'APARTMENT', 'SUBLEASE', 'ONLINE', 3.10, 'FIXED', 0.550, 'INCLUDED', 67.00, 'FIXED', 120.00, 'METERED', 60.00, 'INCLUDED', '/api/uploads/seed_house_17.svg', '["/api/uploads/seed_house_17.svg", "/api/uploads/seed_house_22.svg", "/api/uploads/seed_house_04.svg"]', '近地铁,可看房,配套齐全', 2),
(23, '上海闵行区优选住宅 18号', '上海闵行区核心生活圈，房型规整，适合上班族和学生', '上海市', '上海', '闵行区', '上海市上海闵行区示范路137号9栋1809室', 5710.00, 11420.00, 73.9, 2, 2, 2, 19, 23, 'MEDIUM', 'HOUSE', 'AGENT', 'ONLINE', 3.40, 'INCLUDED', 0.630, 'METERED', 75.00, 'METERED', 155.00, 'FIXED', 80.00, 'METERED', '/api/uploads/seed_house_18.svg', '["/api/uploads/seed_house_18.svg", "/api/uploads/seed_house_23.svg", "/api/uploads/seed_house_05.svg"]', '拎包入住,采光好,交通便利', 2),
(24, '上海徐汇区优选单间 19号', '上海徐汇区核心生活圈，房型规整，适合家庭长期居住', '上海市', '上海', '徐汇区', '上海市上海徐汇区示范路138号1栋1901室', 5940.00, 5940.00, 76.6, 3, 0, 1, 2, 24, 'FINE', 'ROOM', 'OWNER', 'ONLINE', 3.70, 'METERED', 0.710, 'FIXED', 35.00, 'INCLUDED', 190.00, 'INCLUDED', 100.00, 'FIXED', '/api/uploads/seed_house_19.svg', '["/api/uploads/seed_house_19.svg", "/api/uploads/seed_house_24.svg", "/api/uploads/seed_house_06.svg"]', '近地铁,可看房,配套齐全', 2),
(25, '广州天河区优选别墅 20号', '广州天河区核心生活圈，房型规整，适合家庭长期居住', '广东省', '广州', '天河区', '广东省广州天河区示范路139号2栋2002室', 6170.00, 12340.00, 79.3, 4, 2, 2, 3, 25, 'LUXURY', 'VILLA', 'SUBLEASE', 'ONLINE', 4.00, 'FIXED', 0.790, 'INCLUDED', 43.00, 'FIXED', 225.00, 'METERED', 120.00, 'INCLUDED', '/api/uploads/seed_house_20.svg', '["/api/uploads/seed_house_20.svg", "/api/uploads/seed_house_01.svg", "/api/uploads/seed_house_07.svg"]', '拎包入住,采光好,交通便利', 2),
(26, '深圳南山区优选公寓 21号', '深圳南山区核心生活圈，房型规整，适合上班族和学生', '广东省', '深圳', '南山区', '广东省深圳南山区示范路140号3栋103室', 6400.00, 12800.00, 82.0, 1, 1, 1, 4, 26, 'SIMPLE', 'APARTMENT', 'AGENT', 'ONLINE', 2.80, 'INCLUDED', 0.550, 'METERED', 51.00, 'METERED', 260.00, 'FIXED', 40.00, 'METERED', '/api/uploads/seed_house_21.svg', '["/api/uploads/seed_house_21.svg", "/api/uploads/seed_house_02.svg", "/api/uploads/seed_house_08.svg"]', '近地铁,可看房,配套齐全', 2),
(27, '杭州西湖区优选住宅 22号', '杭州西湖区核心生活圈，房型规整，适合上班族和学生', '浙江省', '杭州', '西湖区', '浙江省杭州西湖区示范路141号4栋204室', 6630.00, 6630.00, 84.7, 2, 2, 2, 5, 27, 'MEDIUM', 'HOUSE', 'OWNER', 'ONLINE', 3.10, 'METERED', 0.630, 'FIXED', 59.00, 'INCLUDED', 295.00, 'INCLUDED', 60.00, 'FIXED', '/api/uploads/seed_house_22.svg', '["/api/uploads/seed_house_22.svg", "/api/uploads/seed_house_03.svg", "/api/uploads/seed_house_09.svg"]', '拎包入住,采光好,交通便利', 2),
(28, '南京鼓楼区优选单间 23号', '南京鼓楼区核心生活圈，房型规整，适合家庭长期居住', '江苏省', '南京', '鼓楼区', '江苏省南京鼓楼区示范路142号5栋305室', 6860.00, 13720.00, 87.4, 3, 0, 1, 6, 28, 'FINE', 'ROOM', 'SUBLEASE', 'ONLINE', 3.40, 'FIXED', 0.710, 'INCLUDED', 67.00, 'FIXED', 330.00, 'METERED', 80.00, 'INCLUDED', '/api/uploads/seed_house_23.svg', '["/api/uploads/seed_house_23.svg", "/api/uploads/seed_house_04.svg", "/api/uploads/seed_house_10.svg"]', '近地铁,可看房,配套齐全', 2),
(29, '成都高新区优选别墅 24号', '成都高新区核心生活圈，房型规整，适合家庭长期居住', '四川省', '成都', '高新区', '四川省成都高新区示范路143号6栋406室', 7090.00, 14180.00, 90.1, 4, 2, 2, 7, 29, 'LUXURY', 'VILLA', 'AGENT', 'ONLINE', 3.70, 'INCLUDED', 0.790, 'METERED', 75.00, 'METERED', 365.00, 'FIXED', 100.00, 'METERED', '/api/uploads/seed_house_24.svg', '["/api/uploads/seed_house_24.svg", "/api/uploads/seed_house_05.svg", "/api/uploads/seed_house_11.svg"]', '拎包入住,采光好,交通便利', 2),
(30, '武汉洪山区优选公寓 25号', '武汉洪山区核心生活圈，房型规整，适合上班族和学生', '湖北省', '武汉', '洪山区', '湖北省武汉洪山区示范路144号7栋507室', 7320.00, 7320.00, 92.8, 1, 1, 1, 8, 30, 'SIMPLE', 'APARTMENT', 'OWNER', 'ONLINE', 4.00, 'METERED', 0.550, 'FIXED', 35.00, 'INCLUDED', 120.00, 'INCLUDED', 120.00, 'FIXED', '/api/uploads/seed_house_01.svg', '["/api/uploads/seed_house_01.svg", "/api/uploads/seed_house_06.svg", "/api/uploads/seed_house_12.svg"]', '近地铁,可看房,配套齐全', 2),
(31, '西安雁塔区优选住宅 26号', '西安雁塔区核心生活圈，房型规整，适合上班族和学生', '陕西省', '西安', '雁塔区', '陕西省西安雁塔区示范路145号8栋608室', 7550.00, 15100.00, 95.5, 2, 2, 2, 9, 31, 'MEDIUM', 'HOUSE', 'SUBLEASE', 'ONLINE', 2.80, 'FIXED', 0.630, 'INCLUDED', 43.00, 'FIXED', 155.00, 'METERED', 40.00, 'INCLUDED', '/api/uploads/seed_house_02.svg', '["/api/uploads/seed_house_02.svg", "/api/uploads/seed_house_07.svg", "/api/uploads/seed_house_13.svg"]', '拎包入住,采光好,交通便利', 2),
(32, '重庆渝中区优选单间 27号', '重庆渝中区核心生活圈，房型规整，适合家庭长期居住', '重庆市', '重庆', '渝中区', '重庆市重庆渝中区示范路146号9栋709室', 7780.00, 15560.00, 98.2, 3, 0, 1, 10, 32, 'FINE', 'ROOM', 'AGENT', 'ONLINE', 3.10, 'INCLUDED', 0.710, 'METERED', 51.00, 'METERED', 190.00, 'FIXED', 60.00, 'METERED', '/api/uploads/seed_house_03.svg', '["/api/uploads/seed_house_03.svg", "/api/uploads/seed_house_08.svg", "/api/uploads/seed_house_14.svg"]', '近地铁,可看房,配套齐全', 2),
(33, '天津和平区优选别墅 28号', '天津和平区核心生活圈，房型规整，适合家庭长期居住', '天津市', '天津', '和平区', '天津市天津和平区示范路147号1栋801室', 8010.00, 8010.00, 100.9, 4, 2, 2, 11, 33, 'LUXURY', 'VILLA', 'OWNER', 'ONLINE', 3.40, 'METERED', 0.790, 'FIXED', 59.00, 'INCLUDED', 225.00, 'INCLUDED', 80.00, 'FIXED', '/api/uploads/seed_house_04.svg', '["/api/uploads/seed_house_04.svg", "/api/uploads/seed_house_09.svg", "/api/uploads/seed_house_15.svg"]', '拎包入住,采光好,交通便利', 2),
(34, '青岛市南区优选公寓 29号', '青岛市南区核心生活圈，房型规整，适合上班族和学生', '山东省', '青岛', '市南区', '山东省青岛市南区示范路148号2栋902室', 8240.00, 16480.00, 103.6, 1, 1, 1, 12, 14, 'SIMPLE', 'APARTMENT', 'SUBLEASE', 'ONLINE', 3.70, 'FIXED', 0.550, 'INCLUDED', 67.00, 'FIXED', 260.00, 'METERED', 100.00, 'INCLUDED', '/api/uploads/seed_house_05.svg', '["/api/uploads/seed_house_05.svg", "/api/uploads/seed_house_10.svg", "/api/uploads/seed_house_16.svg"]', '近地铁,可看房,配套齐全', 2),
(35, '厦门思明区优选住宅 30号', '厦门思明区核心生活圈，房型规整，适合上班族和学生', '福建省', '厦门', '思明区', '福建省厦门思明区示范路149号3栋1003室', 8470.00, 16940.00, 106.3, 2, 2, 2, 13, 15, 'MEDIUM', 'HOUSE', 'AGENT', 'ONLINE', 4.00, 'INCLUDED', 0.630, 'METERED', 75.00, 'METERED', 295.00, 'FIXED', 120.00, 'METERED', '/api/uploads/seed_house_06.svg', '["/api/uploads/seed_house_06.svg", "/api/uploads/seed_house_11.svg", "/api/uploads/seed_house_17.svg"]', '拎包入住,采光好,交通便利', 2);

-- 为所有房源（含原有 5 条 + 新增 30 条）初始化多图明细表 house_images（每房源 3 张）
-- 说明：sort 从 0 开始递增，前端按 sort 升序展示。
INSERT INTO `house_images` (`house_id`, `image_url`, `sort`) VALUES
(1, '/api/uploads/seed_house_01.svg', 0),
(1, '/api/uploads/seed_house_02.svg', 1),
(1, '/api/uploads/seed_house_03.svg', 2),
(2, '/api/uploads/seed_house_04.svg', 0),
(2, '/api/uploads/seed_house_05.svg', 1),
(2, '/api/uploads/seed_house_06.svg', 2),
(3, '/api/uploads/seed_house_07.svg', 0),
(3, '/api/uploads/seed_house_08.svg', 1),
(3, '/api/uploads/seed_house_09.svg', 2),
(4, '/api/uploads/seed_house_10.svg', 0),
(4, '/api/uploads/seed_house_11.svg', 1),
(4, '/api/uploads/seed_house_12.svg', 2),
(5, '/api/uploads/seed_house_13.svg', 0),
(5, '/api/uploads/seed_house_14.svg', 1),
(5, '/api/uploads/seed_house_15.svg', 2),
(6, '/api/uploads/seed_house_01.svg', 0),
(6, '/api/uploads/seed_house_06.svg', 1),
(6, '/api/uploads/seed_house_12.svg', 2),
(7, '/api/uploads/seed_house_02.svg', 0),
(7, '/api/uploads/seed_house_07.svg', 1),
(7, '/api/uploads/seed_house_13.svg', 2),
(8, '/api/uploads/seed_house_03.svg', 0),
(8, '/api/uploads/seed_house_08.svg', 1),
(8, '/api/uploads/seed_house_14.svg', 2),
(9, '/api/uploads/seed_house_04.svg', 0),
(9, '/api/uploads/seed_house_09.svg', 1),
(9, '/api/uploads/seed_house_15.svg', 2),
(10, '/api/uploads/seed_house_05.svg', 0),
(10, '/api/uploads/seed_house_10.svg', 1),
(10, '/api/uploads/seed_house_16.svg', 2),
(11, '/api/uploads/seed_house_06.svg', 0),
(11, '/api/uploads/seed_house_11.svg', 1),
(11, '/api/uploads/seed_house_17.svg', 2),
(12, '/api/uploads/seed_house_07.svg', 0),
(12, '/api/uploads/seed_house_12.svg', 1),
(12, '/api/uploads/seed_house_18.svg', 2),
(13, '/api/uploads/seed_house_08.svg', 0),
(13, '/api/uploads/seed_house_13.svg', 1),
(13, '/api/uploads/seed_house_19.svg', 2),
(14, '/api/uploads/seed_house_09.svg', 0),
(14, '/api/uploads/seed_house_14.svg', 1),
(14, '/api/uploads/seed_house_20.svg', 2),
(15, '/api/uploads/seed_house_10.svg', 0),
(15, '/api/uploads/seed_house_15.svg', 1),
(15, '/api/uploads/seed_house_21.svg', 2),
(16, '/api/uploads/seed_house_11.svg', 0),
(16, '/api/uploads/seed_house_16.svg', 1),
(16, '/api/uploads/seed_house_22.svg', 2),
(17, '/api/uploads/seed_house_12.svg', 0),
(17, '/api/uploads/seed_house_17.svg', 1),
(17, '/api/uploads/seed_house_23.svg', 2),
(18, '/api/uploads/seed_house_13.svg', 0),
(18, '/api/uploads/seed_house_18.svg', 1),
(18, '/api/uploads/seed_house_24.svg', 2),
(19, '/api/uploads/seed_house_14.svg', 0),
(19, '/api/uploads/seed_house_19.svg', 1),
(19, '/api/uploads/seed_house_01.svg', 2),
(20, '/api/uploads/seed_house_15.svg', 0),
(20, '/api/uploads/seed_house_20.svg', 1),
(20, '/api/uploads/seed_house_02.svg', 2),
(21, '/api/uploads/seed_house_16.svg', 0),
(21, '/api/uploads/seed_house_21.svg', 1),
(21, '/api/uploads/seed_house_03.svg', 2),
(22, '/api/uploads/seed_house_17.svg', 0),
(22, '/api/uploads/seed_house_22.svg', 1),
(22, '/api/uploads/seed_house_04.svg', 2),
(23, '/api/uploads/seed_house_18.svg', 0),
(23, '/api/uploads/seed_house_23.svg', 1),
(23, '/api/uploads/seed_house_05.svg', 2),
(24, '/api/uploads/seed_house_19.svg', 0),
(24, '/api/uploads/seed_house_24.svg', 1),
(24, '/api/uploads/seed_house_06.svg', 2),
(25, '/api/uploads/seed_house_20.svg', 0),
(25, '/api/uploads/seed_house_01.svg', 1),
(25, '/api/uploads/seed_house_07.svg', 2),
(26, '/api/uploads/seed_house_21.svg', 0),
(26, '/api/uploads/seed_house_02.svg', 1),
(26, '/api/uploads/seed_house_08.svg', 2),
(27, '/api/uploads/seed_house_22.svg', 0),
(27, '/api/uploads/seed_house_03.svg', 1),
(27, '/api/uploads/seed_house_09.svg', 2),
(28, '/api/uploads/seed_house_23.svg', 0),
(28, '/api/uploads/seed_house_04.svg', 1),
(28, '/api/uploads/seed_house_10.svg', 2),
(29, '/api/uploads/seed_house_24.svg', 0),
(29, '/api/uploads/seed_house_05.svg', 1),
(29, '/api/uploads/seed_house_11.svg', 2),
(30, '/api/uploads/seed_house_01.svg', 0),
(30, '/api/uploads/seed_house_06.svg', 1),
(30, '/api/uploads/seed_house_12.svg', 2),
(31, '/api/uploads/seed_house_02.svg', 0),
(31, '/api/uploads/seed_house_07.svg', 1),
(31, '/api/uploads/seed_house_13.svg', 2),
(32, '/api/uploads/seed_house_03.svg', 0),
(32, '/api/uploads/seed_house_08.svg', 1),
(32, '/api/uploads/seed_house_14.svg', 2),
(33, '/api/uploads/seed_house_04.svg', 0),
(33, '/api/uploads/seed_house_09.svg', 1),
(33, '/api/uploads/seed_house_15.svg', 2),
(34, '/api/uploads/seed_house_05.svg', 0),
(34, '/api/uploads/seed_house_10.svg', 1),
(34, '/api/uploads/seed_house_16.svg', 2),
(35, '/api/uploads/seed_house_06.svg', 0),
(35, '/api/uploads/seed_house_11.svg', 1),
(35, '/api/uploads/seed_house_17.svg', 2);

-- 为避免主键自增回退，手动对齐 houses 自增起点（下一条将从 36 开始）
ALTER TABLE `houses` AUTO_INCREMENT = 36;
