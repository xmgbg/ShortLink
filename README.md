# ShortLink - SaaS 短链接系统

## 项目介绍

ShortLink 是一个企业级 SaaS 短链接管理系统，基于 Spring Cloud 微服务架构构建，具备高并发、海量存储等场景的处理能力。

### 核心功能

- **短链接生成与管理**：支持单个/批量创建短链接，自定义短链接后缀，设置有效期
- **分组管理**：支持短链接分组，方便分类管理
- **回收站**：删除的短链接进入回收站，支持恢复和彻底删除
- **访问统计**：提供丰富的访问数据统计，包括 PV/UV/IP、地域分布、设备类型、浏览器、操作系统、访问网络等维度
- **用户系统**：用户注册、登录、个人信息管理，支持手机号脱敏展示
- **风控限流**：基于 Redis + Lua 脚本实现用户维度的流量风控
- **演示模式**：支持演示环境配置，限制危险操作

### 技术栈

#### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.0.7 | 应用框架 |
| Spring Cloud | 2022.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2022.0.0.0-RC2 | 微服务生态（Nacos 服务发现） |
| Spring Cloud Gateway | - | API 网关 |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| ShardingSphere | 5.3.2 | 分库分表 + 数据加密 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存 / 分布式锁 |
| Redisson | 3.27.2 | Redis 客户端 |
| Nacos | 2.x | 注册中心 & 配置中心 |
| Hutool | 5.8.27 | 工具库 |
| Fastjson2 | 2.0.36 | JSON 序列化 |
| EasyExcel | 3.1.3 | Excel 导入导出 |
| Sentinel | - | 流量防护 |

#### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.3.4 | 前端框架 |
| Vite | 4.4.9 | 构建工具 |
| Element Plus | 2.3.14 | UI 组件库 |
| ECharts | 4.8 | 图表可视化 |
| Axios | 1.5.1 | HTTP 客户端 |
| Vue Router | 4.2.4 | 路由管理 |
| Vuex | 4.0.2 | 状态管理 |

### 项目结构

```
shortlink/
├── admin/                   # 管理端服务（用户、分组、短链接管理）
├── project/                 # 核心短链接服务（生成、跳转、统计）
├── aggregation/             # 聚合服务（合并 admin + project 功能，单体部署方案）
├── gateway/                 # API 网关（路由分发、Token 鉴权）
├── console-vue/             # 前端管理控制台
└── pom.xml                  # 根 POM（依赖版本管理）
```

### 架构说明

项目支持两种部署模式：

#### 微服务模式（gateway profile: dev）

```
浏览器 → Gateway(8000) → Admin(8002) / Project(8001)
```

Gateway 作为统一入口，将请求路由到对应的微服务，同时负责 Token 鉴权。

#### 聚合服务模式（gateway profile: aggregation）

```
浏览器 → Gateway(8000) → Aggregation(8003)
```

Aggregation 服务整合了 Admin 和 Project 的全部功能，适合小规模部署场景。

### 数据库设计

- 采用 ShardingSphere 实现分表策略
- `t_user` 表：按 `username` 哈希取模分 16 张表（t_user_0 ~ t_user_15）
- `t_group` 表：按 `username` 哈希取模分 16 张表（t_group_0 ~ t_group_15）
- 用户敏感字段（手机号、邮箱）使用 AES 加密存储

---

## 环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | Java 开发环境 |
| Maven | 3.6+ | 项目构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存 / 分布式锁 |
| Nacos | 2.x | 服务注册与发现 |
| Node.js | 16+ | 前端运行环境 |

---

## 快速开始

### 1. 克隆项目

```bash
git clone <your-repo-url>
cd shortlink
```

### 2. 环境准备

#### 启动 MySQL

