# ACME 服务整体优化设计

- 日期：2026-06-30
- 作者：phoebej（与 Claude Code 协作）
- 范围：HMoneta 后端 ACME 服务 + 前端证书管理，全量优化（修 bug + 架构重构 + 新功能）

## 背景与动机

当前 ACME 实现"能跑通但有若干 bug 且难扩展"。核心问题：

| # | 类别 | 问题 | 位置 |
|---|------|------|------|
| 1 | 🔴 Bug | `AcmeExceptionEnum.getCode()/getMessage()` 都返回 `""`，所有 ACME 异常码和文案丢失 | `AcmeExceptionEnum.java:24,29` |
| 2 | 🔴 Bug | `AcmeAsyncLogEntity` + Repository 已注入但从不落库（finally 里保存逻辑被注释），任务状态无法追踪 | `AcmeServiceComponent.java:266-274` |
| 3 | 🔴 Bug | `renewCertification(domain, taskId)` 内部调 `applyCertification` 又生成新 taskId，传入的 taskId 只用于日志、返回值被丢弃 | `AcmeService.java:124-132` |
| 4 | 🔴 Bug | 默认 `acme.url` 是 staging，申请出的证书浏览器不信任 | `application.yml:54` |
| 5 | 🟠 健壮性 | 域名解析 `substring(indexOf('.'))` 对多级 TLD/泛域名/根域全错；多处 `.get()` 裸调用 NPE 风险 | `AcmeService.java:248-249`、`AcmeServiceComponent.java:179-180` |
| 6 | 🟠 架构 | `useDnsChallengeGetCertification` 130+ 行深嵌套；`@Async + @Transactional` 让申请在 DNS 传播等待期间一直占 DB 连接 | `AcmeServiceComponent.java:127-276` |
| 7 | 🟠 架构 | 证书文件读写散落 3 处；路径 `"certs/"+domain` 硬编码相对路径 | `AcmeService.java`、`AcmeServiceComponent.java` |
| 8 | 🟡 功能 | 申请 fire-and-forget，无状态查询接口；续期无并发锁，同域可能并发重复申请 | controller / task |
| 9 | 🟡 功能 | 仅单域名、单 CA；无 SAN/泛域名/多 CA；无证书删除；下载只能整包 ZIP | 全局 |
| 10 | 🟡 代码 | `log`（@Log4j2）与 `logger`（手写 SLF4J）混用 | `AcmeServiceComponent.java` |

## 决策摘要（来自需求澄清）

- 范围：C = A（修 bug/稳现有流程）+ B（架构重构）+ 新功能（多域名/SAN/泛域名、多 CA、证书删除、内容预览、ZIP 下载、续期并发锁）。
- 数据模型：1 证书 = N 域名（SAN），证书与 `DnsResolveUrl` 解耦，新增「证书管理」页。
- CA：预置主流 CA（Let's Encrypt 生产/staging、ZeroSSL、Google Trust Services），前端下拉选 CA，首次申请自动建账号；staging 为带⚠️的显式选项。
- 任务追踪：修复 `AcmeAsyncLogEntity` 落库 + 新增 `GET /hm/acme/task/{taskId}`；前端申请后轮询该接口，完成弹通知；实时进度复用现有 SSE 日志流。
- 文件管理：统一 `CertificateFileManager`，目录结构 `certs/{certId}/`；下载 = PEM 内容预览 + 整包 ZIP；**无需兼容旧数据**。
- 证书删除：仅本地清理（删 DB 记录 + 删 `certs/{certId}/` 目录），不调 ACME 吊销。
- 并发控制：进程内锁（`ConcurrentHashMap<certId, ReentrantLock>`），同 certId 串行、跨 certId 并行；同 certId 已有任务在跑时**快速失败不等待**。
- 续期时机：保持现状（每日 0 点扫描，≤30 天触发）。
- 架构拆分：方案 2 = 领域分层（门面 + 4 协作组件 + 2 工具）。
- 邮箱：全局邮箱方案，设置页保留邮箱输入卡片。

## 架构总览（方案 2：领域分层）

