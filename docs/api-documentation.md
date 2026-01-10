# HMoneta API接口文档

## API基础信息

- **API基础路径**: `/hm`
- **协议**: HTTP/HTTPS
- **认证方式**: JWT Token
- **内容类型**: application/json
- **项目版本**: V0.0.1-Beta2
- **构建系统**: Gradle 9.2.1

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
- **功能描述**: 查询所有DNS解析记录信息，包含证书有效期信息
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
        "lastUpdateTime": "2025-12-22T10:30:00",
        "acmeCerInfo": {
          "notBefore": "2025-12-22T00:00:00Z",
          "notAfter": "2026-03-22T23:59:59Z"
        }
      }
    ]
  }
]
```

**新增字段说明**:

- `acmeCerInfo`: 证书有效期信息对象
    - `notBefore`: 证书生效时间 (ISO 8601格式)
    - `notAfter`: 证书到期时间 (ISO 8601格式)

### 3. 插入DNS解析分组

- **接口路径**: `POST /hm/dns/insert_group`
- **功能描述**: 创建新的DNS解析分组
- **请求参数**:

```json
{
  "providerId": "DNS提供商ID",
  "groupName": "新分组名称",
  "authenticateWayMap": {
    "配置参数": "配置值"
  },
  "urls": [
    "域名1",
    "域名2"
  ]
}
```

- **参数说明**:
    - `providerId`: DNS提供商ID（字符串格式）
    - `groupName`: 分组名称
    - `authenticateWayMap`: 认证方式配置映射
    - `urls`: 域名列表
- **响应示例**: HTTP 200 OK

### 4. 修改DNS解析分组

- **接口路径**: `POST /hm/dns/modify_group`
- **功能描述**: 修改DNS解析分组信息，支持统一操作（修改和删除）
- **请求参数**:

```json
{
  "id": "分组ID",
  "groupName": "修改后的分组名称",
  "authenticateWayMap": {
    "配置参数": "配置值"
  },
  "isDelete": false
}
```

- **参数说明**:
    - `id`: 分组ID（字符串格式）
    - `groupName`: 分组名称
    - `authenticateWayMap`: 认证方式配置映射
    - `isDelete`: 是否删除分组（true=删除，false=修改）
- **响应示例**: `{"message": "success"}`

### 5. 修改DNS解析URL

- **接口路径**: `POST /hm/dns/url/modify`
- **功能描述**: 修改DNS解析URL记录
- **请求参数**:

```json
{
  "id": "URL记录ID",
  "url": "newdomain.com",
  "groupId": "分组ID"
}
```

- **响应示例**: HTTP 200 OK

### 6. 删除DNS解析URL

- **接口路径**: `POST /hm/dns/url/delete`
- **功能描述**: 删除DNS解析URL记录
- **请求参数**: URL ID (字符串格式)
- **请求体**: 直接传递字符串ID，不需要JSON格式
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

5. **证书有效期管理**
    - 自动存储和管理证书有效期信息（notBefore/notAfter）
    - 提供证书有效期API查询接口
    - 在DNS解析信息中展示证书有效期

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

| 错误码 | 说明      |
|-----|---------|
| 200 | 请求成功    |
| 400 | 请求参数错误  |
| 401 | 认证失败    |
| 403 | 权限不足    |
| 404 | 资源不存在   |
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

# 下载证书
curl -X GET "http://localhost:8080/hm/acme/download-cert/example.com" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -o example.com_certificate.zip

# 插入DNS分组
curl -X POST http://localhost:8080/hm/dns/insert_group \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{"providerId": "1", "groupName": "新分组", "authenticateWayMap": {}, "urls": ["domain1.com"]}'

# 修改DNS分组（统一接口）
curl -X POST http://localhost:8080/hm/dns/modify_group \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{"id": "分组ID", "groupName": "修改后的分组", "authenticateWayMap": {}, "isDelete": false}'

# 删除DNS分组（通过统一接口设置isDelete=true）
curl -X POST http://localhost:8080/hm/dns/modify_group \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{"id": "分组ID", "isDelete": true}'

# 修改DNS URL
curl -X POST http://localhost:8080/hm/dns/url/modify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{"id": "URL记录ID", "url": "newdomain.com", "groupId": "分组ID"}'

# 删除DNS URL
curl -X POST http://localhost:8080/hm/dns/url/delete \
  -H "Content-Type: text/plain" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d "URL记录ID"
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

// 下载证书
function downloadCertificate(domain) {
    fetch(`http://localhost:8080/hm/acme/download-cert/${domain}`, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token
        }
    })
        .then(response => response.blob())
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${domain}_certificate.zip`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
        });
}

// 查询DNS解析记录（包含证书有效期信息）
fetch('http://localhost:8080/hm/dns/resolve_info', {
    method: 'GET',
    headers: {
        'Authorization': 'Bearer ' + token
    }
})
    .then(response => response.json())
    .then(data => {
        console.log('DNS解析记录:', data);
        data.forEach(group => {
            group.dnsRecords.forEach(record => {
                if (record.acmeCerInfo) {
                    console.log(`域名 ${record.url} 证书有效期:`, record.acmeCerInfo);
                }
            });
        });
    });

// 修改DNS分组（统一操作接口）
fetch('http://localhost:8080/hm/dns/modify_group', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
        id: "分组ID",
        groupName: "修改后的分组名称",
        authenticateWayMap: {
            "配置参数": "配置值"
        },
        isDelete: false
    })
})
    .then(response => response.json())
    .then(result => console.log('分组修改结果:', result));

// 删除DNS分组（通过统一接口设置isDelete=true）
fetch('http://localhost:8080/hm/dns/modify_group', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
        id: "分组ID",
        isDelete: true
    })
})
    .then(response => response.json())
    .then(result => console.log('分组删除结果:', result));
```

