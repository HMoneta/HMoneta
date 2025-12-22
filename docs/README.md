# HMoneta官方文档

## 项目概述

HMoneta 是一个基于 Spring Boot 的 DNS 动态更新服务（DDNS），主要用于监控公网 IP 变化并自动更新 DNS 记录。该项目支持多种 DNS 供应商（通过插件系统），具有前端界面（HMfront）和后端 API，使用 PostgreSQL 作为数据库。项目还集成了 ACME 协议支持，可以自动申请和管理 SSL 证书。系统提供了完整的用户认证、实时日志监控、插件管理和定时任务功能。

## 技术栈

- **后端**: Spring Boot 4.0.0, Java 25
- **前端**: Vue 3 (3.5.22), Vuetify 3 (3.10.5), Vite 7 (7.1.5), Pinia (3.0.3), Yarn 4.10.3
- **数据库**: PostgreSQL, JPA/Hibernate
- **依赖管理**: Maven
- **插件系统**: PF4J (Plugin Framework for Java) + Spring Boot Integration
- **安全**: JWT 认证, MD5 密码加密 (通过MD5 + salt实现)
- **定时任务**: Spring Scheduling
- **实时通信**: WebSocket
- **证书管理**: ACME4J (用于 SSL 证书申请和管理)
- **配置管理**: Spring Dotenv
- **日志系统**: SLF4J + Logback
- **API**: RESTful API
- **工具库**: Apache Commons Text, Commons Validator, Bouncy Castle, Hypersistence Utils for Hibernate, tools.jackson (for JSON processing)

## 核心功能

### 基础功能
1. **动态 DNS 更新**: 定时检测公网 IP 变化，自动更新 DNS 解析记录
2. **多 DNS 提供商支持**: 通过插件系统支持不同的 DNS 服务提供商
3. **Web 管理界面**: 提供前端界面进行 DNS 配置和管理
4. **WebSocket 实时日志**: 通过 WebSocket 实现实时日志推送
5. **用户认证**: JWT 基础的用户认证系统，自动创建默认管理员账户
6. **ACME 证书管理**: 支持通过 ACME 协议自动申请和管理 SSL 证书，包含通过 DNS-01 挑战验证申请证书的功能
7. **DNS 解析分组管理**: 支持将多个 DNS 解析记录分组管理
8. **插件管理**: 支持动态插件加载、卸载、启动和停止，支持ZIP格式插件上传