```
CertificateService (门面/编排: 锁 + 任务日志 + @Async)
   ├── AcmeCaRegistry        (内存枚举, CA 元数据)
   ├── AcmeDomainResolver    (域名解析工具)
   ├── AcmeAccountService    (CA 账号: 按需创建/登录)
   ├── AcmeOrderService      (ACME 订单/挑战/签发, 事务收窄)
   ├── CertificateFileManager(文件读写唯一出口)
   └── AcmeAsyncLogRepository(任务日志)
```

依赖单向，每个组件单一职责、可独立单测。

## 数据模型

### ① `AcmeUserInfoEntity`（改造 → 关联 CA）

```
acme_user_info
├─ id (PK, String/UUID)
├─ ca_code        (新增, 如 "letsencrypt" / "letsencrypt_staging" / "zerossl" / "gts")
├─ userEmail
├─ publicKey / privateKey  (该 CA 的账号 KeyPair, 按需创建)
└─ unique(ca_code)         (每个 CA 一个账号)
```

旧表数据无需兼容（迁移直接清空重建）。

### ② `AcmeCertificationEntity`（改造 → 支持多域名 SAN）

```
acme_certification
├─ id (PK, Long)
├─ cert_id        (新增, UUID, 业务主键, 对应 certs/{certId}/ 目录)
├─ name           (新增, 证书显示名, 用户起)
├─ ca_code        (新增, 签发该证书的 CA)
├─ domains        (替换原 domain 字段 → SAN 列表)
│   ├─ @JdbcTypeCode(SqlTypes.JSON) List<String>, PG 存 jsonb
│   └─ 含泛域名 *.example.com; 第一个为 CN/主域名
├─ provider_name  (保留, DNS 供应商, 续期用)
├─ cert_public_key / cert_private_key  (保留, 证书本身的 KeyPair)
├─ cert_apply_time / not_before / not_after  (保留)
└─ (删除原 domain 字段)
```

`AcmeCerInfoResp` 跟着变：不再 `findOneByDomain`，按 certId 查、返回 domains 列表。

### ③ `AcmeAsyncLogEntity`（改造 → 任务状态追踪，修 #2/#3）

```
acme_async_log
├─ task_id (PK, String)
├─ cert_id        (新增, 关联证书)
├─ domains        (新增, 本次涉及域名, 日志展示用; @JdbcTypeCode(SqlTypes.JSON) List<String>, jsonb)
├─ type           (新增, APPLY / RENEW)
├─ status         (替换原 boolean success → PENDING/RUNNING/SUCCESS/FAILED)
├─ log_info       (保留, 汇总文案)
├─ start_time     (新增)
└─ end_time       (新增)
```

### ④ `AcmeGlobalConfigEntity`（新增 → 全局邮箱）

```
acme_global_config
├─ id (PK, 固定 = 1, 单行)
└─ user_email     (全局邮箱, 设置页配置)
```

`/hm/acme/email` GET/POST 操作此表。`AcmeAccountService.findOrCreateAccount` 创建新 CA 账号时从此读取邮箱写入对应 `AcmeUserInfoEntity.userEmail`。这样"全局邮箱"有唯一明确存储，不与按 CA 拆分的 `AcmeUserInfoEntity` 产生歧义。

### 证书 ↔ DNS 解耦

证书不再挂在 `DnsResolveUrl` 上；删除 `DnsService.queryAllDnsResolve` 里逐 URL 查证书的 join（`DnsService.java:247-258`），删除 `DnsResolveUrlResp.acmeCerInfo` 字段。证书信息改由新的「证书管理」页面独立展示。

## 组件设计

### `AcmeCaRegistry`（枚举, 无状态）

预置项：`LETS_ENCRYPT(prod)`、`LETS_ENCRYPT_STAGING(staging, ⚠️)`、`ZEROSSL`、`GOOGLE_TRUST_SERVICES`。每项含 `code`、`directoryUrl`、`staging` 标志、`displayName`。提供 `getByCode()`、`list()`。前端下拉和后端申请都从这里取。

### `AcmeDomainResolver`（静态工具）

