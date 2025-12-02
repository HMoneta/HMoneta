# HMoneta API接口文档

## API基础信息

- **API基础路径**: `/hm`
- **协议**: HTTP/HTTPS
- **认证方式**: JWT Token
- **内容类型**: application/json

## DNS 管理 API

### 1. 获取所有DNS提供商

- **接口路径**: `GET /hm/dns/query_all`
- **功能描述**: 获取所有可用的DNS提供商信息
- **请求参数**: 无
- **响应示例**:
```json
[
  {
    "id": 1,
    "providerName": "Cloudflare",
    "providerDesc": "Cloudflare DNS服务",
    "createTime": "2025-11-22T10:30:00",
    "updateTime": "2025-11-22T10:30:00"
  }
]
```

### 2. 查询所有DNS解析记录

- **接口路径**: `GET /hm/dns/resolve_info`
- **功能描述**: 查询所有DNS解析记录信息
- **请求参数**: 无
- **响应示例**:
```json
[
  {
    "id": 1,
    "groupName": "主域名组",
    "providerName": "Cloudflare",
    "dnsRecords": [
      {
        "id": 1,
        "url": "example.com",
        "currentIp": "192.168.1.1",
        "lastUpdateTime": "2025-11-22T10:30:00"
      }
    ]
  }
]
```

### 3. 插入DNS解析分组

- **接口路径**: `POST /hm/dns/insert_group`
- **功能描述**: 创建新的DNS解析分组
- **请求参数**:
```json
{
  "groupName": "新分组名称",
  "providerId": 1,
  "description": "分组描述"
}
```
- **响应示例**: HTTP 200 OK

### 4. 修改DNS解析分组

- **接口路径**: `POST /hm/dns/modify_group`
- **功能描述**: 修改DNS解析分组信息
- **请求参数**:
```json
{
  "groupId": 1,
  "groupName": "修改后的分组名称",
  "providerId": 1,
  "description": "修改后的分组描述"
}
```
- **响应示例**: `{"message": "success"}`

### 5. 修改DNS解析URL

- **接口路径**: `POST /hm/dns/url/modify`
- **功能描述**: 修改DNS解析URL记录
- **请求参数**:
```json
{
  "id": 1,
  "url": "newdomain.com",
  "groupId": 1
}
```
- **响应示例**: HTTP 200 OK

### 6. 删除DNS解析URL

- **接口路径**: `POST /hm/dns/url/delete`
- **功能描述**: 删除DNS解析URL记录
- **请求参数**: URL ID (String)
- **响应示例**: HTTP 200 OK

## 用户管理 API

### 1. 用户登录

- **接口路径**: `POST /hm/user/login`
- **功能描述**: 用户登录验证
- **请求参数**:
```json
{
  "username": "admin",
  "password": "password"
}
```
- **响应示例**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. 验证用户Token有效性

- **接口路径**: `GET /hm/user/valid`
- **功能描述**: 验证JWT Token是否有效
- **请求参数**: `HMToken` (query参数)
- **响应示例**: `true` 或 `false`

## 插件管理 API

### 1. 上传插件文件

- **接口路径**: `POST /hm/plugin/upload`
- **功能描述**: 上传新的DNS提供商插件
- **请求参数**: 
  - multipart/form-data 格式
  - 文件字段名: `plugin`
- **响应示例**:
```json
{
  "message": "ZIP文件上传成功: HMoneta-Official-Dns-Plugin-Cloudflare_123456789.zip"
}
```

## ACME 证书管理 API

### 1. 修改 ACME 用户信息

- **接口路径**: `POST /hm/acme/modify`
- **功能描述**: 修改 ACME 用户信息
- **请求参数**:
```json
{
  "userEmail": "user@example.com"
}
```
- **响应示例**: HTTP 200 OK

### 2. 申请 SSL 证书