### 高级功能 (V0.0.1-Alpha)
9. **证书有效期管理功能**: 自动存储和管理证书有效期信息（notBefore/notAfter），在DNS解析信息中展示证书有效期，提供证书有效期API查询接口
10. **证书下载功能**: 支持下载打包的证书文件（ZIP格式），包含.key、.crt、.pem、.fullchain.pem等格式文件
11. **敏感字段过滤功能**: 通过@JwtExclude注解标记敏感字段，防止敏感信息泄露
12. **MDC日志追踪功能**: 使用MDC（Mapped Diagnostic Context）实现日志ID追踪，便于调试和问题定位
13. **异步任务日志**: 支持异步任务日志记录和查询，通过AcmeAsyncLogEntity实体管理异步任务日志
14. **ACME 证书申请重试机制**: 包含登录重试和网络异常处理机制
15. **环境配置控制**: 使用Spring Profiles控制功能启用（如定时任务仅在非dev环境运行）
16. **实时日志监控**: 支持按服务订阅日志，支持订阅所有日志
17. **证书申请与打包**: 支持证书申请、生成完整证书链和打包功能
18. **证书有效期API**: 提供证书有效期查询接口，在DNS解析信息中展示证书有效期

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
java -jar target/HMoneta-0.0.1-Alpha.jar
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
  - `common/`: 通用配置、枚举、异常处理、切面和配置类
    - `aspect/`: AOP切面 (ApiAspect.java)
    - `config/`: Spring配置类 (CorsConfig.java, HMInterceptor.java, PluginConfiguration.java, WebSocketConfig.java)
    - `enums/`: 枚举类
      - `exception/`: 异常枚举
        - `acme/`: ACME相关异常
        - `dns/`: DNS相关异常
        - `plugin/`: 插件相关异常
        - `user/`: 用户相关异常
    - `exception/`: 自定义异常类
    - `handle/`: 异常处理器 (ControllerExceptionHandler.java)
    - `interceptor/`: 拦截器 (ApiInterceptor.java)
  - `controller/`: API 控制器，包括 DNS、插件、用户管理
    - `acme/`: ACME 相关 API 控制器
      - `dto/`: ACME 请求和响应数据传输对象
    - `dns/`: DNS 相关 API 控制器
      - `entity/`: DNS 请求和响应实体类
        - `req/`: DNS 请求实体类
        - `resp/`: DNS 响应实体类
    - `plugin/`: 插件管理 API 控制器
    - `user/`: 用户管理 API 控制器
      - `entity/`: 用户相关请求实体类
  - `database/`: JPA 实体和存储库
    - `entity/`: JPA 实体类
      - `acme/`: ACME 相关实体 (AcmeAsyncLogEntity, AcmeCertificationEntity, AcmeUserInfoEntity)
      - `dns/`: DNS 相关实体 (DnsProviderEntity, DnsResolveGroupEntity, DnsResolveUrlEntity)
      - `user/`: 用户相关实体 (UserEntity)
    - `repository/`: JPA 存储库接口
  - `plugin/`: 插件API接口
    - `api/`: 插件API定义
      - `dns/`: DNS提供商插件接口 (HmDnsProviderPlugin.java)
  - `service/`: 业务逻辑层
    - `acme/`: ACME 证书申请和管理服务，支持通过 DNS-01 挑战自动申请 SSL 证书，包含证书打包和存储功能
    - `dns/`: DNS 解析、分组管理服务
    - `plugin/`: 插件管理服务
    - `user/`: 用户认证和管理服务
  - `task/`: 定时任务
    - `dns/`: DNS 更新任务 (DnsUpdateTask.java)
  - `util/`: 工具类 (IpUtil, JwtUtil, Md5Util, ObjectUtil, WebUtil)
  - `websocket/`: WebSocket 处理器，用于实时日志推送
    - `log/`: 日志 WebSocket 处理器和追加器
  - `HMonetaApplication.java`: 应用程序入口

### 前端结构

- `HMfront/hm-front/`: Vue 3 前端项目
  - `src/pages/`: 页面组件 (about.vue, dns.vue, index.vue, login.vue, loginNew.vue, logPage.vue, pluginManager.vue, setting.vue, HelloWorld.vue)
  - `src/components/`: 通用组件
  - `src/common/request.js`: API 请求配置和封装
  - `src/stores/`: Pinia 状态管理
  - `src/router/`: Vue Router 配置
  - `src/plugins/`: Vuetify 等插件配置
  - `src/layouts/`: 页面布局组件
  - `src/assets/`: 静态资源文件
  - `src/styles/`: 样式文件

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

### ACME 管理 API

- `POST /hm/acme/modify`: 修改 ACME 用户信息
- `GET /hm/acme/apply`: 申请 SSL 证书
- `GET /hm/acme/download-cert/{domain}`: 下载指定域名的证书包（ZIP格式）

### WebSocket 实时日志 API

- `ws://服务器地址/ws/logs`: WebSocket连接端点，用于实时日志推送

## 插件系统