入参一个域名，输出 `{mainDomain, txtHost}`，用于 DNS-01 的 TXT 记录定位。规则：

- 去掉前缀 `*.`（泛域名 DNS-01 校验域是 `_acme-challenge.<去掉*的部分>`）。
- 用"内置常见多级 TLD 白名单"识别主域。白名单覆盖：`.com.cn / .net.cn / .org.cn / .gov.cn / .co.uk / .com.au / .co.jp` 等；其余按"最后两段为主域"回退。不引第三方 PSL 库。
- apex（`example.com`）→ `txtHost="_acme-challenge"`、`mainDomain="example.com"`。
- `www.example.com` → `txtHost="_acme-challenge.www"`、`mainDomain="example.com"`。

替换 `AcmeServiceComponent.java:179-180` 的 `substring(indexOf('.'))` 脆弱写法。

### `AcmeAccountService`

- `findOrCreateAccount(caCode) → AcmeUserInfoEntity`：按 `ca_code` 查；无则建（Session 设超时 120s、KeyPair 创建、`AccountBuilder.create()`、落库）。把现有 `findOrCreateAcmeUser` 的"永远取 get(0)"改为按 caCode 取。
- `login(userInfo, session) → Login`：带重试（现有 3 次/5s 逻辑搬这里）。

### `AcmeOrderService`（核心, 拆短方法 + 收窄事务）

把现有 130 行 `useDnsChallengeGetCertification` 拆成：

- `placeOrder(login, domains) → Order`：支持 N 域名（`.domain(d1).domain(d2)...`），含 `*.x` 泛域名。
- `solveDns01Challenges(order, plugin, domains)`：遍历**每个 authorization**（不再是 `get(0)`），写 TXT、`waitForDnsPropagation`、`trigger`、轮询状态。
- `cleanupTxt(plugin, domains)`：统一清理（保证异常也清理）。
- `finalizeAndIssue(order) → Certificate`：CSR、轮询签发、返回。

事务边界：`@Transactional` 只包"写 `acme_certification` 记录 + 保存 KeyPair"这一小段；DNS 等待、网络 IO 不进事务。修 #6。`@Async` 保留在门面 `CertificateService` 层，不与 `@Transactional` 叠加在同一方法。

异常处理：内部 acme4j 抛的 `AcmeException` 等 catch 后记入任务日志（status=FAILED, logInfo=e.getMessage），再向上抛 `HMException(ACME_APPLY_FAILED)`，由全局异常处理转成统一响应。不直接 `throw new RuntimeException(e)`。

### `CertificateFileManager`（唯一文件出口）

根目录 `${acme.cert-dir:certs}`（可配置，修 #7）。

- `save(certId, primaryDomain, keyPair, certificate)` → 写 `certs/{certId}/{primaryDomain}.key|.crt|.pem|.fullchain.pem`
- `readContent(certId, primaryDomain) → Map<格式, PEM文本>`（预览用）
- `packZip(certId, primaryDomain) → byte[]`（整包下载）
- `delete(certId)`（删目录）

收编现有散落在 `AcmeServiceComponent.saveCertificateFiles` 和 `AcmeService.packCertifications` / `getCertAndKeyByDomain` 的逻辑。

## 并发锁（修 #8）

`CertificateService` 内持 `ConcurrentHashMap<String, ReentrantLock>`，key=certId。申请/续期入口 `tryLock(certId)`：

- 拿到锁 → 执行；finally 释放。
- 拿不到 → 直接抛 `HMException(CERT_TASK_RUNNING)`，不阻塞等待。

覆盖手动申请 vs 手动申请、定时续期 vs 手动申请、定时续期 vs 定时续期（同 certId）。跨 certId 天然并行。申请"新建证书"时还没 certId，用临时 UUID 作为锁 key，与该证书后续续期 key 一致。

## 任务追踪（修 #2/#3）

