# HMoneta 项目架构文档

## 1. 整体架构

HMoneta 项目遵循前后端分离架构设计，由后端API服务和前端Web界面组成。

### 1.1 架构图

```
┌─────────────────┐    HTTP/HTTPS    ┌─────────────────┐
│   前端界面       │ ──────────────→  │   后端服务      │
│   (Vue 3)      │                  │  (Spring Boot)  │
└─────────────────┘                  └─────────────────┘
                                              │
                                      WebSocket│
                                              ▼
                                      ┌─────────────────┐
                                      │   实时日志推送   │
                                      └─────────────────┘
                                              │
                                          数据库操作
                                              ▼
                                      ┌─────────────────┐
                                      │   PostgreSQL    │
                                      └─────────────────┘
```

### 1.2 技术栈

- **后端**: Spring Boot 4.0.0, Java 25
- **前端**: Vue 3 (3.5.22), Vuetify 3 (3.10.5), Vite 7 (7.1.5), Pinia (3.0.3), Yarn 4.10.3
- **数据库**: PostgreSQL, JPA/Hibernate
- **插件系统**: PF4J (Plugin Framework for Java) + Spring Boot Integration
- **安全**: JWT 认证, MD5 密码加密 (通过MD5 + salt实现)
- **定时任务**: Spring Scheduling
- **实时通信**: WebSocket
- **证书管理**: ACME4J (3.5.0)
- **配置管理**: Spring Dotenv (4.0.0)
- **工具库**: Apache Commons Text, Commons Validator, Bouncy Castle (bcpkix-jdk18on), Hypersistence Utils for Hibernate, tools.jackson (for JSON processing)
- **日志追踪**: MDC (Mapped Diagnostic Context) 用于日志ID追踪
- **敏感信息过滤**: @JwtExclude 注解标记敏感字段

## 2. 后端架构

### 2.1 包结构

```
src/main/java/fan/summer/hmoneta/
├── HMonetaApplication.java           # 应用入口
├── common/                         # 通用组件
│   ├── HMBanner.java               # Banner显示
│   ├── aspect/                     # AOP切面
│   │   └── ApiAspect.java          # API日志切面
│   ├── config/                     # 配置类
│   │   ├── CorsConfig.java         # CORS配置
│   │   ├── HMInterceptor.java      # 拦截器配置
│   │   ├── PluginConfiguration.java # 插件配置
│   │   └── WebSocketConfig.java    # WebSocket配置
│   ├── enums/                      # 枚举类
│   │   └── exception/              # 异常枚举
│   │       ├── HMExceptionEnum.java # 基础异常枚举
│   │       ├── acme/               # ACME相关异常枚举
│   │       ├── dns/                # DNS相关异常枚举
│   │       ├── plugin/             # 插件相关异常枚举
│   │       └── user/               # 用户相关异常枚举
│   ├── exception/                  # 异常处理
│   │   └── HMException.java        # 自定义异常
│   └── interceptor/                # 拦截器
│       └── ApiInterceptor.java     # API拦截器
├── controller/                     # 控制器层
│   ├── acme/                       # ACME相关控制器
│   │   └── AcmeController.java     # ACME证书管理控制器
│   ├── dns/                        # DNS相关控制器
│   │   └── DnsController.java      # DNS管理控制器
│   ├── plugin/                     # 插件相关控制器
│   │   └── PluginController.java   # 插件管理控制器
│   └── user/                       # 用户相关控制器
│       └── UserController.java     # 用户管理控制器
├── database/                       # 数据库相关
│   ├── entity/                     # 实体类
│   │   ├── acme/                   # ACME相关实体
│   │   │   ├── AcmeAsyncLogEntity.java        # ACME异步任务日志实体
│   │   │   ├── AcmeCertificationEntity.java   # ACME证书认证实体
│   │   │   └── AcmeUserInfoEntity.java        # ACME用户信息实体
│   │   ├── dns/                    # DNS相关实体
│   │   │   ├── DnsProviderEntity.java         # DNS提供商实体
│   │   │   ├── DnsResolveGroupEntity.java     # DNS解析分组实体
│   │   │   └── DnsResolveUrlEntity.java       # DNS解析URL实体
│   │   └── user/                   # 用户相关实体
│   │       └── UserEntity.java     # 用户实体
│   └── repository/                 # JPA仓库接口
│       ├── acme/                   # ACME相关仓库
│       ├── dns/                    # DNS相关仓库
│       └── user/                   # 用户相关仓库
├── service/                        # 业务逻辑层
│   ├── acme/                       # ACME服务
│   │   ├── AcmeService.java        # ACME证书申请服务
│   │   ├── AcmeServiceComponent.java # ACME证书申请核心组件
│   │   └── AcmeTaskContext.java    # ACME任务上下文
│   ├── dns/                        # DNS服务
│   │   └── DnsService.java         # DNS解析管理服务
│   ├── plugin/                     # 插件服务
│   │   └── PluginService.java      # 插件管理服务
│   └── user/                       # 用户服务
│       └── UserService.java        # 用户认证管理服务
├── task/                           # 定时任务
│   └── dns/                        # DNS相关任务
│       └── DnsUpdateTask.java      # DNS更新定时任务
├── util/                           # 工具类
│   ├── IpUtil.java                 # IP工具类
│   ├── JwtUtil.java                # JWT工具类
│   ├── Md5Util.java                # MD5工具类
│   ├── ObjectUtil.java             # 对象工具类
│   └── WebUtil.java                # Web工具类
└── websocket/                      # WebSocket相关
    └── log/                        # 日志相关
        ├── LogWebSocketHandler.java    # 日志WebSocket处理器
        └── WebSocketLogAppender.java   # WebSocket日志追加器
```