HMoneta支持自定义插件，插件均需实现*HMoneta-Official-Plugin-Api*
> HMoneta-Official-Plugin-Api [项目地址](https://github.com/HMoneta/HMoneta-Official-Plugin-Api)

## 开发约定和最佳实践

### 代码规范
- 使用 Lombok 简化代码
- 使用 JPA 进行数据库操作
- 使用统一的异常处理机制（HMException, HMExceptionEnum）
- 遵循 RESTful API 设计原则
- 使用 JWT 进行用户认证
- 使用 Spring Boot 的配置属性进行配置管理
- 使用 PF4J 插件框架扩展功能
- ACME 证书管理遵循 RFC 8555 标准
- 前端使用 Vuetify 组件库保持 UI 一致性
- 前端使用 ESLint 进行代码质量检查

### 安全实践
- API 端点统一使用 `/hm` 前缀
- 前端请求统一使用 `VITE_API_BASE_URL` 环境变量作为基础路径
- 使用AOP记录API访问日志
- 密码使用MD5加盐加密存储
- JWT工具类支持敏感字段过滤，通过@JwtExclude注解标记敏感字段
- 使用CORS配置支持跨域请求

### 日志和监控
- 日志系统使用SLF4J + Logback
- 使用MDC进行日志追踪，便于调试和问题定位
- 通过ApiAspect实现AOP日志记录
- WebSocket日志系统支持按服务订阅和全量订阅

### 插件系统
- 插件存储在 `./plugins` 目录
- 插件系统通过HmDnsProviderPlugin接口扩展DNS提供商功能
- 支持ZIP格式插件上传
- 插件系统支持版本检查和自动更新数据库记录

### 证书管理
- ACME证书申请使用DNS-01挑战方式
- 证书文件存储在 `certs/{domain}` 目录下，包含.key、.crt、.pem、.fullchain.pem格式
- 证书申请过程通过_acme-challenge TXT记录进行DNS验证
- 证书申请完成后会清理验证用的DNS记录
- 证书打包功能支持ZIP格式压缩
- 新增证书下载功能，通过GET /hm/acme/download-cert/{domain}端点提供ZIP格式证书包下载

### 定时任务
- 使用Spring Scheduling实现定时任务
- 定时DNS更新任务间隔为10分钟（600000毫秒），仅在非开发环境运行
- 定时任务通过@Profile("!dev")注解限制只在非开发环境运行
- 定时任务使用Spring Scheduling，可配置线程池大小
- 定时任务线程池使用HMScheduling-前缀命名

### 数据库操作
- 数据库连接使用 HikariCP 连接池进行优化
- 修复了ACME证书申请过程中的数据库连接泄漏问题
- 优化了异步任务中的数据库操作异常处理
- 支持多环境配置 (dev, prod)

### 文件上传
- 支持文件上传功能，最大支持100MB
- 前端使用最新的 Vite 构建工具进行开发和构建

### 异常处理
- 异常处理使用ControllerExceptionHandler统一处理
- 增强了ACME证书申请过程中的网络异常处理
- 添加了ACME挑战获取的异常处理机制
- 强化了异步任务的异常捕获和处理机制

### 异步处理
- 异步任务通过AcmeAsyncLogEntity实体跟踪日志和状态，解决异步处理中的日志查询问题
- ACME证书申请支持登录重试机制，最多重试3次
- DNS传播验证使用最多10次尝试验证，每次间隔10秒

### 证书管理增强
- 证书申请过程包含自动清理验证用DNS记录功能
- 增加了证书有效期信息的数据库存储功能，通过AcmeCertificationEntity的notBefore和notAfter字段管理证书有效期
- 通过DnsResolveUrlResp实体的acmeCerInfo字段在DNS解析信息中展示证书有效期

### 前端架构
- 前端使用Vue 3 + Vite + Pinia架构
- 前端使用@vueuse/core进行WebSocket连接管理
- 前端代码质量通过ESLint进行规范检查
- 使用现代前端构建工具链和插件优化开发体验

### 连接管理
- 实现了WebSocket日志系统的ping/pong心跳机制，确保连接稳定性
- 优化了WebSocket连接管理，防止连接泄漏

### 错误处理
- 强化了前端和后端的错误处理机制
- 修复了腾讯云DNS插件中客户端未正确初始化的问题
- 强化了插件加载失败的错误日志记录

### 网络优化
- 增强了ACME证书申请过程中的网络超时设置
- 增加了对DNS更新失败情况的处理机制

### 存储优化
- 优化了证书文件的打包和存储逻辑
- 增加了对插件版本的管理和更新功能