```
申请/续期入口(CertificateService)
 ├─ 1. 建 AcmeAsyncLogEntity(taskId, certId, domains, type, status=PENDING, startTime) 并 save
 ├─ 2. tryLock(certId)；失败 → 更新 status=FAILED(logInfo="任务进行中") 并返回 taskId
 ├─ 3. status=RUNNING 并 save
 ├─ 4. @Async 调 AcmeOrderService 编排（SSE 日志流照常实时推，不动）
 ├─ 5. 成功 → status=SUCCESS + 落 acme_certification；失败 → status=FAILED + logInfo=异常文案
 └─ 6. endTime 落库
```

关键修复：续期用调用方传入的 taskId（修 #3）。`AcmeUpdateTask` 生成 taskId 后传入，整个链路用同一个 taskId。

`AcmeAsyncLogRepository` 加 `findByCertIdOrderByStartTimeDesc(certId)`。

## 续期任务改造（`AcmeUpdateTask`）

- 扫描对象从"逐域名"改为"逐证书"（`acme_certification` 表），按证书 `not_after` 判断临期（≤ `${acme.renewal.threshold-days:30}`）。
- 证书维度所有 SAN 域名一起续（整张重签）。
- 每个临期证书生成一个 taskId，调 `CertificateService.renewCertificate(certId, taskId)`（新签名）。
- 定时任务只负责"扫描 + 派发"，不关心单证书成败（失败已在任务日志里）。

## REST API

统一 `/hm/acme` 前缀，`@RestController` + `ResponseEntity`。

| 方法 | 路径 | 说明 | 替代 |
|------|------|------|------|
| GET | `/hm/acme/cas` | 列出可选 CA（含 staging⚠️标记） | 新增 |
| GET | `/hm/acme/list` | 列出所有证书（certId/name/domains/ca/有效期/临期标志） | 新增 |
| GET | `/hm/acme/{certId}` | 单证书详情 | 新增 |
| POST | `/hm/acme/apply` | body `{name, caCode, domains[], providerName?}` → `{taskId}` | 扩展原 `/apply` |
| POST | `/hm/acme/renew/{certId}` | 手动续期 → `{taskId}` | 新增 |
| DELETE | `/hm/acme/{certId}` | 删 DB 记录 + `certs/{certId}/`（仅本地） | 新增 |
| GET | `/hm/acme/task/{taskId}` | 查任务状态（PENDING/RUNNING/SUCCESS/FAILED + logInfo + domains） | 新增 |
| GET | `/hm/acme/{certId}/content` | 证书 PEM 内容预览 → `{key, crt, pem, fullchain}` | 替代 `/str-cert/{domain}` |
| GET | `/hm/acme/{certId}/download` | 整包 ZIP 下载 | 替代 `/download-cert/{domain}` |
| GET | `/hm/acme/email` | 取全局邮箱 | 新增（回填用） |
| POST | `/hm/acme/email` | 存全局邮箱 | 替代原 `/modify` |

删除的旧端点：`GET /apply`、`GET /download-cert/{domain}`、`GET /str-cert/{domain}`、`POST /modify`。

端点命名风格 RESTful 嵌套（`/{certId}/download`、`/{certId}/content`），与现有 DNS 查询的下划线动词风格不同，但 ACME 资源型操作用 RESTful 更清晰。破坏性路径变更前端同步改，无需兼容。

## 前端

### 新增页面 `HMfront/hm-front/src/pages/certManager.vue`

- 顶栏：「申请证书」按钮（打开申请对话框）。
- 列表：表格 = 证书名 / 域名（SAN 折叠展示，泛域名带标记）/ CA / 有效期 / 状态徽章（有效🟢/临期🟡/过期🔴/申请中⏳）。
- 操作：每行 续期 / 下载 ZIP / 预览内容 / 删除（`ConfirmDialog` 二次确认）。
- 任务态：申请/续期后按 taskId 轮询 `GET /task/{taskId}`（间隔 ~2s，到 SUCCESS/FAILED 停），完成弹 `notificationStore.showSuccess/Error`，刷新列表。
- 申请对话框字段：证书名（text）、CA（下拉，`GET /cas`，staging 带⚠️）、域名（多选/标签输入，支持 `*.x`）、DNS 供应商（下拉，复用 DNS 页供应商列表，因为写 TXT 要知道用哪个供应商）。域名校验前端正则 + 后端 `AcmeDomainResolver` 兜底。

