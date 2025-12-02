# HMoneta 项目升级历史

## V0.0.1-SNAPSHOT (2025年12月)

### 新增功能
- 证书有效期管理功能
  - 自动存储证书有效期信息（notBefore/notAfter）
  - 在DNS解析信息中展示证书有效期
  - 提供证书有效期API查询接口
- 证书下载功能
  - 新增证书下载API：`GET /hm/acme/download-cert/{domain}`
  - 支持ZIP格式证书包下载
  - 包含.key、.crt、.pem、.fullchain.pem等格式文件

### 功能优化
- 证书有效期跟踪机制
  - 在AcmeCertificationEntity实体中添加notBefore和notAfter字段
  - 通过AcmeCerInfoResp响应实体传递证书有效期信息
  - 在DNS解析信息查询结果中包含证书有效期数据
- ACME证书申请异步处理机制
  - 使用@Async注解实现异步证书申请
  - 通过AcmeAsyncLogEntity实体跟踪异步任务日志
  - 支持任务状态跟踪和实时推送
  - 引入AcmeTaskContext任务上下文
- 插件管理功能增强
  - 支持ZIP格式插件上传
  - 插件版本检查和自动更新数据库记录
- WebSocket日志系统增强
  - 支持按服务订阅日志
  - 支持订阅所有日志
  - 实现ping/pong心跳机制

### 技术优化
- ACME证书申请过程优化
  - 添加登录重试机制，最多重试3次
  - DNS传播验证使用最多10次尝试验证，每次间隔10秒
  - 证书申请过程包含自动清理验证用DNS记录功能
- 安全机制改进
  - 从BCrypt改为MD5加盐加密存储密码
- 数据库操作优化
  - 修复了ACME证书申请过程中的数据库连接泄漏问题
  - 优化了异步任务中的数据库操作异常处理
- 异常处理增强
  - 增强了ACME证书申请过程中的网络异常处理
  - 添加了ACME挑战获取的异常处理机制

### 架构改进
- 定时任务配置优化
  - DNS更新任务仅在非开发环境运行
  - 任务线程池使用HMScheduling-前缀命名
- 证书管理功能增强
  - 证书文件存储在 `certs/{domain}` 目录下
  - 支持.key、.crt、.pem、.fullchain.pem等格式
  - 证书打包功能支持ZIP格式压缩
  - 证书有效期信息存储在数据库中便于管理

### API端点更新
- 新增 ACME 管理 API
  - `POST /hm/acme/modify`: 修改 ACME 用户信息
  - `GET /hm/acme/apply`: 申请 SSL 证书
  - `GET /hm/acme/download-cert/{domain}`: 下载指定域名的证书包（ZIP格式）
- DNS解析信息API增强
  - 在DNS解析信息中包含证书有效期信息
  - 通过DnsResolveUrlResp实体的acmeCerInfo字段传递证书有效期
- WebSocket 实时日志 API 增强
  - 支持订阅特定服务日志
  - 支持全量日志订阅
  - 增加心跳检测机制

### 问题修复
- 修复腾讯云DNS插件中客户端未正确初始化的问题
- 解决了异步任务中可能出现的空指针异常

## V0.0.1 (2025年11月)

### 新增功能
- 基础DNS动态更新服务 (DDNS)
- 支持多种DNS提供商（通过插件系统）
- 前端Web管理界面 (Vue 3 + Vuetify)
- JWT用户认证系统
- WebSocket实时日志推送
- ACME协议支持（SSL证书自动申请和管理）