### 2.2 核心模块

#### 2.2.1 DNS管理模块 (`/dns`)

**职责**：
- 管理DNS解析记录
- 支持分组管理DNS解析
- 提供DNS解析更新API

**核心组件**：
- `DnsController`: DNS相关API入口
- `DnsService`: DNS业务逻辑处理
- `DnsUpdateTask`: DNS更新定时任务（每10分钟检查一次公网IP并更新DNS记录）

#### 2.2.2 插件管理模块 (`/plugin`)

**职责**：
- 管理DNS提供商插件
- 支持插件的加载、卸载、启动、停止
- 通过PF4J框架实现插件化架构

**核心组件**：
- `PluginController`: 插件管理API入口
- `PluginService`: 插件业务逻辑处理
- `HmDnsProviderPlugin`: DNS提供商插件接口

#### 2.2.3 用户管理模块 (`/user`)

**职责**：
- 用户认证和授权
- JWT Token管理
- 密码加密处理

**核心组件**：
- `UserController`: 用户管理API入口
- `UserService`: 用户业务逻辑处理
- `JwtUtil`: JWT工具类
- `Md5Util`: MD5加盐加密工具类

#### 2.2.4 ACME证书管理模块 (`/acme`)

**职责**：
- SSL证书申请和管理
- DNS-01挑战验证
- 证书自动续期
- 证书打包下载
- 异步任务日志管理
- 证书有效期管理和API查询
- 证书下载功能
- 敏感字段过滤和MDC日志追踪

**核心组件**：
- `AcmeController`: ACME相关API控制器
  - `POST /hm/acme/modify`: 修改ACME用户信息
  - `GET /hm/acme/apply`: 申请SSL证书
  - `GET /hm/acme/download-cert/{domain}`: 下载指定域名的证书包（ZIP格式）
- `AcmeService`: ACME证书申请服务
- `AcmeServiceComponent`: ACME证书申请核心组件，包含异步证书申请实现
- `AcmeTaskContext`: ACME任务上下文，用于跟踪异步申请过程
- `AcmeCertificationEntity`: ACME证书认证实体，包含证书有效期信息(notBefore/notAfter)
- `AcmeUserInfoEntity`: ACME用户信息实体
- `AcmeAsyncLogEntity`: ACME异步任务日志实体，用于跟踪异步证书申请任务的状态和日志信息
- `AcmeCerInfoResp`: ACME证书信息响应实体，包含证书有效期信息(notBefore/notAfter)

**新增功能 (V0.0.1-Alpha)**:
- **证书有效期管理**: 自动存储证书有效期信息（notBefore/notAfter）
- **证书下载功能**: 支持ZIP格式证书包下载，包含.key、.crt、.pem、.fullchain.pem等格式
- **敏感字段过滤**: 通过@JwtExclude注解标记敏感字段，防止敏感信息泄露
- **MDC日志追踪**: 使用MDC（Mapped Diagnostic Context）实现日志ID追踪，便于调试和问题定位
- **异步任务重试机制**: 包含登录重试（最多3次）和DNS传播验证（最多10次）
- **自动清理功能**: 证书申请完成后自动清理验证用DNS记录

#### 2.2.5 WebSocket日志模块 (`/websocket`)

**职责**：
- 实时日志推送
- WebSocket连接管理
- 日志信息传输

**核心组件**：
- `LogWebSocketHandler`: 日志WebSocket处理器
- `WebSocketLogAppender`: WebSocket日志追加器

### 2.3 数据库设计

#### 2.3.1 实体关系图

```
UserEntity (1) -- (n) DnsProviderEntity (1) -- (n) DnsResolveGroupEntity (1) -- (n) DnsResolveUrlEntity
    │                  │                          │                              │
    │                  │                          │                              │
    │                  └──────────────────────────┼──────────────────────────────┘
    │                                             │
    └─────────────────────────────────────────────┘
AcmeUserInfoEntity (1) -- (n) AcmeCertificationEntity -- (n) AcmeAsyncLogEntity
```

#### 2.3.2 主要实体说明

- **UserEntity**: 用户信息表，存储系统用户信息
- **DnsProviderEntity**: DNS提供商表，存储支持的DNS提供商信息
- **DnsResolveGroupEntity**: DNS解析分组表，将DNS解析记录进行分组管理
- **DnsResolveUrlEntity**: DNS解析URL表，存储具体的DNS解析记录
- **AcmeUserInfoEntity**: ACME用户信息表，存储ACME账户信息
- **AcmeCertificationEntity**: ACME证书认证信息表，存储证书申请过程信息
- **AcmeAsyncLogEntity**: ACME异步任务日志表，用于跟踪异步证书申请任务的状态和日志信息

