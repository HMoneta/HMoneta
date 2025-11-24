# HMoneta官方文档

## 项目概述

HMoneta 是一个基于 Spring Boot 的 DNS 动态更新服务（DDNS），主要用于监控公网 IP 变化并自动更新 DNS 记录。该项目支持多种 DNS 供应商（通过插件系统），具有前端界面（HMfront）和后端 API，使用 PostgreSQL 作为数据库。项目还集成了 ACME 协议支持，可以自动申请和管理 SSL 证书。

## 技术栈

- **后端**: Spring Boot 4.0.0, Java 25
- **前端**: Vue 3, Vuetify 3, Vite 7, Pinia
- **数据库**: PostgreSQL, JPA/Hibernate
- **依赖管理**: Maven
- **插件系统**: PF4J (Plugin Framework for Java) + Spring Boot Integration
- **安全**: JWT 认证, BCrypt 密码加密
- **定时任务**: Spring Scheduling
- **实时通信**: WebSocket
- **证书管理**: ACME4J (用于 SSL 证书申请和管理)
- **配置管理**: Spring Dotenv
- **日志系统**: SLF4J + Logback
- **API**: RESTful API

## 核心功能

1. **动态 DNS 更新**: 定时检测公网 IP 变化，自动更新 DNS 解析记录
2. **多 DNS 提供商支持**: 通过插件系统支持不同的 DNS 服务提供商
3. **Web 管理界面**: 提供前端界面进行 DNS 配置和管理
4. **WebSocket 实时日志**: 通过 WebSocket 实现实时日志推送
5. **用户认证**: JWT 基础的用户认证系统
6. **ACME 证书管理**: 支持通过 ACME 协议自动申请和管理 SSL 证书
7. **DNS 解析分组管理**: 支持将多个 DNS 解析记录分组管理

## 快速开始

### 1.环境准备

- **后端环境**:
  - Java JDK 版本为 25
  - Maven 3.x 作为项目管理器
- **前端环境**:
  - Node.js 18+ 和 yarn 4.10.3 作为包管理器

> 使用Java 25 LTS作为开发语言，是为了后期更好地使用Spring Boot 4

### 2.从Github拉取项目源码

```bash
git clone https://github.com/HMoneta/HMoneta.git
```

### 3.前端工程编译

```bash
# 进入前端工程
cd HMfront/hm-front
# 安装依赖
yarn install
# 创建环境配置文件
cp .env.example .env.development
```

请在环境文件中添加如下环境变量

```env
VITE_API_BASE_URL=http://后端服务器地址/hm
VITE_WS_BASE_URL=ws://后端服务器地址/ws/logs
```

```bash
# 编译前端工程
yarn build
# 或者开发模式运行
yarn dev
```

编译产物将生成在dist文件夹中，将文件夹中产物部署在Nginx或其他Web服务器上即可

### 4.后端工程编译和运行

```bash
# 使用 Maven Wrapper 构建
./mvnw clean package

# 运行应用
./mvnw spring-boot:run

# 或者运行打包后的 JAR
java -jar target/HMoneta-0.0.1-SNAPSHOT.jar
```

### 环境配置

需要在 `src/main/resources/application-dev.yml` 或通过环境变量配置以下参数：

```yaml
DB_URL: PostgreSQL 数据库连接 URL
DB_USERNAME: 数据库用户名
DB_PASSWORD: 数据库密码
hmoneta.acme.uri: ACME 服务提供商 URI (如 Let's Encrypt)
```

## 项目架构

### 后端结构

- `src/main/java/fan/summer/hmoneta/`: 主要的 Java 源代码
  - `controller/`: API 控制器，包括 DNS、插件、用户和 ACME 管理
    - `dns/`: DNS 相关 API 控制器
    - `plugin/`: 插件管理 API 控制器
    - `user/`: 用户管理 API 控制器
  - `service/`: 业务逻辑层
    - `acme/`: ACME 证书申请和管理服务
    - `dns/`: DNS 解析、分组管理服务
    - `plugin/`: 插件管理服务
    - `user/`: 用户认证和管理服务
  - `database/`: JPA 实体和存储库
    - `entity/`: JPA 实体类
      - `acme/`: ACME 相关实体 (AcmeChallengeInfoEntity, AcmeUserInfoEntity)
      - `dns/`: DNS 相关实体 (DnsProviderEntity, DnsResolveGroupEntity, DnsResolveUrlEntity)
      - `user/`: 用户相关实体 (UserEntity)
    - `repository/`: JPA 存储库接口
  - `task/`: 定时任务
    - `dns/`: DNS 更新任务 (DnsUpdateTask.java)
  - `websocket/`: WebSocket 处理器，用于实时日志推送
    - `log/`: 日志 WebSocket 处理器和追加器
  - `common/`: 通用配置、枚举、异常处理、切面和配置类
  - `util/`: 工具类 (IpUtil, JwtUtil, Md5Util, ObjectUtil, WebUtil)
  - `HMonetaApplication.java`: 应用程序入口

### 前端结构

- `HMfront/hm-front/`: Vue 3 前端项目
  - `src/pages/`: 页面组件 (dns.vue, login.vue, pluginManager.vue, logPage.vue, setting.vue)
  - `src/components/`: 通用组件
  - `src/common/request.js`: API 请求配置和封装
  - `src/stores/`: Pinia 状态管理
  - `src/router/`: Vue Router 配置
  - `src/plugins/`: Vuetify 等插件配置

## API 文档

### DNS 管理 API

- `GET /hm/dns/query_all`: 获取所有DNS提供商
- `GET /hm/dns/resolve_info`: 查询所有DNS解析记录
- `POST /hm/dns/insert_group`: 插入DNS解析分组
- `POST /hm/dns/modify_group`: 修改DNS解析分组
- `POST /hm/dns/url/modify`: 修改DNS解析URL
- `POST /hm/dns/url/delete`: 删除DNS解析URL

### 用户管理 API

- `POST /hm/user/login`: 用户登录
- `GET /hm/user/valid`: 验证用户token有效性

### 插件管理 API

- `POST /hm/plugin/upload`: 上传插件文件

### WebSocket 实时日志 API

- `ws://服务器地址/ws/logs`: WebSocket连接端点，用于实时日志推送

### ACME 证书管理服务

ACME证书管理功能通过`AcmeService`实现，但尚未暴露为控制器API端点。主要功能包括：
- 通过DNS-01挑战验证申请SSL证书
- 证书自动续期
- 证书打包下载功能
- ACME挑战信息管理

## 插件系统

HMoneta支持自定义插件，插件均需实现*HMoneta-Official-Plugin-Api*
> HMoneta-Official-Plugin-Api [项目地址](https://github.com/HMoneta/HMoneta-Official-Plugin-Api)

## 开发约定

- 使用 Lombok 简化代码
- 使用 JPA 进行数据库操作
- 使用统一的异常处理机制（HMException, HMExceptionEnum）
- 遵循 RESTful API 设计原则
- 使用日志记录关键操作和错误信息
- 使用 JWT 进行用户认证
- 使用 Spring Boot 的配置属性进行配置管理
- 使用 PF4J 插件框架扩展功能
- ACME 证书管理遵循 RFC 8555 标准
