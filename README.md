# HMoneta

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5.22-brightgreen.svg)](https://vuejs.org/)
[![Vuetify](https://img.shields.io/badge/Vuetify-3.10.5-blue.svg)](https://vuetifyjs.com/)

**基于 Spring Boot 的 DNS 动态更新服务（DDNS）**

[快速开始](#快速开始) • [功能特性](#功能特性) • [技术栈](#技术栈) • [API文档](docs/api-documentation.md) • [项目架构](docs/architecture.md)

</div>

## 项目概述

HMoneta 是一个基于 Spring Boot 的 DNS 动态更新服务（DDNS），主要用于监控公网 IP 变化并自动更新 DNS 记录。该项目支持多种 DNS 供应商（通过插件系统），具有前端界面（HMfront）和后端 API，使用 PostgreSQL 作为数据库。项目还集成了 ACME 协议支持，可以自动申请和管理 SSL 证书。系统提供了完整的用户认证、实时日志监控、插件管理和定时任务功能。

## 功能特性

### 核心功能
- 🔄 **动态 DNS 更新**: 定时检测公网 IP 变化，自动更新 DNS 解析记录
- 🔌 **多 DNS 提供商支持**: 通过插件系统支持不同的 DNS 服务提供商
- 🌐 **Web 管理界面**: 提供前端界面进行 DNS 配置和管理
- 📡 **WebSocket 实时日志**: 通过 WebSocket 实现实时日志推送
- 🔐 **用户认证**: JWT 基础的用户认证系统，自动创建默认管理员账户
- 🛡️ **ACME 证书管理**: 支持通过 ACME 协议自动申请和管理 SSL 证书

### 高级功能
- 📊 **证书有效期管理**: 自动存储和管理证书有效期信息
- 📦 **证书下载功能**: 支持下载打包的证书文件（ZIP格式）
- 🏷️ **敏感字段过滤**: 通过@JwtExclude注解标记敏感字段
- 🔍 **MDC日志追踪**: 使用MDC实现日志ID追踪
- ⚙️ **环境配置控制**: 使用Spring Profiles控制功能启用
- 🔄 **异步任务日志**: 支持异步任务日志记录和查询
- 🔄 **ACME 证书申请重试机制**: 包含登录重试和网络异常处理

## 技术栈

### 后端
- **框架**: Spring Boot 4.0.0
- **语言**: Java 25
- **数据库**: PostgreSQL + JPA/Hibernate
- **构建**: Maven
- **插件系统**: PF4J + Spring Boot Integration
- **认证**: JWT + MD5加盐加密
- **定时任务**: Spring Scheduling
- **实时通信**: WebSocket
- **证书管理**: ACME4J (3.5.0)
- **配置管理**: Spring Dotenv (4.0.0)
- **日志系统**: SLF4J + Logback

### 前端
- **框架**: Vue 3 (3.5.22)
- **UI组件库**: Vuetify 3 (3.10.5)
- **构建工具**: Vite 7 (7.1.5)
- **状态管理**: Pinia (3.0.3)
- **包管理**: Yarn 4.10.3
- **路由**: Vue Router (4.5.1)

## 快速开始

### 环境要求

- **后端**: Java JDK 25+, Maven 3.x
- **前端**: Node.js 18+, Yarn 4.10.3
- **数据库**: PostgreSQL 12+

### 1. 克隆项目

```bash
git clone https://github.com/HMoneta/HMoneta.git
cd HMoneta
```

### 2. 后端配置

#### 环境变量配置

创建 `src/main/resources/application-dev.yml` 或设置环境变量：

```yaml
server:
  port: 8080
spring:
  application:
    name: HMoneta
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

jwt:
  secret: povTvh!U7e9aJCwLUmp^qIVRCrgOAa=a
  expiration: 86400

acme:
  url: https://acme-staging-v02.api.letsencrypt.org/directory
```

#### 构建和运行

```bash
# 构建项目
./mvnw clean package

# 运行应用
./mvnw spring-boot:run

# 或者运行JAR文件
java -jar target/HMoneta-0.0.1-Alpha.jar
```

### 3. 前端配置

#### 环境变量配置

创建 `HMfront/hm-front/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080/hm
VITE_WS_BASE_URL=ws://localhost:8080/ws/logs
```

#### 构建和运行

```bash
cd HMfront/hm-front

# 安装依赖
yarn install

# 开发模式运行
yarn dev

# 构建生产版本
yarn build

# 代码检查
yarn lint
```

### 4. 访问应用

- **前端界面**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API文档**: [docs/api-documentation.md](docs/api-documentation.md)

默认管理员账户将在首次启动时自动创建。

## 项目结构

```
HMoneta/
├── src/main/java/fan/summer/hmoneta/     # 后端源码
│   ├── controller/                        # API控制器
│   ├── service/                           # 业务逻辑层
│   ├── database/                          # 数据访问层
│   ├── common/                            # 通用组件
│   └── websocket/                         # WebSocket处理
├── HMfront/hm-front/                      # 前端源码
│   ├── src/pages/                         # 页面组件
│   ├── src/components/                    # 通用组件
│   ├── src/stores/                        # 状态管理
│   └── src/router/                        # 路由配置
├── docs/                                  # 项目文档
│   ├── api-documentation.md               # API文档
│   ├── architecture.md                    # 架构文档
│   └── update-history.md                  # 更新历史
├── api-test/                              # API测试文件
└── plugins/                               # 插件目录
```

## API 端点

### DNS 管理
- `GET /hm/dns/query_all` - 获取所有DNS提供商
- `GET /hm/dns/resolve_info` - 查询所有DNS解析记录
- `POST /hm/dns/insert_group` - 插入DNS解析分组
- `POST /hm/dns/modify_group` - 修改DNS解析分组
- `POST /hm/dns/url/modify` - 修改DNS解析URL
- `POST /hm/dns/url/delete` - 删除DNS解析URL

### 用户管理
- `POST /hm/user/login` - 用户登录
- `GET /hm/user/valid` - 验证用户token有效性

### 插件管理
- `POST /hm/plugin/upload` - 上传插件文件

### ACME 管理
- `POST /hm/acme/modify` - 修改 ACME 用户信息
- `GET /hm/acme/apply` - 申请 SSL 证书
- `GET /hm/acme/download-cert/{domain}` - 下载证书包

### WebSocket
- `ws://服务器地址/ws/logs` - 实时日志推送

## 插件系统

HMoneta支持通过插件扩展DNS提供商功能。插件需要实现`HmDnsProviderPlugin`接口。

更多详细信息请参考：
- [插件开发文档](docs/architecture.md#222-插件管理模块)
- [HMoneta-Official-Plugin-Api](https://github.com/HMoneta/HMoneta-Official-Plugin-Api)

## 开发指南

### 代码规范
- 使用 Lombok 简化代码
- 使用 JPA 进行数据库操作
- 使用统一的异常处理机制
- 遵循 RESTful API 设计原则
- 前端使用 ESLint 进行代码质量检查

### 安全实践
- API 端点统一使用 `/hm` 前缀
- 使用AOP记录API访问日志
- 密码使用MD5加盐加密存储
- JWT工具类支持敏感字段过滤

### 部署建议
- 使用环境变量管理敏感配置
- 配置适当的数据库连接池
- 启用HTTPS和防火墙
- 定期备份数据库

## 许可证

本项目采用 [MIT 许可证](LICENSE)。

## 贡献

欢迎提交 Issue 和 Pull Request 来帮助改进项目！

### 贡献指南
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 联系方式

- **项目地址**: [https://github.com/HMoneta/HMoneta](https://github.com/HMoneta/HMoneta)
- **问题反馈**: [https://github.com/HMoneta/HMoneta/issues](https://github.com/HMoneta/HMoneta/issues)
- **官方文档**: [https://hmoneta.github.io/HMoneta/](https://hmoneta.github.io/HMoneta/)

## 致谢

感谢所有为HMoneta项目做出贡献的开发者和用户。

---

<div align="center">

**致力于每人都可以玩转家庭服务**

</div>