## 3. 前端架构

### 3.1 包结构

```
HMfront/hm-front/src/
├── App.vue                         # 根组件
├── main.js                         # 应用入口
├── assets/                         # 静态资源
│   ├── logo.png
│   └── logo.svg
├── common/                         # 公共模块
│   └── request.js                  # API请求配置和封装
├── components/                     # 通用组件
│   ├── AppFooter.vue               # 页脚组件
│   └── README.md
├── layouts/                        # 布局组件
│   ├── default.vue                 # 默认布局
│   └── README.md
├── pages/                          # 页面组件
│   ├── about.vue                   # 关于页面
│   ├── dns.vue                     # DNS管理页面
│   ├── login.vue                   # 登录页面
│   ├── loginNew.vue                # 新登录页面
│   ├── logPage.vue                 # 日志页面
│   ├── pluginManager.vue           # 插件管理页面
│   ├── setting.vue                 # 设置页面
│   └── README.md
├── plugins/                        # 插件配置
│   ├── index.js
│   ├── README.md
│   └── vuetify.js
├── router/                         # 路由配置
│   └── index.js
├── stores/                         # 状态管理 (Pinia)
│   ├── app.js                      # 应用状态
│   └── index.js
└── styles/                         # 样式文件
    ├── README.md
    └── settings.scss
```

### 3.2 核心模块

#### 3.2.1 页面组件

- **dns.vue**: DNS管理页面，提供DNS解析记录的增删改查功能
- **login.vue/loginNew.vue**: 登录页面，用户认证入口
- **pluginManager.vue**: 插件管理页面，管理DNS提供商插件
- **logPage.vue**: 日志页面，显示实时日志信息
- **setting.vue**: 设置页面，系统配置功能

#### 3.2.2 状态管理 (Pinia)

- **stores/app.js**: 应用级别的状态管理

#### 3.2.3 网络请求

- **common/request.js**: 封装API请求，统一处理认证、错误等

#### 3.2.4 UI组件库

使用Vuetify 3作为UI组件库，提供Material Design风格的界面组件。

## 4. 配置文件

### 4.1 应用配置

- **application.yml/application-dev.yml**: Spring Boot配置文件
  - 数据库连接配置
  - 服务器端口配置
  - JWT配置
  - 任务调度配置
  - 文件上传配置
  - ACME服务配置

### 4.2 前端环境配置

- **.env.development/.env.production**: 前端环境变量配置
  - API基础URL
  - WebSocket基础URL

## 5. 安全机制

### 5.1 认证机制

- 使用JWT Token进行用户认证
- 通过拦截器验证Token有效性
- Token有效期配置

### 5.2 数据库安全

- 敏感信息通过环境变量配置
- 密码使用MD5加盐加密存储

### 5.3 API安全

- 通过CORS配置控制跨域访问
- API访问权限控制
- 输入参数验证

## 6. 部署架构

### 6.1 部署方式

- 后端服务打包为JAR文件运行
- 前端构建为静态文件部署到Web服务器（如Nginx）

### 6.2 运行依赖

- PostgreSQL数据库
- Java运行环境（JDK 25）
- Node.js和yarn（前端构建）

## 7. 扩展性设计

### 7.1 插件化架构

通过PF4J框架实现插件化，支持动态加载DNS提供商插件，便于扩展对不同DNS服务商的支持。

### 7.2 模块化设计

采用分层架构和模块化设计，各模块职责清晰，便于维护和扩展。

### 7.3 配置化

通过配置文件和环境变量实现灵活配置，支持不同环境的部署需求。

## 8. 异步任务处理

### 8.1 ACME证书申请异步处理

项目中ACME证书申请采用了异步处理机制，使用@Async注解实现。为了解决异步处理中的日志查询问题，项目采用了以下方案：

1. **异步任务日志实体**: 通过`AcmeAsyncLogEntity`实体存储异步任务日志信息，包含任务ID、域名、日志信息和执行状态
2. **任务状态跟踪**: 在异步方法中通过TaskID跟踪任务状态，更新日志信息到数据库
3. **日志查询API**: 提供API端点查询特定任务的日志信息
4. **实时推送**: 通过WebSocket将异步任务的执行状态实时推送到前端

### 8.2 异步处理优化

- 证书申请过程包含登录重试机制，最多重试3次
- DNS传播验证使用最多10次尝试验证，每次间隔10秒
- 证书申请过程包含自动清理验证用DNS记录功能
- 增强了异步任务中的网络异常处理机制

## 9. 定时任务配置

项目使用Spring Scheduling框架进行定时任务执行。
- `DnsUpdateTask`: DNS更新定时任务，每10分钟（600000毫秒）执行一次，仅在非开发环境（!dev profile）下运行
- 任务线程池使用HMScheduling-前缀命名
- 支持任务执行日志记录和异常处理