确保 MySQL 服务已启动，创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS `link` DEFAULT CHARACTER SET utf8mb4;
```

数据库连接配置位于各模块的 `shardingsphere-config-dev.yaml` 中，默认配置：

```yaml
host: 127.0.0.1
port: 3306
database: link
username: root
password: 233666
```

如需修改，请同步更新以下文件：
- `admin/src/main/resources/shardingsphere-config-dev.yaml`
- `project/src/main/resources/shardingsphere-config-dev.yaml`
- `aggregation/src/main/resources/shardingsphere-config-dev.yaml`

#### 启动 Redis

确保 Redis 服务已启动，默认连接 `127.0.0.1:6379`。

如需修改，请在各模块的 `application.yaml` 中更新：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      # password: your_password
```

#### 启动 Nacos

确保 Nacos 服务已启动，默认连接 `127.0.0.1:8848`。

如需修改，请在各模块的 `application.yaml` 中更新：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

### 3. 初始化数据库表

执行项目中的 SQL 建表脚本（如项目提供了 init.sql），创建分表后的实际表结构（t_user_0 ~ t_user_15, t_group_0 ~ t_group_15 等）。

### 4. 启动后端服务

#### 方式一：微服务模式（推荐生产环境）

在项目根目录执行构建：

```bash
mvn clean package -DskipTests
```

按顺序启动各服务：

```bash
# 1. 启动 Admin 服务（端口 8002）
java -jar admin/target/shortlink-admin-1.0-SNAPSHOT.jar

# 2. 启动 Project 服务（端口 8001）
java -jar project/target/shortlink-project-1.0-SNAPSHOT.jar

# 3. 启动 Gateway 网关，使用 dev profile（端口 8000）
java -jar gateway/target/shortlink-gateway-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

或者使用 Maven 插件直接运行：

```bash
# 启动 Admin
mvn -pl admin spring-boot:run

# 启动 Project
mvn -pl project spring-boot:run

# 启动 Gateway（dev 模式）
mvn -pl gateway spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 方式二：聚合服务模式（推荐小规模部署）

```bash
mvn clean package -DskipTests

# 1. 启动 Aggregation 服务（端口 8003）
java -jar aggregation/target/shortlink-aggregation-1.0-SNAPSHOT.jar

# 2. 启动 Gateway 网关，使用 aggregation profile（端口 8000）
java -jar gateway/target/shortlink-gateway-1.0-SNAPSHOT.jar --spring.profiles.active=aggregation
```

### 5. 启动前端

```bash
cd console-vue

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端开发服务器默认运行在 `http://localhost:8990`，API 请求代理到 Gateway `http://localhost:8000`。

### 6. 访问系统

- **前端控制台**：`http://localhost:8990`
- **Gateway 网关**：`http://localhost:8000`
- **Admin 服务**：`http://localhost:8002`
- **Project 服务**：`http://localhost:8001`
- **Aggregation 服务**：`http://localhost:8003`

---

## 配置说明

### 数据库环境切换

通过 `database.env` 变量切换数据库配置环境（默认 dev）：

```bash
java -jar admin/target/shortlink-admin-1.0-SNAPSHOT.jar --database.env=prod
```

对应的配置文件：
- `shardingsphere-config-dev.yaml` — 开发环境
- `shardingsphere-config-prod.yaml` — 生产环境

### 短链接域名配置

在 `project` 或 `aggregation` 服务的 `application.yaml` 中：

```yaml
short-link:
  domain:
    default: nurl.ink:8001  # 短链接默认域名
```

### 流量风控配置

在 `admin` 或 `aggregation` 服务的 `application.yaml` 中：

```yaml
short-link:
  flow-limit:
    enable: true          # 是否启用限流
    time-window: 1        # 时间窗口（秒）
    max-access-count: 20  # 窗口内最大访问次数
```

### 演示模式

在 `aggregation` 服务的 `application.yaml` 中：

```yaml
short-link:
  demo-mode:
    enable: false  # 是否启用演示模式
    blacklist:     # 演示模式下禁用的 API 路径
      - /api/short-link/admin/v1/group
      - /api/short-link/admin/v1/recycle-bin/remove
      # ...
```

---

## 主要端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8000 | API 网关统一入口 |
| project | 8001 | 核心短链接服务 |
| admin | 8002 | 管理端服务 |
| aggregation | 8003 | 聚合服务（可选） |
| console-vue | 8990 | 前端开发服务器 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 注册中心 |