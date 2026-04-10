# 🏠 房屋租赁系统 (House Leasing System)

基于 **Spring Boot 3 + Vue 3 + MySQL 8** 的全栈房屋租赁平台，融合信息透明机制、智能推荐算法与自动化审批流程。

## Abstract

With the rapid pace of urbanization, large numbers of young workers are flooding into major cities, and the residential rental market continues to expand. However, the current rental industry still relies on traditional means for listing properties, facilitating transactions, and managing contracts, resulting in issues such as difficulty verifying landlord identities, non-transparent fee breakdowns, and irregular signing procedures. Mainstream platforms lack clear differentiation between direct landlords, subletters, and licensed agents, and utility costs such as water, electricity, and gas are not uniformly disclosed in listings. Tenants often only learn the true cost of renting after moving in, compromising their right to be informed and their decision-making efficiency.

To address these issues, this paper designs and implements a house leasing management platform based on Spring Boot. The system adopts a B/S architecture, with a backend built on Spring Boot 3.x combined with MyBatis-Plus for data persistence, a frontend constructed with Vue 3 and Element Plus, MySQL 8.0 for data storage, and Redis for caching frequently accessed data. The platform serves three user roles—tenants, landlords, and administrators—and encompasses six subsystems: user services, property management, leasing operations, contract approval, message notifications, and backend administration.

The core innovations include: proposing a "three-type landlord + five-fee-item" dual-transparency display model that mandates disclosure of landlord classification and the pricing methods for utilities (water, electricity, gas, property management fees, and internet); constructing a hybrid recommendation engine that combines collaborative filtering with content-based similarity to precisely match listings based on user behavior; integrating the Activiti 7 workflow engine to drive review and contract-signing workflows, complemented by RabbitMQ for asynchronous notifications; and developing a lightweight contract risk-control module that automatically detects risky clauses—such as vague deposit terms and absent maintenance responsibilities—using a rule-based library. Functional, performance, and security testing has verified that all system modules operate correctly and meet the intended design goals.

**Keywords:** house leasing; Spring Boot; information transparency; hybrid recommendation; workflow engine; contract risk control

## ✨ 核心特性

- **三类房东透明标识**：一手房东（绿）/ 二手房东（橙）/ 持牌中介（蓝），消除信息不对称
- **五项费用强制披露**：水、电、燃气、物业、网络费用标准与计价方式一目了然
- **混合推荐算法**：基于协同过滤 + 内容相似度的个性化房源推送
- **Activiti 流程引擎**：房源上架、合同签署自动化审批流转
- **RabbitMQ 异步通知**：合同状态变更、预约确认等消息推送
- **Redis 缓存加速**：热门房源缓存，高并发优化
- **合同风控模块**：基于规则引擎自动识别风险条款
- **ECharts 数据可视化**：运营数据图表展示

## 🛠️ 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Vite + ECharts |
| 后端 | Spring Boot 3.x + MyBatis-Plus + Spring Security |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 认证 | JWT |
| 文档 | SpringDoc OpenAPI |

## 🚀 快速启动

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis 7.x（可选，服务不可用时自动降级）
- RabbitMQ 3.x（可选，服务不可用时自动降级）

### 1. 初始化数据库
```bash
mysql -u root -p < sql/init.sql
```

### 2. 启动后端
```bash
cd backend
mvn spring-boot:run
```
后端地址：http://localhost:8080
API 文档：http://localhost:8080/swagger-ui.html

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```
前端地址：http://localhost:5173

## 👤 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 房东 | landlord1 | landlord123 |
| 租客 | tenant1 | tenant123 |

> 注：demo 数据中所有密码均已用 BCrypt 加密存储。

## 📁 项目结构

```
HouseLeasingSystem/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/houseleasing/
│   │   ├── controller/         # RESTful API 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── mapper/             # MyBatis-Plus 数据访问层
│   │   ├── entity/             # 数据实体
│   │   ├── config/             # 配置类
│   │   ├── security/           # JWT 安全认证
│   │   ├── mq/                 # RabbitMQ 消息处理
│   │   └── activiti/           # Activiti 工作流
│   └── src/main/resources/
│       └── application.yml
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── components/         # 复用组件
│   │   ├── api/                # API 封装
│   │   ├── stores/             # Pinia 状态管理
│   │   └── router/             # 路由配置
│   └── package.json
└── sql/
    └── init.sql                # 数据库初始化脚本
```

## 🔑 主要 API

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/auth/register | POST | 用户注册 |
| /api/auth/login | POST | 用户登录 |
| /api/houses | GET | 房源列表（支持多条件筛选） |
| /api/houses/{id} | GET | 房源详情 |
| /api/orders | POST | 发起租赁意向/预约 |
| /api/contracts/generate | POST | 生成合同 |
| /api/contracts/{id}/risk-check | GET | 合同风险检测 |
| /api/recommend | GET | AI 个性化推荐 |
| /api/admin/statistics | GET | 后台统计数据 |

## 📊 系统架构

```
┌─────────────────────────────────────┐
│          Vue 3 Frontend              │
│   Element Plus + ECharts + Pinia     │
└──────────────┬──────────────────────┘
               │ HTTP/REST
┌──────────────▼──────────────────────┐
│        Spring Boot Backend           │
│  Spring Security (JWT) + MyBatis+    │
├───────────┬──────────────┬───────────┤
│   Redis   │   RabbitMQ   │  Activiti │
│ (缓存热点) │ (异步通知)   │ (审批流)  │
└───────────┴──────┬───────┴───────────┘
                   │
         ┌─────────▼────────┐
         │    MySQL 8.0      │
         │  (持久化存储)      │
         └──────────────────┘
```
