# SafeLine WAF API Documentation

> SafeLine WAF 社区版 API 文档，基于 Swagger 2.0 规范。
>
> **Base URL**: `https://<host>:<port>/api`
>
> **认证方式**:
> - Header: `X-SLCE-API-TOKEN: <token>`
> - 登录获取 JWT: `POST /open/auth/login`，后续请求携带 `Authorization: Bearer <jwt>`

---

## Table of Contents

1. [认证/用户](#1-认证用户)
2. [Web 服务/站点](#2-web-服务站点)
3. [SSL 证书](#3-ssl-证书)
4. [攻击日志/事件](#4-攻击日志事件)
5. [统计](#5-统计)
6. [安全态势](#6-安全态势)
7. [策略/规则](#7-策略规则)
8. [认证防御](#8-认证防御)
9. [IP 组](#9-ip-组)
10. [站点分组](#10-站点分组)
11. [转发规则](#11-转发规则)
12. [静态文件](#12-静态文件)
13. [防篡改](#13-防篡改)
14. [系统](#14-系统)
15. [告警](#15-告警)
16. [Syslog](#16-syslog)
17. [阻断页面](#17-阻断页面)
18. [Portal/验证码](#18-portal验证码)
19. [MCP](#19-mcp)
20. [降级](#20-降级)
21. [限速放行](#21-限速放行)
22. [检测器](#22-检测器)
23. [威胁情报](#23-威胁情报)
24. [JA4 指纹](#24-ja4-指纹)
25. [上报行为](#25-上报行为)
26. [SafePoint](#26-safepoint)

---

## 1. 认证/用户

### 1.1 获取 CSRF Token
```
GET /open/auth/csrf
```

Response:
```json
{
  "data": {
    "csrf_token": "string"
  }
}
```

### 1.2 用户登录
```
POST /open/auth/login
Content-Type: application/json

{
  "username": "string",          // 必填
  "password": "string",          // 必填
  "csrf_token": "string",        // 可选
  "callback_address": "string",  // 可选
  "test": boolean                // 可选
}
```

Response:
```json
{
  "data": {
    "jwt": "string",
    "id": 1,
    "tfa_enabled": true,
    "tfa_binded": false,
    "tfa_bind_url": "string",
    "redirect": "string"
  }
}
```

### 1.3 TFA 两步验证登录
```
POST /open/auth/tfa
Content-Type: application/json

{
  "code": "string",          // 必填，TFA 验证码
  "csrf_token": "string",
  "timestamp": 1234567890
}
```

### 1.4 登出
```
POST /open/auth/logout
```

### 1.5 获取 API Token
```
GET /open/auth/token
```

Response:
```json
{ "data": "string" }
```

### 1.6 更新 API Token
```
PUT /open/auth/token
```

### 1.7 删除 API Token
```
DELETE /open/auth/token
```

### 1.8 用户列表
```
GET /open/users
```

Response:
```json
{
  "data": [{
    "id": 1,
    "username": "string",
    "role": 1,               // 0=Unknown, 1=Admin, 2=Manager, 3=Config, 4=Audit
    "password_enabled": true,
    "tfa_enabled": true,
    "tfa_binded": true,
    "last_login": 1234567890,
    "lock_until": 1234567890,
    "pwd_updated_at": 1234567890
  }]
}
```

### 1.9 创建用户
```
POST /open/users
Content-Type: application/json

{
  "username": "string",       // 必填
  "password": "string",       // 必填
  "role": 1,                  // 必填，1=Admin, 2=Manager, 3=Config, 4=Audit
  "tfa_enabled": boolean
}
```

Response:
```json
{ "data": { "id": 1 } }
```

### 1.10 更新用户
```
PUT /open/users
Content-Type: application/json

{
  "id": 1,
  "username": "string",
  "password": "string",
  "role": 1,
  "tfa_enabled": boolean
}
```

### 1.11 删除用户
```
DELETE /open/users
Content-Type: application/json

{ "id": 1 }
```

### 1.12 重置用户 TOTP
```
POST /open/users/{id}/totp
```

### 1.13 解锁用户
```
POST /business/users/{id}/unlock
```

---

## 2. Web 服务/站点

### 2.1 站点列表
```
GET /open/site
Query: group_id, site, sp_enabled, stat_enabled
```

Response:
```json
{
  "data": [{
    "id": 1,
    "server_names": ["example.com"],
    "ports": ["80", "443"],
    "upstreams": ["127.0.0.1:8080"],
    "cert_id": 1,
    "mode": 0,               // 0=defense, 1=offline, 2=dry run
    "is_enabled": true,
    "group_id": 1,
    "title": "string",
    "comment": "string",
    "type": 0,
    "position": 1,
    "stat_enabled": true,
    "sp_enabled": true,
    "health_check": true,
    "health_state": {},
    "created_at": "string",
    "updated_at": "string"
  }],
  "total": 10
}
```

### 2.2 创建站点
```
POST /open/site
Content-Type: application/json

{
  "server_names": ["string"],     // 必填，域名列表
  "ports": ["string"],            // 必填，端口列表
  "upstreams": ["string"],        // 必填，上游服务器
  "cert_id": 1,
  "type": 0,
  "group_id": 1,
  "comment": "string",
  "email": "string",
  "health_check": boolean,
  "load_balance": { "balance_type": 0 },
  "redirect_status_code": 301,
  "stat_enabled": boolean,
  "static_default": 0
}
```

Response:
```json
{ "data": 1 }  // 返回站点 ID
```

### 2.3 更新站点
```
PUT /open/site
Content-Type: application/json
```

### 2.4 删除站点
```
DELETE /open/site
Content-Type: application/json

{ "ids": [1, 2, 3] }
```

### 2.5 站点详情
```
GET /open/site/{id}
```

Response:
```json
{
  "data": {
    "id": 1,
    "server_names": ["example.com"],
    "ports": ["80", "443"],
    "upstreams": ["127.0.0.1:8080"],
    "cert_id": 1,
    "cert_type": 1,           // 0=无证书, 1=上传, 2=自签名, 3=管理证书
    "mode": 0,
    "is_enabled": true,
    "group_id": 1,
    "title": "string",
    "comment": "string",
    "icon": "string",
    "index": "string",
    "type": 0,
    "position": 1,
    "acl_enabled": true,
    "auth_defense_id": 1,
    "challenge_id": 1,
    "chaos_id": 1,
    "chaos_is_enabled": true,
    "wr_id": 1,
    "semantics": true,
    "portal": true,
    "portal_redirect": "string",
    "cc_bot": true,
    "stat_enabled": true,
    "sp_enabled": true,
    "static": true,
    "static_default": 0,
    "health_check": true,
    "health_state": {},
    "load_balance": { "balance_type": 0 },
    "forwarding_rules": [],
    "exclude_paths": [],
    "exclude_content_type": [],
    "custom_location": [],
    "created_at": "string",
    "updated_at": "string"
  }
}
```

### 2.6 更新站点基本信息
```
PUT /open/site/{id}/basic_info
Content-Type: application/json

{
  "group_id": 1,
  "comment": "string",
  "icon": "string"
}
```

### 2.7 更新站点运行模式
```
PUT /open/site/{id}/mode
Content-Type: application/json

{
  "ids": [1],
  "mode": 0  // 0=defense, 1=offline, 2=dry run
}
```

### 2.8 更新站点分组
```
PUT /open/site/{id}/group
Content-Type: application/json

{ "group_id": 1 }
```

### 2.9 站点排序
```
PUT /open/site/{id}/sort
Content-Type: application/json

{ "position": 1 }
```

### 2.10 站点路由列表
```
GET /open/site/{id}/resources
```

Response:
```json
{
  "data": {
    "nodes": [{
      "id": 1,
      "site_id": 1,
      "path": "/api",
      "method": "GET",
      "content_type": "application/json",
      "status_code": 200,
      "response_time": 100,
      "req_today": 1000,
      "content_length": 1024,
      "req_header": "string",
      "created_at": "string",
      "updated_at": "string"
    }],
    "total": 10
  }
}
```

### 2.11 删除站点路由
```
DELETE /open/site/{id}/resources
Content-Type: application/json

{ "ids": [1, 2] }
```

### 2.12 获取站点安全配置
```
GET /open/site/{id}/proxy
```

Response:
```json
{
  "data": {
    "id": 1,
    "site_id": 1,
    "host": { "global": false, "value": "string" },
    "force_https": { "global": false, "value": true },
    "http2": { "global": false, "value": true },
    "http3": { "global": false, "value": false },
    "http_1.0": { "global": false, "value": false },
    "hsts": { "global": false, "value": true },
    "hsts_max_age": { "global": false, "value": "string" },
    "hsts_sub": { "global": false, "value": true },
    "hsts_preload": { "global": false, "value": false },
    "gzip": { "global": false, "value": true },
    "br": { "global": false, "value": true },
    "ipv6": { "global": false, "value": false },
    "ssl_protocols": { "global": false, "value": "string" },
    "ssl_ciphers": { "global": false, "value": "string" },
    "xfp": { "global": false, "value": "string" },
    "xfh": { "global": false, "value": "string" },
    "ip_source": { "global": false, "value": "string" },
    "ip_value": { "global": false, "value": "string" },
    "reset_xff": { "global": false, "value": false },
    "default_server": { "global": false, "value": false },
    "ntlm": { "global": false, "value": false },
    "sse": { "global": false, "value": false },
    "http_headers": { "global": false, "value": [] },
    "global": {},
    "created_at": "string",
    "updated_at": "string"
  }
}
```

### 2.13 设置站点安全配置
```
PUT /open/site/{id}/proxy
Content-Type: application/json

{
  "force_https": { "value": true },
  "http2": { "value": true },
  "ssl_protocols": { "value": "string" }
}
```

### 2.14 获取 Tengine 配置
```
GET /open/site/{id}/nginx_config
```

### 2.15 更新 Tengine 配置
```
PUT /open/site/{id}/nginx_config
Content-Type: application/json

{ "custom_location": [] }
```

### 2.16 站点访问/错误日志
```
GET /open/site/{id}/log
Query: type (access/error)
```

### 2.17 下载日志
```
GET /open/site/{id}/log/download?filename=xxx
```

### 2.18 日志限流
```
GET /open/site/{id}/log/limit?type=access
POST /open/site/{id}/log/limit?type=access&limit=1000
```

### 2.19 健康检查
```
POST /open/site/{id}/healthcheck
Content-Type: application/json

{
  "hosts": ["string"],
  "upstreams": ["string"]
}
```

### 2.20 获取限速配置
```
GET /open/site/{id}/acl
```

### 2.21 设置限速配置
```
PUT /open/site/{id}/acl
Content-Type: application/json

{
  "rules": [],
  "use_global": false
}
```

### 2.22 添加限速规则
```
POST /open/site/{id}/acl
Content-Type: application/json

{
  "name": "string",
  "type": "req",         // req/attack/error
  "action": "ban",       // ban/challenge
  "period": 60,
  "count": 100,
  "block_min": 10,
  "enabled": true,
  "conditions": [{
    "condition": {
      "field": "string",
      "operator": "eq",  // eq/not_eq/has/not_has/prefix/re
      "value": ["string"]
    },
    "strategy": "string"
  }]
}
```

### 2.23 更新限速规则
```
PUT /open/site/{id}/acl/{rule_id}
```

### 2.24 删除限速规则
```
DELETE /open/site/{id}/acl/{rule_id}
```

### 2.25 获取站点动态安全
```
GET /open/site/{id}/chaos
```

### 2.26 创建站点动态安全
```
POST /open/site/{id}/chaos
Content-Type: application/json

{
  "is_enabled": true,
  "html_encryption": false,
  "html_fast_decryption": false,
  "js_encryption": false,
  "img_encryption": false,
  "img_watermark": "string",
  "img_text": "string",
  "js_path": []
}
```

### 2.27 获取站点语义引擎
```
GET /open/site/{id}/semantics
```

### 2.28 设置站点语义引擎
```
PUT /open/site/{id}/semantics
Content-Type: application/json

{
  "semantics": { "key": "value" },
  "use_global": false
}
```

### 2.29 等候室配置
```
GET /open/site/{id}/waiting
POST /open/site/{id}/waiting
Content-Type: application/json

{
  "is_enabled": true,
  "max_concurrent": 100,
  "max_waiting": 50,
  "session_timeout": 300
}
```

### 2.30 更新站点认证防御
```
PUT /open/site/{id}/defense
Content-Type: application/json

{
  "id": 1,
  "enable": true,
  "negate": false,
  "pattern": [[{}]],
  "auth_source_ids": [],
  "auth_callback": "string",
  "tfa_enabled": true,
  "review": 0
}
```

### 2.31 更新站点反爬配置
```
PUT /open/site/{id}/challenge
Content-Type: application/json

{
  "id": 1,
  "enable": true,
  "level": 1,
  "negate": false,
  "pattern": [[{}]],
  "replay": false,
  "expire": 300
}
```

### 2.32 路由排除配置
```
GET /open/site/{id}/excludes
POST /open/site/{id}/excludes
Content-Type: application/json

{
  "prefixes": [],
  "content_types": []
}
```

---

## 3. SSL 证书

### 3.1 证书列表
```
GET /open/cert
```

Response:
```json
{
  "data": {
    "nodes": [{
      "id": 1,
      "type": 1,
      "domains": ["example.com"],
      "issuer": "Let's Encrypt",
      "valid_before": "2025-12-31",
      "expired": false,
      "revoked": false,
      "self_signature": false,
      "trusted": true,
      "related_sites": ["example.com"],
      "acme_message": "string"
    }],
    "total": 5
  }
}
```

### 3.2 创建/更新证书
```
POST /open/cert
Content-Type: application/json

// 手动上传
{
  "type": 1,
  "manual": {
    "crt": "-----BEGIN CERTIFICATE-----\n...",
    "key": "-----BEGIN PRIVATE KEY-----\n..."
  }
}

// ACME 自动申请
{
  "type": 0,
  "acme": {
    "domains": ["example.com"],
    "email": "admin@example.com"
  }
}
```

### 3.3 证书详情
```
GET /open/cert/{id}
```

### 3.4 删除证书
```
DELETE /open/cert/{id}
```

---

## 4. 攻击日志/事件

### 4.1 攻击日志列表
```
GET /open/records
Query: page, page_size, start, end, ip, host, port, url, attack_type, action, event_id, ja4_fingerprint
```

Response:
```json
{
  "data": [{
    "id": 1,
    "event_id": "string",
    "site_uuid": "string",
    "website": "example.com",
    "host": "example.com",
    "src_ip": "1.2.3.4",
    "src_port": 12345,
    "dst_ip": "10.0.0.1",
    "dst_port": 80,
    "protocol": 1,
    "method": "GET",
    "url_path": "/api/test",
    "query_string": "a=1&b=2",
    "action": 1,           // 0=放行, 1=阻断
    "attack_type": 1,
    "risk_level": 3,
    "rule_id": "1001",
    "short_rule_id": "001",
    "rule_id_list": ["1001", "1002"],
    "policy_name": "SQL注入",
    "reason": "string",
    "module": "string",
    "payload": "string",
    "decode_path": "string",
    "req_header": "string",
    "req_body": "string",
    "rsp_header": "string",
    "rsp_body": "string",
    "status_code": 403,
    "timestamp": 1234567890,
    "created_at": "string",
    "updated_at": "string",
    "country": "China",
    "province": "Beijing",
    "city": "Beijing",
    "location": "string",
    "lat": "39.9042",
    "lng": "116.4074",
    "ja4_fingerprint": "string",
    "socket_ip": "string"
  }],
  "total": 100
}
```

### 4.2 攻击日志详情
```
GET /open/record/:id
```

### 4.3 限速日志
```
GET /open/records/acl
Query: page, page_size
```

### 4.4 认证防御日志
```
GET /open/records/auth_defense
Query: page, page_size, ip, site, started_at_begin, started_at_end
```

### 4.5 认证防御日志 V2
```
GET /open/v2/records/auth_defense
Query: page, page_size, begin, end, ip, site, status, username
```

### 4.6 反爬日志
```
GET /open/records/challenge
Query: page, page_size, ip, site, started_at_begin, started_at_end
```

### 4.7 黑白名单规则日志
```
GET /open/records/rule
Query: page, page_size, start, end, ip, host, port, url, attack_type, action, event_id, ja4_fingerprint
```

### 4.8 等候室日志
```
GET /open/records/waiting
Query: page, page_size, site, started_at_begin, started_at_end
```

### 4.9 攻击事件列表
```
GET /open/events
Query: page, page_size, start, end, ip, host, port
```

### 4.10 黑白名单攻击事件
```
GET /open/events/rule
Query: page, page_size, start, end, ip, host, port
```

### 4.11 导出攻击日志
```
GET /commercial/record/export
Query: start, end, ip, host, port, url, attack_type, action, event_id, ja4_fingerprint, type
Content-Type: application/octet-stream
```

---

## 5. 统计

### 5.1 QPS 统计
```
GET /stat/qps
```

### 5.2 高级访问统计
```
GET /stat/advance/access
Query: begin_time, end_time, site_id
```

### 5.3 高级攻击拦截统计
```
GET /stat/advance/attack
Query: begin_time, end_time, site_id
```

### 5.4 高级客户端统计
```
GET /stat/advance/client
Query: begin_time, end_time, site_id, size
```

### 5.5 高级域名统计
```
GET /stat/advance/domain
Query: begin_time, end_time, site_id, size, refer
```

### 5.6 高级错误码统计
```
GET /stat/advance/error_status_code
Query: begin_time, end_time, site_id, upstream
```

### 5.7 高级地理位置统计
```
GET /stat/advance/location
Query: begin_time, end_time, site_id, size, action, global
```

### 5.8 高级页面统计
```
GET /stat/advance/page
Query: begin_time, end_time, site_id, size, refer
```

### 5.9 高级状态码统计
```
GET /stat/advance/status_code
Query: begin_time, end_time, site_id, size, upstream
```

### 5.10 访问趋势
```
GET /stat/advance/trend/access
Query: begin_time, end_time, site_id
```

### 5.11 拦截趋势
```
GET /stat/advance/trend/intercept
Query: begin_time, end_time, site_id
```

---

## 6. 安全态势

### 6.1 实时安全态势
```
GET /open/security_posture/realtime
Query: site_id, top
```

### 6.2 安全态势统计
```
GET /open/security_posture/statistics
Query: begin_time, end_time, site_id
```

### 6.3 安全态势趋势
```
GET /open/security_posture/trends
Query: begin_time, end_time, site_id, top, type
// type: attack/black_white/acl/challenge/waiting/auth/attack_type/black_white_rule/attack_host/attack_page
```

### 6.4 设置站点安全态势
```
POST /open/security_posture/site
Content-Type: application/json

{ "site_id": 1, "enabled": true }
```

---

## 7. 策略/规则

### 7.1 自定义规则列表
```
GET /open/policy
Query: page, page_size, action  // action: -1=全部, 0=放行, 1=阻断, 2=挑战, 3=认证防御
```

### 7.2 创建规则
```
POST /open/policy
Content-Type: application/json

{
  "name": "string",
  "action": 1,
  "is_enabled": true,
  "level": 1,
  "pattern": [[{
    "k": "string",
    "op": "string",
    "sub_k": "string",
    "v": ["string"]
  }]],
  "log": true,
  "expire": 3600
}
```

### 7.3 更新规则
```
PUT /open/policy
Content-Type: application/json

{
  "id": 1,
  "name": "string",
  "action": 1,
  "is_enabled": true,
  "level": 1,
  "pattern": [],
  "log": true,
  "expire": 3600
}
```

### 7.4 删除规则
```
DELETE /open/policy
Content-Type: application/json

{ "id": 1 }
```

### 7.5 规则详情
```
GET /open/policy/detail?id=1
```

### 7.6 启用/禁用规则
```
PUT /open/policy/switch
Content-Type: application/json

{ "id": 1, "is_enabled": true }
```

### 7.7 增强规则
```
GET /open/skynet/rule
PUT /open/skynet/rule
Content-Type: application/json

{
  "id": ["string"],
  "mode": "default",  // strict/default/dry_run/disable/deny
  "global": false
}
```

### 7.8 增强规则全局开关
```
GET /open/skynet/rule/switch
PUT /open/skynet/rule/switch
Content-Type: application/json

{ "enable": true }
```

---

## 8. 认证防御

### 8.1 认证源列表
```
GET /open/auth_defense/source
```

### 8.2 创建认证源
```
POST /open/auth_defense/source
Content-Type: application/json

{
  "title": "string",
  "type": 0,
  "association": 0,
  "auth": {
    "ldap": {
      "url": "string",
      "bind_dn": "string",
      "bind_pass": "string",
      "base_dn": "string",
      "filter": "string"
    },
    "cas": { "url": "string" },
    "oauth2": {
      "provider": "string",
      "app_id": "string",
      "app_secret": "string",
      "scope": "string",
      "extra": {},
      "mapping": {}
    }
  },
  "network_proxy": false
}
```

### 8.3 认证源详情
```
GET /open/auth_defense/source/{id}
```

### 8.4 更新认证源
```
PUT /open/auth_defense/source/{id}
```

### 8.5 删除认证源
```
DELETE /open/auth_defense/source/{id}
```

### 8.6 密码认证配置
```
GET /open/auth_defense/source/password
PUT /open/auth_defense/source/password
Content-Type: application/json

{ "title": "string" }
```

### 8.7 认证源用户列表
```
GET /open/auth_defense/source/{id}/user
Query: page, page_size, uname
```

### 8.8 更新认证源用户
```
PUT /open/auth_defense/source/{id}/user/{user_id}
Content-Type: application/json

{ "status": 1 }
```

### 8.9 删除认证源用户
```
DELETE /open/auth_defense/source/{id}/user/{user_id}
```

### 8.10 认证防御用户列表
```
GET /open/auth_defense/user
Query: page, page_size, username
```

### 8.11 创建认证防御用户
```
POST /open/auth_defense/user
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "email": "string",
  "phone": "string",
  "tfa_enabled": false
}
```

### 8.12 批量删除认证防御用户
```
DELETE /open/auth_defense/user
Content-Type: application/json

{ "ids": [1, 2] }
```

### 8.13 认证防御用户详情
```
GET /open/auth_defense/user/{user_id}
```

### 8.14 更新认证防御用户
```
PUT /open/auth_defense/user/{user_id}
Content-Type: application/json

{
  "username": "string",
  "password": "string",
  "email": "string",
  "phone": "string",
  "tfa_enabled": false,
  "websites": [{ "site_id": 1, "status": 1 }]
}
```

### 8.15 删除认证防御用户
```
DELETE /open/auth_defense/user/{user_id}
```

### 8.16 重置用户 TOTP
```
PUT /open/auth_defense/user/{user_id}/reset_totp
```

### 8.17 解绑用户
```
PUT /open/auth_defense/user/{user_id}/unbind
Content-Type: application/json

{ "type": 0 }
```

### 8.18 用户审核列表
```
GET /open/auth_defense/user/review
Query: page, page_size, only_review
```

### 8.19 审核用户
```
PUT /open/auth_defense/user/review/{review_id}
Content-Type: application/json

{ "status": 1 }
```

### 8.20 合并用户到主用户
```
POST /open/auth_defense/user/merge
Content-Type: application/json

{
  "main": 1,
  "merges": [2, 3],
  "dry_run": false
}
```

---

## 9. IP 组

### 9.1 IP 组列表
```
GET /open/ipgroup
Query: top
```

### 9.2 创建 IP 组
```
POST /open/ipgroup
Content-Type: application/json

{
  "comment": "string",
  "ips": ["1.2.3.4", "10.0.0.0/24"]
}
```

### 9.3 更新 IP 组
```
PUT /open/ipgroup
Content-Type: application/json

{
  "id": 1,
  "comment": "string",
  "ips": []
}
```

### 9.4 删除 IP 组
```
DELETE /open/ipgroup
Content-Type: application/json

{ "ids": [1, 2] }
```

### 9.5 IP 组详情
```
GET /open/ipgroup/detail?id=1
```

### 9.6 向 IP 组追加 IP
```
POST /open/ipgroup/append
Content-Type: application/json

{
  "ip_group_ids": [1],
  "ips": ["1.2.3.4"]
}
```

### 9.7 搜索引擎爬虫组
```
GET /open/ipgroup/crawler
POST /open/ipgroup/crawler
```

### 9.8 通过链接获取 IP
```
GET /open/ipgroup/link?href=https://example.com/ips.txt
```

### 9.9 通过链接创建 IP 组
```
POST /open/ipgroup/link?url=https://example.com/ips.txt&comment=string
```

---

## 10. 站点分组

### 10.1 分组列表
```
GET /open/site/group
Query: site
```

### 10.2 创建分组
```
POST /open/site/group
Content-Type: application/json

{ "name": "string" }
```

### 10.3 更新分组
```
PUT /open/site/group/{id}
Content-Type: application/json

{ "name": "string" }
```

### 10.4 删除分组
```
DELETE /open/site/group/{id}
```

### 10.5 分组排序
```
PUT /open/site/group/{id}/sort
Content-Type: application/json

{ "position": 1 }
```

### 10.6 分组开关状态
```
GET /open/site/group/switch
PUT /commercial/site/group/switch
Content-Type: application/json

{ "enable": true }
```

---

## 11. 转发规则

### 11.1 转发规则列表
```
GET /open/site/{id}/forwarding_rules
```

### 11.2 创建转发规则
```
POST /open/site/{id}/forwarding_rules
Content-Type: application/json

{
  "path_prefix": "/api",
  "upstreams": ["10.0.0.1:8080"],
  "comment": "string"
}
```

### 11.3 更新转发规则
```
PUT /open/site/{id}/forwarding_rules/{rule_id}
Content-Type: application/json

{
  "path_prefix": "/api",
  "upstreams": ["10.0.0.1:8080"],
  "comment": "string"
}
```

### 11.4 删除转发规则
```
DELETE /open/site/{id}/forwarding_rules/{rule_id}
```

### 11.5 启用/禁用转发规则
```
PUT /open/site/{id}/forwarding_rules/{rule_id}/switch
Content-Type: application/json

{ "is_enabled": true }
```

---

## 12. 静态文件

### 12.1 静态文件列表
```
GET /open/site/{id}/static?path=/
```

### 12.2 创建静态文件/目录
```
POST /open/site/{id}/static
Content-Type: application/json

{
  "path": "/index.html",
  "page": "string",
  "dir": false,
  "zip": false
}
```

### 12.3 重命名
```
PUT /open/site/{id}/static
Content-Type: application/json

{
  "old_path": "/old.html",
  "new_path": "/new.html",
  "copy": false
}
```

### 12.4 删除
```
DELETE /open/site/{id}/static
Content-Type: application/json

{ "path": "/index.html" }
```

---

## 13. 防篡改

### 13.1 站点防篡改列表
```
GET /business/site/{site_id}/anti_tamper
Query: changed
```

### 13.2 创建防篡改
```
POST /business/site/{site_id}/anti_tamper
Content-Type: application/json

{ "resource_ids": [1, 2] }
```

### 13.3 刷新防篡改
```
PUT /business/site/{site_id}/anti_tamper
```

### 13.4 防篡改详情
```
GET /business/anti_tamper/{id}
```

### 13.5 更新防篡改
```
PUT /business/anti_tamper/{id}
```

### 13.6 删除防篡改
```
DELETE /business/anti_tamper/{id}
```

### 13.7 防篡改页面
```
GET /business/anti_tamper/{id}/page?content=1
```

### 13.8 防篡改资源
```
GET /business/site/{site_id}/anti_tamper/resource/{resource_id}
```

---

## 14. 系统

### 14.1 系统信息
```
GET /open/system
```

### 14.2 更新管理证书
```
PUT /open/system
Content-Type: application/json

{ "cert_id": 1 }
```

### 14.3 系统架构
```
GET /open/system/arch
```

### 14.4 系统版本
```
GET /open/system/edition
```

### 14.5 系统密钥
```
GET /open/system/key
```

### 14.6 登录方式配置
```
GET /open/system/login_method
PUT /open/system/login_method
Content-Type: application/json

{
  "type": 1,
  "cas": {
    "url": "string",
    "version": 1,
    "role_key": "string",
    "mapping": {}
  },
  "oauth": {
    "url": "string",
    "client_id": "string",
    "client_secret": "string",
    "scope": "string",
    "role_key": "string",
    "mapping": {}
  }
}
```

### 14.7 登录类型
```
GET /open/system/login_type
```

### 14.8 网络代理
```
GET /open/system/network_proxy
PUT /open/system/network_proxy
Content-Type: application/json

{
  "url": "string",
  "username": "string",
  "password": "string"
}
```

### 14.9 协议状态
```
GET /open/system/protocol
POST /open/system/protocol
```

### 14.10 License 信息
```
GET /open/system/authorize
POST /open/system/authorize
Content-Type: application/json

{ "code": "string" }
PUT /open/system/authorize
DELETE /open/system/authorize
```

### 14.11 客服配置
```
GET /open/system/customer_service
PUT /open/system/customer_service
Content-Type: application/json

{
  "type": 1,
  "pages": []
}
```

### 14.12 全局代理设置
```
GET /open/global/proxy
PUT /open/global/proxy
Content-Type: application/json

{
  "host": { "value": "string" },
  "force_https": { "value": true },
  "http2": { "value": true },
  "ipv6": { "value": false }
}
```

### 14.13 全局语义配置
```
GET /open/global/mode
PUT /open/global/mode
Content-Type: application/json

{
  "semantics": { "key": "value" }
}
```

### 14.14 日志清理配置
```
GET /open/global/log_clean
POST /open/global/log_clean
Content-Type: application/json

{
  "max_day": 30,
  "max_report_day": 90,
  "max_stat_day": 180
}
```

### 14.15 审计日志
```
GET /business/audit_log
Query: page, page_size
```

### 14.16 报告列表
```
GET /business/report
Query: page, page_size, name
```

### 14.17 创建报告
```
POST /business/report
Content-Type: application/json

{
  "name": "string",
  "begin_time": 1234567890,
  "end_time": 1234567890
}
```

### 14.18 报告详情
```
GET /business/report/{id}
```

### 14.19 删除报告
```
DELETE /business/report/{id}
```

### 14.20 前端样式
```
GET /business/frontend_style
PUT /business/frontend_style
Content-Type: application/json

{
  "title": "string",
  "icon": "string"
}
```

### 14.21 登录安全配置
```
GET /business/account
POST /business/account
Content-Type: application/json

{
  "login_expire": 3600,
  "password_expire_day": 90,
  "password_length": 8,
  "password_complex": [0, 1, 2, 3],
  "access_whitelist": ["1.2.3.4"],
  "lock_config": {
    "login_failed": 5,
    "lock_interval": 30,
    "lock_interval_unit": "minute"
  },
  "expired_unit": "day"
}
```

---

## 15. 告警

### 15.1 告警配置
```
GET /alarm
```

### 15.2 更新告警配置
```
PUT /alarm
Content-Type: application/json

{
  "cur_platform": "string",
  "network_proxy": false,
  "platform": {
    "ding_talk": { "url": "string", "sign": "string" },
    "feishu": { "url": "string", "sign": "string" },
    "qiye_weixin": { "url": "string" },
    "telegram": { "token": "string", "chat_id": 123 },
    "discord": { "url": "string" }
  },
  "event": {
    "attack": { "enabled": true, "alarm_period": 300 },
    "acl": { "enabled": true, "alarm_period": 300 },
    "black_white": { "enabled": true, "alarm_period": 300 },
    "tamper": { "enabled": true, "alarm_period": 300 },
    "auth_defense": { "enabled": true, "alarm_period": 300 },
    "challenge": { "enabled": true, "alarm_period": 300 },
    "waiting_room": { "enabled": true, "alarm_period": 300 },
    "system": { "enabled": true, "alarm_period": 300 }
  },
  "site_ids": []
}
```

### 15.3 测试告警配置
```
POST /alarm/test
```

---

## 16. Syslog

### 16.1 Syslog 配置
```
GET /commercial/syslog
```

### 16.2 更新 Syslog 配置
```
PUT /commercial/syslog
Content-Type: application/json

{
  "type": "string",
  "protocol": "tcp",
  "host": "string",
  "port": 514,
  "rfc": "string",
  "label": "string",
  "log_types": []
}
```

### 16.3 测试 Syslog
```
POST /commercial/syslog/test
```

---

## 17. 阻断页面

### 17.1 获取阻断页面
```
GET /commercial/block_page?type=string
```

### 17.2 更新阻断页面
```
PUT /commercial/block_page
Content-Type: application/json

{
  "type": "string",
  "response_html": "string",
  "recover": false
}
```

### 17.3 阻断页面列表
```
GET /commercial/block_page_list
```

---

## 18. Portal/验证码

### 18.1 Portal 配置
```
GET /open/portal
PUT /open/portal
Content-Type: application/json

{
  "enable": true,
  "domain": "string",
  "ports": ["443"],
  "cert_id": 1,
  "tfa_enabled": true,
  "auth_source_ids": []
}
```

### 18.2 Portal 代理配置
```
GET /open/portal/proxy_config
PUT /open/portal/proxy_config
Content-Type: application/json

{ "force_https": true }
```

### 18.3 Portal 样式
```
GET /open/portal/style
GET /commercial/portal/style
PUT /commercial/portal/style
Content-Type: application/json

{
  "title": "string",
  "icon": "string",
  "theme": "string",
  "app_arrange": "string"
}
```

### 18.4 挑战配置
```
GET /open/challenge/config
POST /open/challenge/config
Content-Type: application/json

{ "server": "cloud" }
```

---

## 19. MCP

### 19.1 获取 MCP 服务器地址
```
GET /mcp
```

### 19.2 设置 MCP 服务器地址
```
POST /mcp
Content-Type: application/json

{
  "server": "string",
  "secret": "string"
}
```

---

## 20. 降级

### 20.1 降级状态
```
GET /open/Commercial/downgrade
```

### 20.2 执行降级
```
PUT /open/Commercial/downgrade
```

---

## 21. 限速放行

### 21.1 限速放行（解封）
```
PUT /open/acl/relieve
Content-Type: application/json

{
  "id": [1, 2],
  "search": {
    "ip": "string",
    "site": "string",
    "begin": 1234567890,
    "end": 1234567890
  }
}
```

---

## 22. 检测器

### 22.1 检测器性能
```
GET /open/detector
POST /open/detector
Content-Type: application/json

{
  "mode": 0,
  "ts": 1234567890
}
```

---

## 23. 威胁情报

### 23.1 恶意 IP 共享计划
```
GET /open/intelligence
POST /open/intelligence
Content-Type: application/json

{ "share_enabled": true }
```

### 23.2 恶意 IP 库
```
POST /open/intelligence/ip_lib
Content-Type: application/json

{ "use_commercial_lib": true }
```

### 23.3 云端策略列表
```
GET /open/cloud/policies?name=string
```

### 23.4 订阅云端策略
```
POST /open/cloud/policies/subscribe
Content-Type: application/json

{ "id": 1 }
```

---

## 24. JA4 指纹

### 24.1 获取 JA4 指纹配置
```
GET /open/ja4
```

### 24.2 设置 JA4 指纹库
```
PUT /open/ja4
Content-Type: application/json

{ "pro": true }
```

---

## 25. 上报行为

### 25.1 共享用户行为
```
GET /open/share_behaviour
POST /open/share_behaviour
Content-Type: application/json

{ "enable": true }
```

### 25.2 共享浏览器指纹
```
GET /open/share_fingerprint
POST /open/share_fingerprint
Content-Type: application/json

{ "enable": true }
```

---

## 26. SafePoint

### 26.1 SafePoint 咨询
```
POST /open/safepoint/consult
Content-Type: application/json

{
  "name": "string",
  "company": "string",
  "phone": "string",
  "email": "string",
  "location": "string",
  "purpose": "string",
  "message": "string",
  "apps": []
}
```

---

## Appendix: Enums

### PolicyAction
| Value | Description |
|-------|-------------|
| 0 | 放行 (Allow) |
| 1 | 阻断 (Deny) |
| 2 | 挑战 (Challenge) |
| 3 | 认证防御 (Auth Defense) |
| 4 | 动态安全 (Chaos) |
| 5 | 等候室 (Waiting Room) |
| 999 | 混合 (Mix) |

### UserRole
| Value | Description |
|-------|-------------|
| 0 | Unknown |
| 1 | Admin |
| 2 | Manager |
| 3 | Config |
| 4 | Audit |

### ACLConfigAction
| Value | Description |
|-------|-------------|
| ban | 封禁 |
| challenge | 挑战 |

### ACLConfigType
| Value | Description |
|-------|-------------|
| req | 高频请求 |
| attack | 高频攻击 |
| error | 高频错误 |

### MatchOperator
| Value | Description |
|-------|-------------|
| eq | 等于 |
| not_eq | 不等于 |
| has | 包含 |
| not_has | 不包含 |
| prefix | 前缀匹配 |
| re | 正则匹配 |

### LicenseState
| Value | Description |
|-------|-------------|
| unknown | 未知 |
| none | 没有 license |
| applying | 应用中 |
| rejected | 无效 |
| approved | 有效 |
| revoked | 回收 |
| revoking | 回收中 |
| expired | 过期 |
| verify_failed | 请求百川报错 |
| downgrade | license 降级 |
| arch_mismatch | 架构不匹配 |

### AuthDefenseSourceType
| Value | Description |
|-------|-------------|
| 0 | Pass |
| 1 | 微信账号平台 |
| 2 | GitHub |
| 3 | CAS |
| 4 | OIDC |
| 5 | 钉钉 |
| 6 | 企业微信 |
| 7 | LDAP |

### Edition
| Value | Description |
|-------|-------------|
| 0 | 未知 |
| 1 | 专业版 |
| 2 | 企业版 |
| 3 | Lite 版 |

### ChallengeServer
| Value | Description |
|-------|-------------|
| cloud | 云端 |
| local | 本地 |

### TrendsQueryType
| Value | Description |
|-------|-------------|
| attack | 攻击趋势 |
| black_white | 黑白名单趋势 |
| acl | 限速趋势 |
| challenge | 挑战趋势 |
| waiting | 等候室趋势 |
| auth | 认证趋势 |
| attack_type | 攻击类型趋势 |
| black_white_rule | 黑白名单规则趋势 |
| attack_host | 攻击主机趋势 |
| attack_page | 攻击页面趋势 |