- **接口路径**: `GET /hm/acme/apply`
- **功能描述**: 申请 SSL 证书
- **请求参数**: 
  - `domain` (query参数): 域名
- **响应示例**: 任务ID字符串

### 3. 下载 SSL 证书

- **接口路径**: `GET /hm/acme/download-cert/{domain}`
- **功能描述**: 下载指定域名的证书包
- **请求参数**: 
  - `domain` (路径参数): 域名
- **响应示例**: ZIP格式的证书包文件，包含.key、.crt、.pem、.fullchain.pem等格式文件
- **响应头**: 
  - `Content-Type`: application/octet-stream
  - `Content-Disposition`: attachment; filename="{domain}_certificate.zip"

### 3. 下载 SSL 证书

- **接口路径**: `GET /hm/acme/download-cert/{domain}`
- **功能描述**: 下载指定域名的证书包
- **请求参数**: 
  - `domain` (路径参数): 域名
- **响应示例**: ZIP格式的证书包文件，包含.key、.crt、.pem、.fullchain.pem等格式文件
- **响应头**: 
  - `Content-Type`: application/octet-stream
  - `Content-Disposition`: attachment; filename="{domain}_certificate.zip"

## WebSocket 实时日志 API

### 1. 日志推送连接

- **连接路径**: `ws://服务器地址/ws/logs`
- **功能描述**: 通过WebSocket连接获取实时日志信息
- **消息格式**: 文本消息，包含日志级别和内容
- **示例消息**: 
```json
{
  "timestamp": "2025-11-22T10:30:00",
  "level": "INFO",
  "message": "DNS更新任务执行成功"
}
```

## ACME 证书管理服务

### ACME证书管理功能

ACME证书管理功能通过`AcmeService`实现，主要功能包括：

1. **通过DNS-01挑战验证申请SSL证书**
   - 自动创建_acme-challenge记录
   - 验证DNS记录生效
   - 完成证书申请流程
   - 证书异步申请处理

2. **证书自动续期**
   - 定期检查证书有效期
   - 自动续期即将过期的证书

3. **证书打包下载功能**
   - 生成.crt、.key、.pem、.fullchain.pem等格式证书
   - 支持ZIP格式打包下载
   - 提供下载API端点：`GET /hm/acme/download-cert/{domain}`

4. **ACME挑战信息管理**
   - 管理证书申请过程信息
   - 记录申请状态和日志
   - 通过AcmeAsyncLogEntity实体跟踪异步任务日志

## 认证与授权

### JWT Token 使用

- 所有需要认证的API接口需要在请求头中包含 `Authorization` 字段
- 格式: `Bearer {JWT_TOKEN}`
- Token有效期: 24小时 (可配置)

### 未授权响应

当请求未通过认证时，API返回:
```json
{
  "error": "Unauthorized",
  "message": "Token无效或已过期"
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 认证失败 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 请求示例

### cURL 示例

```bash
# 获取DNS提供商列表
curl -X GET http://localhost:8080/hm/dns/query_all \
  -H "Authorization: Bearer {JWT_TOKEN}"

# 用户登录
curl -X POST http://localhost:8080/hm/user/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# 上传插件
curl -X POST http://localhost:8080/hm/plugin/upload \
  -F "plugin=@plugin.zip"

# 申请SSL证书
curl -X GET "http://localhost:8080/hm/acme/apply?domain=example.com" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### JavaScript/Fetch 示例

```javascript
// 获取DNS提供商列表
fetch('http://localhost:8080/hm/dns/query_all', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + token
  }
})
.then(response => response.json())
.then(data => console.log(data));

// 用户登录
fetch('http://localhost:8080/hm/user/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin',
    password: 'password'
  })
})
.then(response => response.text())
.then(token => console.log(token));

// 申请SSL证书
fetch('http://localhost:8080/hm/acme/apply?domain=example.com', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + token
  }
})
.then(response => response.text())
.then(taskId => console.log('证书申请任务ID:', taskId));
```