## 新增功能说明

### V0.0.1-Beta2 (2026年1月10日)

#### 架构升级

- **Gradle现代化**: 全面采用Gradle 9.2.1构建系统，提升构建性能和现代化程度
- **构建优化**: 启用Gradle Configuration Cache，显著提升构建速度
- **前端配置升级**: 前端使用ES模块格式的Vite配置文件，提升构建性能

#### API接口优化

- **统一分组操作**: 优化DNS分组管理，将修改和删除操作合并到统一接口
- **简化设计**: 提高接口的一致性和易用性
- **参数优化**: 通过`isDelete`参数控制操作类型，移除独立的删除分组端点

### V0.0.1-Alpha (2025年12月11日)

#### 证书有效期管理

- 在DNS解析信息中自动包含证书有效期信息
- 通过`acmeCerInfo`字段提供证书生效时间和到期时间
- 支持证书有效期API查询和前端展示

#### 证书下载功能

- 新增证书下载API：`GET /hm/acme/download-cert/{domain}`
- 支持ZIP格式证书包下载，包含多种证书格式
- 包含.key、.crt、.pem、.fullchain.pem等完整证书链文件

#### 敏感字段过滤

- 通过@JwtExclude注解标记敏感字段
- JWT工具类支持敏感字段过滤机制
- 增强API响应安全性

#### MDC日志追踪

- 使用MDC（Mapped Diagnostic Context）实现日志ID追踪
- 生成LOG_ID用于日志追踪和调试
- 便于问题定位和系统监控

#### WebSocket增强

- 支持按服务订阅日志和全量订阅
- 实现ping/pong心跳机制
- 优化连接管理和错误处理

### V0.0.1-Alpha2 (2025年12月31日)

#### 前端界面优化

- **现代化登录界面**: 全新重写登录界面，采用粒子效果背景和玻璃拟态设计
- **悬浮按钮交互**: 使用悬浮按钮替换原本新增分组按钮，提升操作体验
- **UI夜晚模式优化**: 背景透明设计和夜晚模式优化，提升夜间使用体验
- **即时DNS解析**: 新增修改网址后直接触发DNS解析功能
- **认证状态检查**: 在前端main.js中增加认证状态检查

## API更新日志

### V0.0.1-Beta2 (2026年1月10日)

- **统一分组操作**: DNS分组管理接口优化，支持修改和删除的统一操作
- **构建系统升级**: 从Maven迁移到Gradle 9.2.1
- **Java版本升级**: 升级到Java 25 (LTS)
- **前端配置优化**: Vite配置现代化，使用ES模块格式

### V0.0.1-Alpha2 (2025年12月31日)

- **悬浮按钮**: DNS管理页面采用悬浮按钮交互模式
- **认证优化**: 前端增加认证状态检查机制

### V0.0.1-Alpha (2025年12月11日)

- **证书API**: 新增证书下载和有效期管理API
- **WebSocket增强**: 支持实时日志推送和任务跟踪
- **安全增强**: JWT敏感字段过滤机制

---

**当前版本**: V0.0.1-Beta2 (2026年1月10日)
**技术栈**: Spring Boot 4.0.1 + Java 25 + Vue 3 + Gradle 9.2.1