### 修改 `dns.vue`（移除证书耦合）

删除 `applyCertificate`、`isCertificateExpiring`、`getCertificateClass`、`getPopText`（`dns.vue:148-205`），及模板里证书相关按钮/tooltip/样式（`cert-valid/cert-expired/cert-unavailable`）。DNS 页回归纯 DNS 解析管理。

### 修改 `setting.vue`（保留邮箱卡片）

- ACME 配置卡片保留邮箱输入；`onSubmit` 改打 `POST /hm/acme/email`。
- 文案补"该邮箱用于所有 CA 账号注册"。
- 加载时 `GET /hm/acme/email` 回填（现无回填逻辑，顺手补）。

### 导航 `layouts/default.vue`

菜单加「证书管理」→ `/certManager`（icon `mdi-certificate-outline`），路由由 `unplugin-vue-router` 自动生成。

SSE 日志不动；证书申请/续期实时日志走全局 SSE 日志流，`certManager.vue` 轮询只负责"任务结束态"。

## Bug 修复与配置

### `AcmeExceptionEnum`

- `getCode()` / `getMessage()` 改为 `return this.code / this.message`（修 #1）。
- 扩充枚举值：`CERT_TASK_RUNNING`(008, 任务进行中)、`CERT_NOT_FOUND`(009, 证书不存在)、`CA_NOT_FOUND`(010, CA 不支持)、`ACME_APPLY_FAILED`(011, 证书申请失败)、`CERT_FILE_ERROR`(012, 证书文件读写错误)。

### 健壮性 / 架构（实现方式见各组件段）

- #5 域名解析 → `AcmeDomainResolver`，删 `substring` 写法。
- #5 裸 `.get()` → `ifPresentOrElse` + 抛 `HMException`。
- #6 长事务 → `AcmeOrderService` 收窄事务边界。
- #7 文件散落 + 硬编码 → `CertificateFileManager` + `${acme.cert-dir}`。
- #10 日志混用 → 全部统一 `@Log4j2` 的 `log`。

### 配置（`application.yml`）

```yaml
acme:
  url: https://acme-v02.api.letsencrypt.org/directory   # 改生产(修 #4); 实际申请用 AcmeCaRegistry 各 CA 自带 URL, 此项仅默认回退
  cert-dir: ${ACME_CERT_DIR:certs}                       # 新增, 可配置, 默认 certs
  renewal:
    threshold-days: 30                                   # 沿用
```

- `application-dev.yml` 的 `acme.url: ...staging...` 保留（dev 默认打 staging，避免开发撞速率限制）。

## 测试策略

- `AcmeDomainResolver`：单测覆盖 apex / 普通 sub / 泛域名 / 多级 TLD（.com.cn / .co.uk）/ 回退场景。
- `AcmeCaRegistry`：单测 `getByCode` 命中/未命中、`list` 完整性。
- `CertificateFileManager`：单测 save → readContent → packZip → delete 往返（用临时目录）。
- `CertificateService` 并发锁：单测同 certId 第二次 `tryLock` 抛 `CERT_TASK_RUNNING`。
- `AcmeOrderService`：对 acme4j 真实网络交互不写单测（依赖外部 CA），但对其内部的 TXT 清理、订单域名组装等纯函数部分单测。
- 现有 `mvn test -Dtest="fan.summer.hmoneta.service.unifi.*"` 模式可参考，ACME 测试置于 `fan.summer.hmoneta.service.acme.*`。

## 不做（YAGNI）

- ACME 证书吊销（`cert.revoke()`）——短有效期，意义不大。
- 分布式锁 / 多实例部署支持——homelab 单机。
- 第三方 Public Suffix List 依赖——内置白名单够用。
- 单文件下载、复制到剪贴板按钮——本次只要预览 + ZIP。
- SAN 多域名的二阶段拆分——本次直接做完整 SAN。
- 旧 `certs/` 目录数据迁移——无需兼容。
