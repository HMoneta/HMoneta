# ACME 服务整体优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 ACME 服务从"单域名/单 CA/fire-and-forget + 若干 bug"升级为"多域名 SAN + 多 CA + 任务可追踪 + 文件统一管理 + 续期并发锁"的可靠系统。

**Architecture:** 方案 2 领域分层——`CertificateService` 门面（锁 + 任务日志 + `@Async`）编排 4 个协作组件：`AcmeAccountService`（账号）、`AcmeOrderService`（订单/挑战/签发，事务收窄）、`CertificateFileManager`（文件唯一出口）、外加 `AcmeCaRegistry`（CA 枚举）和 `AcmeDomainResolver`（域名解析）。证书与 DNS 解耦，独立成页。

**Tech Stack:** Spring Boot 4.0.5 / Java 25 / acme4j-client 3.5.0 / JPA + PostgreSQL（jsonb via `@JdbcTypeCode(SqlTypes.JSON)`）/ JUnit 5 + Mockito / Vue 3 + Vuetify + Pinia。

**Spec:** `docs/superpowers/specs/2026-06-30-acme-optimization-design.md`

---

## 文件结构

### 新建（后端）
- `src/main/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistry.java` — CA 元数据枚举
- `src/main/java/fan/summer/hmoneta/util/AcmeDomainResolver.java` — 健壮域名解析（apex/泛域名/多级 TLD）
- `src/main/java/fan/summer/hmoneta/service/acme/AcmeAccountService.java` — CA 账号按需创建/登录
- `src/main/java/fan/summer/hmoneta/service/acme/AcmeOrderService.java` — 订单/挑战/签发（事务收窄）
- `src/main/java/fan/summer/hmoneta/service/acme/CertificateFileManager.java` — 文件唯一出口
- `src/main/java/fan/summer/hmoneta/database/entity/acme/AcmeGlobalConfigEntity.java` — 全局邮箱单行表
- `src/main/java/fan/summer/hmoneta/database/repository/acme/AcmeGlobalConfigRepository.java`
- `src/main/java/fan/summer/hmoneta/controller/acme/dto/req/CertApplyReq.java` — 申请请求 record
- `src/main/java/fan/summer/hmoneta/controller/acme/dto/resp/AcmeTaskStatusResp.java` — 任务状态响应 record
- `src/main/java/fan/summer/hmoneta/controller/acme/dto/resp/AcmeCaResp.java` — CA 列表项 record
- `src/main/java/fan/summer/hmoneta/controller/acme/dto/resp/CertInfoResp.java` — 证书列表/详情 record
- `src/main/java/fan/summer/hmoneta/controller/acme/dto/resp/CertContentResp.java` — PEM 内容预览 record
- `src/test/java/fan/summer/hmoneta/util/AcmeDomainResolverTest.java`
- `src/test/java/fan/summer/hmoneta/service/acme/CertificateFileManagerTest.java`
- `src/test/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistryTest.java`
- `src/test/java/fan/summer/hmoneta/service/acme/CertificateServiceConcurrencyTest.java`

### 新建（前端）
- `HMfront/hm-front/src/pages/certManager.vue` — 证书管理页

### 修改（后端）
- `common/enums/exception/acme/AcmeExceptionEnum.java` — 修 getCode/getMessage + 扩容枚举
- `database/entity/acme/AcmeUserInfoEntity.java` — 加 `caCode` + unique
- `database/entity/acme/AcmeCertificationEntity.java` — `certId/name/caCode/domains(jsonb)/dnsGroupId`，删 `domain`
- `database/entity/acme/AcmeAsyncLogEntity.java` — `certId/domains/type/status/startTime/endTime`
- `database/repository/acme/AcmeUserInfoRepository.java` — `findByCaCode`
- `database/repository/acme/AcmeCertificationRepository.java` — 按 certId 操作
- `database/repository/acme/AcmeAsyncLogRepository.java` — `findByCertIdOrderByStartTimeDesc`
- `service/acme/AcmeService.java` — 重构为 `CertificateService` 门面（保留类名以减小 diff）
- `service/acme/AcmeServiceComponent.java` — 删除（逻辑拆入 AcmeAccountService/AcmeOrderService/CertificateFileManager）
- `service/acme/AcmeTaskContext.java` — 扩展为携带 `caCode/domains/certId`
- `service/dns/DnsService.java` — 移除 `queryCertificationInfo` join（`:247-258`）
- `controller/dns/entity/resp/DnsResolveUrlResp.java` — 删 `acmeCerInfo` 字段
- `controller/acme/AcmeController.java` — 重写为新 API
- `controller/acme/dto/resp/AcmeCerInfoResp.java` — 删除（DNS 不再带证书信息）
- `controller/acme/dto/AcmeUserReq.java` — 改为 `EmailReq`
- `task/acme/AcmeUpdateTask.java` — 逐证书扫描 + 传 taskId
- `common/config/HMInterceptor.java` — 无需改（`/hm/acme/**` 受保护）
- `src/main/resources/application.yml` / `application-dev.yml` — CA url 生产 + `cert-dir`

### 修改（前端）
- `HMfront/hm-front/src/pages/dns.vue` — 移除证书 UI（`:148-205` + 模板/样式）
- `HMfront/hm-front/src/pages/setting.vue` — 邮箱改打 `/hm/acme/email` + 回填
- `HMfront/hm-front/src/layouts/default.vue` — 加「证书管理」菜单项

---

## Task 1: 修复 AcmeExceptionEnum + 扩容枚举

**Files:**
- Modify: `src/main/java/fan/summer/hmoneta/common/enums/exception/acme/AcmeExceptionEnum.java`

这是后续所有任务依赖的基础（修 #1 Bug：getCode/getMessage 返回空串）。

- [ ] **Step 1: 重写 AcmeExceptionEnum**

完整替换文件内容：

```java
package fan.summer.hmoneta.common.enums.exception.acme;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

public enum AcmeExceptionEnum implements HMExceptionEnum {
    CER_ERROR_LOG_NOT_FIND("001", "未找到指定日志"),
    CER_ERROR_FOLDER_NOT_EXIST("002", "证书文件夹不存在"),
    CER_CREATE_FOLDER_ERROR("003", "创建证书文件夹失败"),
    DNS_SERVICE_NOT_FOUND_PROVIDER_ERROR("004", "未找到DNS供应商"),
    ACME_ACCOUNT_UPDATE_ERROR("005", "更新ACME账户时出错"),
    ACME_ACCOUNT_NOT_EXIST_ERROR("006", "ACME账户不存在"),
    CER_NOT_EXIST_ERROR("007", "证书文件不存在"),
    CERT_TASK_RUNNING("008", "该证书已有任务进行中"),
    CERT_NOT_FOUND("009", "证书不存在"),
    CA_NOT_FOUND("010", "不支持的CA"),
    ACME_APPLY_FAILED("011", "证书申请失败"),
    CERT_FILE_ERROR("012", "证书文件读写错误");

    private final String code;
    private final String message;

    AcmeExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/common/enums/exception/acme/AcmeExceptionEnum.java
git commit -m "fix(acme): 修复异常枚举getCode/getMessage返回空串, 扩容错误码008-012"
```

---

## Task 2: AcmeCaRegistry 枚举（CA 元数据）

**Files:**
- Create: `src/main/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistry.java`
- Test: `src/test/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistryTest.java`

- [ ] **Step 1: 写失败测试**

```java
package fan.summer.hmoneta.common.enums.acme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcmeCaRegistryTest {

    @Test
    void should_get_ca_by_code() {
        AcmeCaRegistry ca = AcmeCaRegistry.getByCode("letsencrypt");
        assertEquals("letsencrypt", ca.getCode());
        assertEquals("https://acme-v02.api.letsencrypt.org/directory", ca.getDirectoryUrl());
        assertFalse(ca.isStaging());
    }

    @Test
    void should_mark_staging_ca() {
        AcmeCaRegistry ca = AcmeCaRegistry.getByCode("letsencrypt_staging");
        assertTrue(ca.isStaging());
    }

    @Test
    void should_list_all_cas() {
        assertTrue(AcmeCaRegistry.list().size() >= 4);
    }

    @Test
    void should_throw_for_unknown_ca() {
        assertThrows(IllegalArgumentException.class, () -> AcmeCaRegistry.getByCode("nope"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.common.enums.acme.AcmeCaRegistryTest"`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 AcmeCaRegistry**

```java
package fan.summer.hmoneta.common.enums.acme;

import lombok.Getter;

/**
 * 预置 ACME CA 元数据。每个 CA 一个 code，对应一个 directoryUrl。
 * staging CA 带 isStaging=true。
 */
@Getter
public enum AcmeCaRegistry {
    LETS_ENCRYPT("letsencrypt", "Let's Encrypt", "https://acme-v02.api.letsencrypt.org/directory", false),
    LETS_ENCRYPT_STAGING("letsencrypt_staging", "Let's Encrypt (Staging)", "https://acme-staging-v02.api.letsencrypt.org/directory", true),
    ZEROSSL("zerossl", "ZeroSSL", "https://acme.zerossl.com/v2/DV90", false),
    GOOGLE_TRUST_SERVICES("gts", "Google Trust Services", "https://dv.acme-v02.api.pki.goog/directory", false);

    private final String code;
    private final String displayName;
    private final String directoryUrl;
    private final boolean staging;

    AcmeCaRegistry(String code, String displayName, String directoryUrl, boolean staging) {
        this.code = code;
        this.displayName = displayName;
        this.directoryUrl = directoryUrl;
        this.staging = staging;
    }

    public static AcmeCaRegistry getByCode(String code) {
        for (AcmeCaRegistry ca : values()) {
            if (ca.code.equals(code)) {
                return ca;
            }
        }
        throw new IllegalArgumentException("未知的 CA code: " + code);
    }

    public static java.util.List<AcmeCaRegistry> list() {
        return java.util.Arrays.asList(values());
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.common.enums.acme.AcmeCaRegistryTest"`
Expected: PASS（4 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistry.java src/test/java/fan/summer/hmoneta/common/enums/acme/AcmeCaRegistryTest.java
git commit -m "feat(acme): 预置 CA 元数据枚举 AcmeCaRegistry"
```

---

## Task 3: AcmeDomainResolver（健壮域名解析）

**Files:**
- Create: `src/main/java/fan/summer/hmoneta/util/AcmeDomainResolver.java`
- Test: `src/test/java/fan/summer/hmoneta/util/AcmeDomainResolverTest.java`

修 #5 域名解析脆弱 Bug。输出 `{mainDomain, txtHost}` 供 DNS-01 TXT 记录定位。

- [ ] **Step 1: 写失败测试**

```java
package fan.summer.hmoneta.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcmeDomainResolverTest {

    @Test
    void should_resolve_normal_subdomain() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("www.example.com");
        assertEquals("example.com", r.mainDomain());
        assertEquals("_acme-challenge.www", r.txtHost());
    }

    @Test
    void should_resolve_apex() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("example.com");
        assertEquals("example.com", r.mainDomain());
        assertEquals("_acme-challenge", r.txtHost());
    }

    @Test
    void should_resolve_wildcard_by_stripping_star() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("*.example.com");
        assertEquals("example.com", r.mainDomain());
        assertEquals("_acme-challenge", r.txtHost());
    }

    @Test
    void should_resolve_wildcard_subdomain() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("*.app.example.com");
        assertEquals("example.com", r.mainDomain());
        assertEquals("_acme-challenge.app", r.txtHost());
    }

    @Test
    void should_resolve_multi_level_tld() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("www.example.com.cn");
        assertEquals("example.com.cn", r.mainDomain());
        assertEquals("_acme-challenge.www", r.txtHost());
    }

    @Test
    void should_resolve_apex_multi_level_tld() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("example.com.cn");
        assertEquals("example.com.cn", r.mainDomain());
        assertEquals("_acme-challenge", r.txtHost());
    }

    @Test
    void should_resolve_codotuk() {
        AcmeDomainResolver.Resolved r = AcmeDomainResolver.resolve("www.example.co.uk");
        assertEquals("example.co.uk", r.mainDomain());
        assertEquals("_acme-challenge.www", r.txtHost());
    }

    @Test
    void should_throw_for_invalid_domain() {
        assertThrows(IllegalArgumentException.class, () -> AcmeDomainResolver.resolve("not-a-domain"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.util.AcmeDomainResolverTest"`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 AcmeDomainResolver**

```java
package fan.summer.hmoneta.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * ACME DNS-01 域名解析。把一个域名拆成 {mainDomain, txtHost}，
 * 用于在 DNS 供应商处定位 _acme-challenge TXT 记录。
 * 处理 apex、泛域名(去掉 *.前缀)、多级 TLD。
 */
public final class AcmeDomainResolver {

    private static final Pattern VALID = Pattern.compile(
            "^(\\*\\.)?([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");

    /** 常见多级 TLD 白名单。其余按"最后两段为主域"回退。 */
    private static final Set<String> MULTI_LEVEL_TLDS = Set.of(
            "com.cn", "net.cn", "org.cn", "gov.cn", "ac.cn",
            "co.uk", "co.jp", "com.au", "com.br", "com.tw");

    private AcmeDomainResolver() {
    }

    public record Resolved(String mainDomain, String txtHost) {
    }

    public static Resolved resolve(String domain) {
        if (domain == null || !VALID.matcher(domain).matches()) {
            throw new IllegalArgumentException("非法域名: " + domain);
        }
        String d = domain;
        if (d.startsWith("*.")) {
            d = d.substring(2);
        }
        String[] parts = d.split("\\.");
        // 末尾两段或三段(若命中多级TLD)构成主域
        String mainDomain;
        if (parts.length >= 3 && MULTI_LEVEL_TLDS.contains(parts[parts.length - 2] + "." + parts[parts.length - 1])) {
            mainDomain = parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else {
            mainDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        String subPart = d.substring(0, d.length() - mainDomain.length());
        if (subPart.endsWith(".")) {
            subPart = subPart.substring(0, subPart.length() - 1);
        }
        String txtHost = subPart.isEmpty() ? "_acme-challenge" : "_acme-challenge." + subPart;
        return new Resolved(mainDomain, txtHost);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.util.AcmeDomainResolverTest"`
Expected: PASS（8 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/util/AcmeDomainResolver.java src/test/java/fan/summer/hmoneta/util/AcmeDomainResolverTest.java
git commit -m "feat(acme): 健壮域名解析 AcmeDomainResolver(支持apex/泛域名/多级TLD)"
```

---

## Task 4: 数据模型改造（实体 + 仓库）

**Files:**
- Modify: `database/entity/acme/AcmeUserInfoEntity.java`
- Modify: `database/entity/acme/AcmeCertificationEntity.java`
- Modify: `database/entity/acme/AcmeAsyncLogEntity.java`
- Create: `database/entity/acme/AcmeGlobalConfigEntity.java`
- Modify: `database/repository/acme/AcmeUserInfoRepository.java`
- Modify: `database/repository/acme/AcmeCertificationRepository.java`
- Modify: `database/repository/acme/AcmeAsyncLogRepository.java`
- Create: `database/repository/acme/AcmeGlobalConfigRepository.java`
- Delete: `controller/acme/dto/resp/AcmeCerInfoResp.java`

> 说明：`ddl-auto: update` 只增列不删列、不强制改类型。旧 `domain` 列会残留为空但无害；本任务用新字段名 `domains`，不读写旧 `domain`。后续任务在首次保存 SAN 证书前，可手动 `DELETE FROM acme_certification; DELETE FROM acme_user_Info;` 清空旧数据（spec 确认无需兼容）。

- [ ] **Step 1: 重写 AcmeUserInfoEntity（加 caCode）**

```java
package fan.summer.hmoneta.database.entity.acme;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Acme 账号信息，按 CA 分（caCode 唯一）。
 */
@Data
@Entity
@Table(name = "acme_user_Info",
        uniqueConstraints = @UniqueConstraint(name = "uk_acme_user_ca", columnNames = "caCode"))
public class AcmeUserInfoEntity {
    @Id
    private String id;
    private String caCode;
    private String userEmail;

    @Lob
    @Column(columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] publicKey;

    @Lob
    @Column(columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] privateKey;

    public void saveKeyPair(KeyPair keyPair) {
        this.publicKey = keyPair.getPublic().getEncoded();
        this.privateKey = keyPair.getPrivate().getEncoded();
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return new KeyPair(
                keyFactory.generatePublic(new X509EncodedKeySpec(this.publicKey)),
                keyFactory.generatePrivate(new PKCS8EncodedKeySpec(this.privateKey)));
    }
}
```

- [ ] **Step 2: 重写 AcmeCertificationEntity（certId/name/caCode/domains/dnsGroupId）**

```java
package fan.summer.hmoneta.database.entity.acme;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 一张 ACME 证书 = N 个域名(SAN)。
 */
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class AcmeCertificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String certId;          // UUID 业务主键, 对应 certs/{certId}/
    private String name;            // 用户起的证书名
    private String caCode;          // 签发 CA
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> domains;   // SAN 列表, 含泛域名; 第一个为 CN
    private String dnsGroupId;      // 续期时回查 DNS 组获取凭据
    private String providerName;    // DNS 供应商显示名
    private byte[] certPublicKey;
    private byte[] certPrivateKey;
    private LocalDateTime certApplyTime;
    private Date notBefore;
    private Date notAfter;

    public void saveKeyPair(KeyPair keyPair) {
        this.certPublicKey = keyPair.getPublic().getEncoded();
        this.certPrivateKey = keyPair.getPrivate().getEncoded();
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return new KeyPair(
                keyFactory.generatePublic(new X509EncodedKeySpec(this.certPublicKey)),
                keyFactory.generatePrivate(new PKCS8EncodedKeySpec(this.certPrivateKey)));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oc = o instanceof HibernateProxy h ? h.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> tc = this instanceof HibernateProxy h ? h.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (tc != oc) return false;
        AcmeCertificationEntity that = (AcmeCertificationEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy h ? h.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
```

- [ ] **Step 3: 重写 AcmeAsyncLogEntity（任务状态追踪）**

```java
package fan.summer.hmoneta.database.entity.acme;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class AcmeAsyncLogEntity {
    @Id
    private String taskId;
    private String certId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> domains;
    @Enumerated(EnumType.STRING)
    private AcmeTaskType type;
    @Enumerated(EnumType.STRING)
    private AcmeTaskStatus status;
    @Column(length = 4000)
    private String logInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum AcmeTaskType { APPLY, RENEW }

    public enum AcmeTaskStatus { PENDING, RUNNING, SUCCESS, FAILED }
}
```

- [ ] **Step 4: 创建 AcmeGlobalConfigEntity + Repository**

```java
// AcmeGlobalConfigEntity.java
package fan.summer.hmoneta.database.entity.acme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/** 全局 ACME 配置（单行, id 固定为 1）。 */
@Entity
@Data
public class AcmeGlobalConfigEntity {
    @Id
    private Long id;
    private String userEmail;
}
```

```java
// AcmeGlobalConfigRepository.java
package fan.summer.hmoneta.database.repository.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeGlobalConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcmeGlobalConfigRepository extends JpaRepository<AcmeGlobalConfigEntity, Long> {
}
```

- [ ] **Step 5: 改造仓库接口**

```java
// AcmeUserInfoRepository.java
package fan.summer.hmoneta.database.repository.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcmeUserInfoRepository extends JpaRepository<AcmeUserInfoEntity, String> {
    Optional<AcmeUserInfoEntity> findByCaCode(String caCode);
}
```

```java
// AcmeCertificationRepository.java
package fan.summer.hmoneta.database.repository.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcmeCertificationRepository extends JpaRepository<AcmeCertificationEntity, Long> {
    Optional<AcmeCertificationEntity> findByCertId(String certId);
    void deleteByCertId(String certId);
}
```

```java
// AcmeAsyncLogRepository.java
package fan.summer.hmoneta.database.repository.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeAsyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcmeAsyncLogRepository extends JpaRepository<AcmeAsyncLogEntity, String> {
    List<AcmeAsyncLogEntity> findByCertIdOrderByStartTimeDesc(String certId);
}
```

- [ ] **Step 6: 删除 AcmeCerInfoResp（DNS 不再带证书信息）**

```bash
rm src/main/java/fan/summer/hmoneta/controller/acme/dto/resp/AcmeCerInfoResp.java
```

- [ ] **Step 7: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS（此步可能因 AcmeService/DnsService 仍引用旧符号而报错——若报错，临时在 AcmeService/DnsService 注释掉 `queryCertificationInfo`、`AcmeCerInfoResp` 引用即可，下一任务会彻底重写）

> 注意：若编译失败是因为 `DnsResolveUrlResp.acmeCerInfo` 字段，先去 `controller/dns/entity/resp/DnsResolveUrlResp.java` 删掉 `acmeCerInfo` 字段及其 import；`DnsService.java:247-258` 的 join 代码先注释。这些在 Task 9 正式处理。

- [ ] **Step 8: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/database/
git commit -m "feat(acme): 数据模型升级(SAN多域名/任务状态/全局邮箱)"
```

---

## Task 5: CertificateFileManager（文件唯一出口）

**Files:**
- Create: `src/main/java/fan/summer/hmoneta/service/acme/CertificateFileManager.java`
- Test: `src/test/java/fan/summer/hmoneta/service/acme/CertificateFileManagerTest.java`

修 #7（文件散落 + 硬编码）。收编现有 `saveCertificateFiles` / `packCertifications` / `getCertAndKeyByDomain` 逻辑。

- [ ] **Step 1: 写失败测试（用临时目录）**

```java
package fan.summer.hmoneta.service.acme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CertificateFileManagerTest {

    private CertificateFileManager manager;

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        manager = new CertificateFileManager(tempDir.toString());
    }

    @Test
    void should_save_and_read_back_files() throws Exception {
        manager.saveText("cid", "example.com", "KEY-CONTENT", "CRT-CONTENT", "PEM-CONTENT", "FULLCHAIN-CONTENT");

        Map<String, String> content = manager.readContent("cid", "example.com");
        assertEquals("KEY-CONTENT", content.get("key"));
        assertEquals("CRT-CONTENT", content.get("crt"));
        assertEquals("PEM-CONTENT", content.get("pem"));
        assertEquals("FULLCHAIN-CONTENT", content.get("fullchain"));

        byte[] zip = manager.packZip("cid", "example.com");
        assertNotNull(zip);
        assertTrue(zip.length > 0);
    }

    @Test
    void should_delete_directory() throws IOException {
        manager.saveText("cid2", "example.com", "k", "c", "p", "f");
        manager.delete("cid2");
        assertFalse(Files.exists(Path.of(manager.getRootDir(), "cid2")));
    }

    @Test
    void should_throw_when_not_exist() {
        assertThrows(IOException.class, () -> manager.readContent("nope", "example.com"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.service.acme.CertificateFileManagerTest"`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 CertificateFileManager**

```java
package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 证书文件唯一读写出口。目录结构: {rootDir}/{certId}/{primaryDomain}.{ext}
 */
@Log4j2
@Component
public class CertificateFileManager {

    private final String rootDir;

    public CertificateFileManager(@Value("${acme.cert-dir:certs}") String rootDir) {
        this.rootDir = rootDir;
    }

    public String getRootDir() {
        return rootDir;
    }

    /** 由 AcmeOrderService 调用: 直接写入已格式化的 PEM 文本。 */
    public void saveText(String certId, String primaryDomain,
                         String key, String crt, String pem, String fullchain) throws IOException {
        File dir = new File(rootDir, certId);
        if (dir.exists()) {
            deleteRecursively(dir);
        }
        if (!dir.mkdirs()) {
            throw new HMException(AcmeExceptionEnum.CER_CREATE_FOLDER_ERROR);
        }
        Files.writeString(new File(dir, primaryDomain + ".key").toPath(), key);
        Files.writeString(new File(dir, primaryDomain + ".crt").toPath(), crt);
        Files.writeString(new File(dir, primaryDomain + ".pem").toPath(), pem);
        Files.writeString(new File(dir, primaryDomain + ".fullchain.pem").toPath(), fullchain);
        log.info("证书文件已保存: {}/{}", certId, primaryDomain);
    }

    /** 读取 4 种格式 PEM 文本, 供预览。 */
    public Map<String, String> readContent(String certId, String primaryDomain) throws IOException {
        File dir = certDirOrThrow(certId);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", Files.readString(new File(dir, primaryDomain + ".key").toPath()));
        map.put("crt", Files.readString(new File(dir, primaryDomain + ".crt").toPath()));
        map.put("pem", Files.readString(new File(dir, primaryDomain + ".pem").toPath()));
        map.put("fullchain", Files.readString(new File(dir, primaryDomain + ".fullchain.pem").toPath()));
        return map;
    }

    /** 整包 ZIP。 */
    public byte[] packZip(String certId, String primaryDomain) throws IOException {
        File dir = certDirOrThrow(certId);
        List<Path> files = Files.walk(dir.toPath()).filter(Files::isRegularFile).toList();
        if (files.isEmpty()) {
            throw new HMException(AcmeExceptionEnum.CER_NOT_EXIST_ERROR);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Path base = dir.toPath();
            for (Path f : files) {
                zos.putNextEntry(new ZipEntry(base.relativize(f).toString()));
                Files.copy(f, zos);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public void delete(String certId) throws IOException {
        File dir = new File(rootDir, certId);
        if (dir.exists()) {
            deleteRecursively(dir);
            log.info("证书目录已删除: {}", certId);
        }
    }

    private File certDirOrThrow(String certId) throws IOException {
        File dir = new File(rootDir, certId);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new HMException(AcmeExceptionEnum.CER_ERROR_FOLDER_NOT_EXIST);
        }
        return dir;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        file.delete();
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.service.acme.CertificateFileManagerTest"`
Expected: PASS（3 tests）

- [ ] **Step 5: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/service/acme/CertificateFileManager.java src/test/java/fan/summer/hmoneta/service/acme/CertificateFileManagerTest.java
git commit -m "feat(acme): CertificateFileManager 统一文件读写出口"
```

---

## Task 6: AcmeAccountService（CA 账号）

**Files:**
- Create: `src/main/java/fan/summer/hmoneta/service/acme/AcmeAccountService.java`

按 caCode 创建/复用账号。修 #5（"永远取 get(0)"）。

- [ ] **Step 1: 实现 AcmeAccountService**

```java
package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.acme.AcmeCaRegistry;
import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.acme.AcmeGlobalConfigEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeGlobalConfigRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeUserInfoRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeNetworkException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class AcmeAccountService {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final AcmeUserInfoRepository acmeUserInfoRepository;
    private final AcmeGlobalConfigRepository acmeGlobalConfigRepository;

    public AcmeAccountService(AcmeUserInfoRepository acmeUserInfoRepository,
                              AcmeGlobalConfigRepository acmeGlobalConfigRepository) {
        this.acmeUserInfoRepository = acmeUserInfoRepository;
        this.acmeGlobalConfigRepository = acmeGlobalConfigRepository;
    }

    /** 按 caCode 查找账号; 无则用全局邮箱创建。 */
    @Transactional(rollbackFor = Exception.class)
    public AcmeUserInfoEntity findOrCreateAccount(AcmeCaRegistry ca) {
        AcmeUserInfoEntity info = acmeUserInfoRepository.findByCaCode(ca.getCode()).orElseGet(() -> {
            AcmeUserInfoEntity created = new AcmeUserInfoEntity();
            created.setId(UUID.randomUUID().toString());
            created.setCaCode(ca.getCode());
            created.setUserEmail(getGlobalEmail());
            return created;
        });

        boolean needCreate = ObjectUtils.isEmpty(info.getPrivateKey()) || ObjectUtils.isEmpty(info.getPublicKey());
        if (needCreate) {
            KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
            Session session = newSession(ca);
            try {
                Account account = new AccountBuilder()
                        .addContact("mailto:" + info.getUserEmail())
                        .agreeToTermsOfService()
                        .useKeyPair(keyPair)
                        .create(session);
                if (ObjectUtils.isEmpty(account)) {
                    throw new HMException(AcmeExceptionEnum.ACME_ACCOUNT_UPDATE_ERROR);
                }
                info.saveKeyPair(keyPair);
                info = acmeUserInfoRepository.save(info);
                log.info("[ACME] 创建 CA {} 账号成功", ca.getCode());
            } catch (Exception e) {
                log.error("[ACME] 创建 CA {} 账号失败: {}", ca.getCode(), e.getMessage());
                throw new HMException(AcmeExceptionEnum.ACME_ACCOUNT_UPDATE_ERROR);
            }
        }
        return info;
    }

    /** 带重试登录。 */
    public Login login(AcmeCaRegistry ca, AcmeUserInfoEntity info) {
        try {
            KeyPair keyPair = info.generateKeyPair();
            Session session = newSession(ca);
            int attempts = 0;
            while (true) {
                try {
                    return new AccountBuilder().onlyExisting().agreeToTermsOfService()
                            .useKeyPair(keyPair).createLogin(session);
                } catch (AcmeNetworkException e) {
                    attempts++;
                    if (attempts >= MAX_LOGIN_ATTEMPTS) throw e;
                    log.warn("[ACME] 登录重试 {}/{}", attempts, MAX_LOGIN_ATTEMPTS);
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        } catch (Exception e) {
            throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
        }
    }

    private Session newSession(AcmeCaRegistry ca) {
        Session session = new Session(ca.getDirectoryUrl());
        session.networkSettings().setTimeout(java.time.Duration.ofSeconds(120));
        return session;
    }

    private String getGlobalEmail() {
        return acmeGlobalConfigRepository.findById(1L)
                .map(AcmeGlobalConfigEntity::getUserEmail)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.ACME_ACCOUNT_NOT_EXIST_ERROR));
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/service/acme/AcmeAccountService.java
git commit -m "feat(acme): AcmeAccountService 按CA创建/登录账号"
```

---

## Task 7: AcmeOrderService（订单/挑战/签发，事务收窄）

**Files:**
- Create: `src/main/java/fan/summer/hmoneta/service/acme/AcmeOrderService.java`
- Modify: `service/acme/AcmeTaskContext.java`

修 #6（长事务）。把现有 130 行方法拆成短方法，事务只包 DB 写小段。

- [ ] **Step 1: 重写 AcmeTaskContext**

```java
package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.acme.AcmeCaRegistry;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import lombok.Data;

import java.util.List;

@Data
public class AcmeTaskContext {
    private String taskId;
    private String certId;
    private AcmeCaRegistry ca;
    private List<String> domains;       // SAN, 第一个为 CN
    private AcmeUserInfoEntity acmeUserInfoEntity;
    private HmDnsProviderPlugin hmDnsProviderPlugin;
}
```

- [ ] **Step 2: 实现 AcmeOrderService**

```java
package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.util.AcmeDomainResolver;
import lombok.extern.log4j.Log4j2;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ACME 订单/挑战/签发编排。事务只包 DB 写小段。
 */
@Log4j2
@Service
public class AcmeOrderService {

    private static final int MAX_DNS_ATTEMPTS = 10;
    private static final long POLL_INTERVAL_MS = 5000;

    private final AcmeCertificationRepository certRepository;
    private final CertificateFileManager fileManager;

    public AcmeOrderService(AcmeCertificationRepository certRepository,
                            CertificateFileManager fileManager) {
        this.certRepository = certRepository;
        this.fileManager = fileManager;
    }

    /**
     * 执行完整申请流程: 登录 → 下单 → 解析所有授权的 DNS-01 → 签发 → 保存。
     * @param ctx 已含 ca/account/plugin/domains/taskId/certId
     * @return 持久化的证书实体(含 notBefore/notAfter)
     */
    public AcmeCertificationEntity execute(AcmeTaskContext ctx, AcmeAccountService accountService) {
        Login login = accountService.login(ctx.getCa(), ctx.getAcmeUserInfoEntity());
        Order order = placeOrder(login, ctx.getDomains());
        solveAllDns01Challenges(ctx, order);
        CertIssueResult issued = finalizeAndIssue(ctx, order);
        return persistAndSaveFiles(ctx, issued);
    }

    /** finalizeAndIssue 的产物: acme4j 证书 + 本地生成的证书 KeyPair。 */
    public record CertIssueResult(Certificate certificate, KeyPair keyPair) {
    }

    private Order placeOrder(Login login, List<String> domains) {
        Order.Builder b = login.newOrder();
        domains.forEach(b::domain);
        return b.create();
    }

    private void solveAllDns01Challenges(AcmeTaskContext ctx, Order order) {
        Order bound = order.getLogin().bindOrder(order.getLocation());
        for (Authorization auth : bound.getAuthorizations()) {
            Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.class)
                    .orElseThrow(() -> new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED));
            String validationDomain = challenge.getDomain(); // _acme-challenge.<域> 前缀域名
            AcmeDomainResolver.Resolved resolved = AcmeDomainResolver.resolve(authIdentifier(auth));
            String digest = challenge.getDigest();
            log.info("[ACME-Task:{}] 写 TXT {} under {}", ctx.getTaskId(), resolved.txtHost(), resolved.mainDomain());
            boolean ok = ctx.getHmDnsProviderPlugin()
                    .modifyDns(resolved.mainDomain(), resolved.txtHost(), "TXT", digest);
            try {
                if (ok && waitForDnsPropagation(validationDomain, digest)) {
                    challenge.trigger();
                    waitAuthorization(auth);
                    if (auth.getStatus() != Status.VALID) {
                        throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
                    }
                } else {
                    throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
                }
            } finally {
                cleanupTxt(ctx.getHmDnsProviderPlugin(), resolved.mainDomain(), resolved.txtHost());
            }
        }
    }

    /** 从 Authorization 取标识域名。acme4j 的 identifier value 即受验域名。 */
    private String authIdentifier(Authorization auth) {
        return auth.getIdentifier().getDomain();
    }

    private CertIssueResult finalizeAndIssue(AcmeTaskContext ctx, Order order) {
        try {
            KeyPair certKeyPair = KeyPairUtils.createKeyPair(2048);
            order.execute(certKeyPair);
            waitOrder(order);
            if (order.getStatus() != Status.VALID) {
                throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
            }
            Certificate cert = order.getCertificate();
            // 保存文件: 组装 4 种 PEM
            String primary = ctx.getDomains().get(0).replaceFirst("^\\*\\.", "");
            X509Certificate leaf = cert.getCertificate();
            List<X509Certificate> chain = cert.getCertificateChain();
            String keyPem = keyPairToPem(certKeyPair);
            String crtPem = certToPem(leaf);
            StringBuilder pem = new StringBuilder(crtToPem(leaf));
            for (int i = 1; i < chain.size(); i++) {
                pem.append('\n').append(certToPem(chain.get(i)));
            }
            String fullchain = keyPem + "\n" + pem;
            fileManager.saveText(ctx.getCertId(), primary, keyPem, crtPem, pem.toString(), fullchain);
            return new CertIssueResult(cert, certKeyPair);
        } catch (HMException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ACME-Task:{}] 签发失败: {}", ctx.getTaskId(), e.getMessage());
            throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected AcmeCertificationEntity persistAndSaveFiles(AcmeTaskContext ctx, CertIssueResult issued) {
        try {
            X509Certificate leaf = issued.certificate().getCertificate();
            AcmeCertificationEntity entity = certRepository.findByCertId(ctx.getCertId())
                    .orElseGet(() -> {
                        AcmeCertificationEntity e = new AcmeCertificationEntity();
                        e.setCertId(ctx.getCertId());
                        return e;
                    });
            entity.setCaCode(ctx.getCa().getCode());
            entity.setDomains(ctx.getDomains());
            entity.setNotBefore(leaf.getNotBefore());
            entity.setNotAfter(leaf.getNotAfter());
            entity.setCertApplyTime(LocalDateTime.now());
            entity.setProviderName(ctx.getHmDnsProviderPlugin().providerName());
            entity.saveKeyPair(issued.keyPair());
            return certRepository.save(entity);
        } catch (Exception e) {
            log.error("[ACME-Task:{}] 持久化证书失败: {}", ctx.getTaskId(), e.getMessage());
            throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
        }
    }

    private void waitAuthorization(Authorization auth) {
        while (!EnumSet.of(Status.VALID, Status.INVALID).contains(auth.getStatus())) {
            try {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
                auth.fetch();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (AcmeException ae) {
                throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
            }
        }
    }

    private void waitOrder(Order order) {
        try {
            while (!EnumSet.of(Status.VALID, Status.INVALID).contains(order.getStatus())) {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
                order.fetch();
            }
        } catch (Exception e) {
            throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
        }
    }

    private void cleanupTxt(HmDnsProviderPlugin plugin, String mainDomain, String txtHost) {
        try {
            plugin.deleteDns(mainDomain, txtHost, "TXT");
        } catch (Exception e) {
            log.warn("[ACME] 清理 TXT 失败: {}", e.getMessage());
        }
    }

    private boolean waitForDnsPropagation(String domain, String expectedTxt) {
        try {
            Lookup lookup = new Lookup(domain, Type.TXT);
            for (int attempt = 1; attempt <= MAX_DNS_ATTEMPTS; attempt++) {
                lookup.run();
                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    for (org.xbill.DNS.Record r : lookup.getAnswers()) {
                        if (r instanceof TXTRecord txt) {
                            for (String s : txt.getStrings()) {
                                if (s.equals(expectedTxt)) return true;
                            }
                        }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(10000);
            }
        } catch (Exception e) {
            log.warn("[ACME] DNS 传播校验异常: {}", e.getMessage());
        }
        return false;
    }

    private String certToPem(X509Certificate cert) throws java.security.cert.CertificateEncodingException {
        String b64 = Base64.getEncoder().encodeToString(cert.getEncoded());
        StringBuilder sb = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < b64.length(); i += 64) {
            sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
        }
        sb.append("-----END CERTIFICATE-----\n");
        return sb.toString();
    }

    private String keyPairToPem(KeyPair keyPair) {
        java.io.StringWriter sw = new java.io.StringWriter();
        try {
            KeyPairUtils.writeKeyPair(keyPair, sw);
        } catch (Exception e) {
            throw new HMException(AcmeExceptionEnum.CERT_FILE_ERROR);
        }
        return sw.toString();
    }
}
```

> 实现说明：`waitAuthorization` / `waitOrder` 已是最终实现（无占位重载）；`solveAllDns01Challenges` 用 `waitAuthorization(auth)`，`finalizeAndIssue` 用 `waitOrder(order)`；`certKeyPair` 通过 `CertIssueResult` 显式透传给 `persistAndSaveFiles`，无字段 holder。

- [ ] **Step 3: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/service/acme/AcmeOrderService.java src/main/java/fan/summer/hmoneta/service/acme/AcmeTaskContext.java
git commit -m "feat(acme): AcmeOrderService 订单/挑战/签发编排(事务收窄+多授权DNS-01)"
```

---

## Task 8: CertificateService 门面（锁 + 任务日志 + @Async）

**Files:**
- Modify: `service/acme/AcmeService.java`（保留类名 AcmeService 作门面，减小 diff）
- Delete: `service/acme/AcmeServiceComponent.java`
- Test: `src/test/java/fan/summer/hmoneta/service/acme/CertificateServiceConcurrencyTest.java`

修 #2、#3、#8。这是编排核心。

- [ ] **Step 1: 写并发锁失败测试**

```java
package fan.summer.hmoneta.service.acme;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CertificateServiceConcurrencyTest {

    @Test
    void second_lock_for_same_certId_should_fail_fast() throws Exception {
        // 7 个依赖全传 null, 测试只验锁逻辑, 不依赖注入字段(self 是字段注入, 不进构造器)
        AcmeService svc = new AcmeService(null, null, null, null, null, null, null);
        Method tryLock = AcmeService.class.getDeclaredMethod("tryLock", String.class);
        tryLock.setAccessible(true);

        assertTrue((boolean) tryLock.invoke(svc, "cert-1"));   // 第一次拿到
        assertFalse((boolean) tryLock.invoke(svc, "cert-1"));  // 同 certId 快速失败

        Method unlock = AcmeService.class.getDeclaredMethod("unlock", String.class);
        unlock.setAccessible(true);
        unlock.invoke(svc, "cert-1");
        assertTrue((boolean) tryLock.invoke(svc, "cert-1"));   // 释放后可再拿
        unlock.invoke(svc, "cert-1");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.service.acme.CertificateServiceConcurrencyTest"`
Expected: FAIL（类/方法不存在）

- [ ] **Step 3: 重写 AcmeService 为门面（保留类名 AcmeService）**

> 实现要点（已在代码中落实，无需"先写后改"）：① 账号逻辑不在本类，不注入 `AcmeUserInfoRepository`；② `DnsResolveGroupEntity.providerId` 指向 `DnsProviderEntity.id`，需经 `DnsProviderRepository` 取 `providerName` 再交给 `PluginService`；③ `@Async` 必须经自注入代理 `self` 调用，同类 `this` 直接调用代理不生效。

```java
package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.acme.AcmeCaRegistry;
import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.acme.AcmeAsyncLogEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeAsyncLogRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveGroupRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.service.plugin.PluginService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 证书服务门面: 并发锁 + 任务日志 + @Async 编排。
 * 类名保留 AcmeService 以减小调用方 diff。
 */
@Log4j2
@Service
public class AcmeService {

    private final AcmeAccountService accountService;
    private final AcmeOrderService orderService;
    private final AcmeCertificationRepository certRepository;
    private final AcmeAsyncLogRepository logRepository;
    private final DnsResolveGroupRepository dnsGroupRepository;
    private final DnsProviderRepository dnsProviderRepository;
    private final PluginService pluginService;

    /** 自注入代理, 使 runAsync/runRenewAsync 的 @Async 生效(@Lazy 打破启动期循环依赖)。 */
    @Autowired
    @Lazy
    private AcmeService self;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public AcmeService(AcmeAccountService accountService,
                       AcmeOrderService orderService,
                       AcmeCertificationRepository certRepository,
                       AcmeAsyncLogRepository logRepository,
                       DnsResolveGroupRepository dnsGroupRepository,
                       DnsProviderRepository dnsProviderRepository,
                       PluginService pluginService) {
        this.accountService = accountService;
        this.orderService = orderService;
        this.certRepository = certRepository;
        this.logRepository = logRepository;
        this.dnsGroupRepository = dnsGroupRepository;
        this.dnsProviderRepository = dnsProviderRepository;
        this.pluginService = pluginService;
    }

    /** 申请证书(新建)。返回 taskId。 */
    public String applyCertificate(CertApplyContext req) {
        String taskId = UUID.randomUUID().toString();
        String certId = UUID.randomUUID().toString();
        startLog(taskId, certId, req.domains(), AcmeAsyncLogEntity.AcmeTaskType.APPLY);
        if (!tryLock(certId)) {
            failLog(taskId, AcmeExceptionEnum.CERT_TASK_RUNNING.getMessage());
            return taskId;
        }
        AcmeCertificationEntity placeholder = new AcmeCertificationEntity();
        placeholder.setCertId(certId);
        placeholder.setName(req.name());
        certRepository.save(placeholder);
        self.runAsync(taskId, certId, req);
        return taskId;
    }

    /** 续期(已有 certId)。taskId 由调用方传入(修 #3)。 */
    public void renewCertificate(String certId, String taskId) {
        AcmeCertificationEntity cert = certRepository.findByCertId(certId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CERT_NOT_FOUND));
        startLog(taskId, certId, cert.getDomains(), AcmeAsyncLogEntity.AcmeTaskType.RENEW);
        if (!tryLock(certId)) {
            failLog(taskId, AcmeExceptionEnum.CERT_TASK_RUNNING.getMessage());
            return;
        }
        AcmeCaRegistry ca = AcmeCaRegistry.getByCode(cert.getCaCode());
        self.runRenewAsync(taskId, certId, ca, cert.getDomains(), cert.getDnsGroupId(), cert.getName());
    }

    @Async
    public void runAsync(String taskId, String certId, CertApplyContext req) {
        AcmeCaRegistry ca = AcmeCaRegistry.getByCode(req.caCode());
        executeAsync(taskId, certId, ca, req.domains(), req.dnsGroupId(), req.name(), "申请成功", "申请失败");
    }

    @Async
    public void runRenewAsync(String taskId, String certId, AcmeCaRegistry ca,
                              List<String> domains, String dnsGroupId, String name) {
        executeAsync(taskId, certId, ca, domains, dnsGroupId, name, "续期成功", "续期失败");
    }

    private void executeAsync(String taskId, String certId, AcmeCaRegistry ca, List<String> domains,
                              String dnsGroupId, String name, String okMsg, String failTag) {
        try {
            runningLog(taskId);
            AcmeUserInfoEntity account = accountService.findOrCreateAccount(ca);
            HmDnsProviderPlugin plugin = buildPlugin(dnsGroupId);
            AcmeTaskContext ctx = new AcmeTaskContext();
            ctx.setTaskId(taskId);
            ctx.setCertId(certId);
            ctx.setCa(ca);
            ctx.setDomains(domains);
            ctx.setAcmeUserInfoEntity(account);
            ctx.setHmDnsProviderPlugin(plugin);
            AcmeCertificationEntity saved = orderService.execute(ctx, accountService);
            saved.setCertId(certId);
            saved.setName(name);
            saved.setDnsGroupId(dnsGroupId);
            certRepository.save(saved);
            successLog(taskId, okMsg);
        } catch (Exception e) {
            log.error("[ACME-Task:{}] {}: {}", taskId, failTag, e.getMessage());
            failLog(taskId, failTag + ": " + e.getMessage());
        } finally {
            unlock(certId);
        }
    }

    private HmDnsProviderPlugin buildPlugin(String dnsGroupId) {
        DnsResolveGroupEntity group = dnsGroupRepository.findById(dnsGroupId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.DNS_SERVICE_NOT_FOUND_PROVIDER_ERROR));
        String providerName = dnsProviderRepository.findById(group.getProviderId())
                .map(DnsProviderEntity::getProviderName)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.DNS_SERVICE_NOT_FOUND_PROVIDER_ERROR));
        HmDnsProviderPlugin plugin = pluginService.getDnsProvider(providerName);
        plugin.authenticate(group.getCredentials());
        return plugin;
    }

    boolean tryLock(String key) {
        return locks.putIfAbsent(key, new Object()) == null;
    }

    void unlock(String key) {
        locks.remove(key);
    }

    private void startLog(String taskId, String certId, List<String> domains, AcmeAsyncLogEntity.AcmeTaskType type) {
        AcmeAsyncLogEntity e = new AcmeAsyncLogEntity();
        e.setTaskId(taskId);
        e.setCertId(certId);
        e.setDomains(domains);
        e.setType(type);
        e.setStatus(AcmeAsyncLogEntity.AcmeTaskStatus.PENDING);
        e.setStartTime(LocalDateTime.now());
        logRepository.save(e);
    }

    private void runningLog(String taskId) {
        updateStatus(taskId, AcmeAsyncLogEntity.AcmeTaskStatus.RUNNING, null);
    }

    private void successLog(String taskId, String info) {
        updateStatus(taskId, AcmeAsyncLogEntity.AcmeTaskStatus.SUCCESS, info);
    }

    private void failLog(String taskId, String info) {
        updateStatus(taskId, AcmeAsyncLogEntity.AcmeTaskStatus.FAILED, info);
    }

    private void updateStatus(String taskId, AcmeAsyncLogEntity.AcmeTaskStatus status, String info) {
        logRepository.findById(taskId).ifPresent(e -> {
            e.setStatus(status);
            if (info != null) e.setLogInfo(info);
            if (status == AcmeAsyncLogEntity.AcmeTaskStatus.SUCCESS || status == AcmeAsyncLogEntity.AcmeTaskStatus.FAILED) {
                e.setEndTime(LocalDateTime.now());
            }
            logRepository.save(e);
        });
    }

    /** 申请请求上下文(内部 DTO, 避免与 controller DTO 耦合)。 */
    public record CertApplyContext(String name, String caCode, List<String> domains, String dnsGroupId) {
    }
}
```

> 测试里的 `new AcmeService(null, null, null, null, null, null, null)` 对应上面 7 个构造参数（不是 8 个——`self` 是字段注入，不进构造器）。请把 Step 1 测试中的 `8 个 null` 改为 **7 个 null**：`new AcmeService(null, null, null, null, null, null, null)`。

- [ ] **Step 4: 删除旧 AcmeServiceComponent**

```bash
rm src/main/java/fan/summer/hmoneta/service/acme/AcmeServiceComponent.java
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行并发锁测试**

Run: `mvn -q test -Dtest="fan.summer.hmoneta.service.acme.CertificateServiceConcurrencyTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/fan/summer/hmoneta/service/acme/ src/test/java/fan/summer/hmoneta/service/acme/CertificateServiceConcurrencyTest.java
git commit -m "feat(acme): AcmeService 门面(进程内锁+任务日志落库+@Async自注入)"
```

---

## Task 9: AcmeController 重写 + 全局邮箱 + 查询服务

**Files:**
- Modify: `controller/acme/AcmeController.java`
- Modify: `controller/acme/dto/AcmeUserReq.java` → 重命名为 `EmailReq.java`（或就地改造）
- Create: `controller/acme/dto/req/CertApplyReq.java`
- Create: `controller/acme/dto/resp/AcmeCaResp.java`
- Create: `controller/acme/dto/resp/CertInfoResp.java`
- Create: `controller/acme/dto/resp/CertContentResp.java`
- Create: `controller/acme/dto/resp/AcmeTaskStatusResp.java`

- [ ] **Step 1: 创建 DTO**

```java
// dto/req/CertApplyReq.java
package fan.summer.hmoneta.controller.acme.dto.req;

import java.util.List;

public record CertApplyReq(String name, String caCode, List<String> domains, String dnsGroupId) {
}
```

```java
// dto/AcmeUserReq.java 改为邮箱请求(保留文件名减少 diff, 或重命名 EmailReq)
package fan.summer.hmoneta.controller.acme.dto;

public record AcmeUserReq(String userEmail) {
}
```

```java
// dto/resp/AcmeCaResp.java
package fan.summer.hmoneta.controller.acme.dto.resp;

public record AcmeCaResp(String code, String displayName, boolean staging) {
}
```

```java
// dto/resp/CertInfoResp.java
package fan.summer.hmoneta.controller.acme.dto.resp;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public record CertInfoResp(String certId, String name, String caCode, List<String> domains,
                           LocalDateTime certApplyTime, Date notBefore, Date notAfter) {
}
```

```java
// dto/resp/CertContentResp.java
package fan.summer.hmoneta.controller.acme.dto.resp;

public record CertContentResp(String key, String crt, String pem, String fullchain) {
}
```

```java
// dto/resp/AcmeTaskStatusResp.java
package fan.summer.hmoneta.controller.acme.dto.resp;

import java.util.List;

public record AcmeTaskStatusResp(String taskId, String certId, List<String> domains,
                                 String type, String status, String logInfo) {
}
```

- [ ] **Step 2: 重写 AcmeController**

```java
package fan.summer.hmoneta.controller.acme;

import fan.summer.hmoneta.common.enums.acme.AcmeCaRegistry;
import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.acme.dto.AcmeUserReq;
import fan.summer.hmoneta.controller.acme.dto.req.CertApplyReq;
import fan.summer.hmoneta.controller.acme.dto.resp.AcmeCaResp;
import fan.summer.hmoneta.controller.acme.dto.resp.AcmeTaskStatusResp;
import fan.summer.hmoneta.controller.acme.dto.resp.CertContentResp;
import fan.summer.hmoneta.controller.acme.dto.resp.CertInfoResp;
import fan.summer.hmoneta.database.entity.acme.AcmeAsyncLogEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeGlobalConfigEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeAsyncLogRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeGlobalConfigRepository;
import fan.summer.hmoneta.service.acme.AcmeService;
import fan.summer.hmoneta.service.acme.CertificateFileManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hm/acme")
public class AcmeController {

    private final AcmeService acmeService;
    private final CertificateFileManager fileManager;
    private final AcmeCertificationRepository certRepository;
    private final AcmeAsyncLogRepository logRepository;
    private final AcmeGlobalConfigRepository globalConfigRepository;

    public AcmeController(AcmeService acmeService, CertificateFileManager fileManager,
                          AcmeCertificationRepository certRepository,
                          AcmeAsyncLogRepository logRepository,
                          AcmeGlobalConfigRepository globalConfigRepository) {
        this.acmeService = acmeService;
        this.fileManager = fileManager;
        this.certRepository = certRepository;
        this.logRepository = logRepository;
        this.globalConfigRepository = globalConfigRepository;
    }

    @GetMapping("/cas")
    public List<AcmeCaResp> listCas() {
        return AcmeCaRegistry.list().stream()
                .map(c -> new AcmeCaResp(c.getCode(), c.getDisplayName(), c.isStaging()))
                .toList();
    }

    @GetMapping("/list")
    public List<CertInfoResp> listCerts() {
        return certRepository.findAll().stream()
                .map(e -> new CertInfoResp(e.getCertId(), e.getName(), e.getCaCode(), e.getDomains(),
                        e.getCertApplyTime(), e.getNotBefore(), e.getNotAfter()))
                .toList();
    }

    @GetMapping("/{certId}")
    public CertInfoResp getCert(@PathVariable String certId) {
        AcmeCertificationEntity e = certRepository.findByCertId(certId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CERT_NOT_FOUND));
        return new CertInfoResp(e.getCertId(), e.getName(), e.getCaCode(), e.getDomains(),
                e.getCertApplyTime(), e.getNotBefore(), e.getNotAfter());
    }

    @PostMapping("/apply")
    public Map<String, String> apply(@RequestBody CertApplyReq req) {
        if (req.domains() == null || req.domains().isEmpty()) {
            throw new HMException(AcmeExceptionEnum.ACME_APPLY_FAILED);
        }
        String taskId = acmeService.applyCertificate(
                new AcmeService.CertApplyContext(req.name(), req.caCode(), req.domains(), req.dnsGroupId()));
        return Map.of("taskId", taskId);
    }

    @PostMapping("/renew/{certId}")
    public Map<String, String> renew(@PathVariable String certId) {
        String taskId = java.util.UUID.randomUUID().toString();
        acmeService.renewCertificate(certId, taskId);
        return Map.of("taskId", taskId);
    }

    @DeleteMapping("/{certId}")
    public ResponseEntity<Void> delete(@PathVariable String certId) {
        AcmeCertificationEntity e = certRepository.findByCertId(certId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CERT_NOT_FOUND));
        try {
            fileManager.delete(certId);
        } catch (IOException ex) {
            throw new HMException(AcmeExceptionEnum.CERT_FILE_ERROR);
        }
        certRepository.delete(e);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/task/{taskId}")
    public AcmeTaskStatusResp taskStatus(@PathVariable String taskId) {
        AcmeAsyncLogEntity e = logRepository.findById(taskId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CER_ERROR_LOG_NOT_FIND));
        return new AcmeTaskStatusResp(e.getTaskId(), e.getCertId(), e.getDomains(),
                e.getType() == null ? null : e.getType().name(),
                e.getStatus() == null ? null : e.getStatus().name(),
                e.getLogInfo());
    }

    @GetMapping("/{certId}/content")
    public CertContentResp content(@PathVariable String certId) {
        AcmeCertificationEntity e = certRepository.findByCertId(certId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CERT_NOT_FOUND));
        String primary = e.getDomains().get(0).replaceFirst("^\\*\\.", "");
        try {
            Map<String, String> c = fileManager.readContent(certId, primary);
            return new CertContentResp(c.get("key"), c.get("crt"), c.get("pem"), c.get("fullchain"));
        } catch (IOException ex) {
            throw new HMException(AcmeExceptionEnum.CERT_FILE_ERROR);
        }
    }

    @GetMapping("/{certId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String certId) {
        AcmeCertificationEntity e = certRepository.findByCertId(certId)
                .orElseThrow(() -> new HMException(AcmeExceptionEnum.CERT_NOT_FOUND));
        String primary = e.getDomains().get(0).replaceFirst("^\\*\\.", "");
        try {
            byte[] zip = fileManager.packZip(certId, primary);
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            h.setContentDispositionFormData(certId + "_certificate.zip", certId + "_certificate.zip");
            h.setContentLength(zip.length);
            return ResponseEntity.ok().headers(h).body(zip);
        } catch (IOException ex) {
            throw new HMException(AcmeExceptionEnum.CERT_FILE_ERROR);
        }
    }

    @GetMapping("/email")
    public AcmeUserReq getEmail() {
        return globalConfigRepository.findById(1L)
                .map(c -> new AcmeUserReq(c.getUserEmail()))
                .orElse(new AcmeUserReq(null));
    }

    @PostMapping("/email")
    public ResponseEntity<Void> saveEmail(@RequestBody AcmeUserReq req) {
        AcmeGlobalConfigEntity entity = globalConfigRepository.findById(1L)
                .orElseGet(() -> {
                    AcmeGlobalConfigEntity g = new AcmeGlobalConfigEntity();
                    g.setId(1L);
                    return g;
                });
        entity.setUserEmail(req.userEmail());
        globalConfigRepository.save(entity);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS（DnsService 对旧 AcmeService 方法的引用在 Task 10 处理；若此处报错，先注释 DnsService 里的引用）

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/fan/summer/hmoneta/controller/acme/
git commit -m "feat(acme): 重写 AcmeController(11 个 RESTful 端点 + 全局邮箱)"
```

---

## Task 10: DnsService 解耦（移除证书 join）

**Files:**
- Modify: `service/dns/DnsService.java:5`（删 import）、`:236` javadoc、`:247-258`（删 join）
- Modify: `controller/dns/entity/resp/DnsResolveUrlResp.java`（删 `acmeCerInfo`）

- [ ] **Step 1: 删除 DnsResolveUrlResp 的 acmeCerInfo 字段**

打开 `controller/dns/entity/resp/DnsResolveUrlResp.java`，删除：
- `import fan.summer.hmoneta.controller.acme.dto.resp.AcmeCerInfoResp;`
- `private AcmeCerInfoResp acmeCerInfo;` 字段及其 getter/setter（如有）。

- [ ] **Step 2: 删除 DnsService 里的证书 join**

在 `service/dns/DnsService.java`：
- 删 `import fan.summer.hmoneta.controller.acme.dto.resp.AcmeCerInfoResp;`（第 5 行）
- 删 `import fan.summer.hmoneta.service.acme.AcmeService;`（若仅此处用）及 `acmeService` 字段（若 DnsService 不再用 AcmeService）
- 把 `queryAllDnsResolve()` 中 `allUrl.forEach(url -> {...})` 里查证书并 `urlResp.setAcmeCerInfo(...)` 的整段（`:247-258`）简化为：

```java
allUrl.forEach(url -> {
    DnsResolveUrlResp urlResp = new DnsResolveUrlResp();
    BeanUtils.copyProperties(url, urlResp);
    urlResps.add(urlResp);
});
```

- 删 `@see ...AcmeService#queryCertificationInfo` 的 javadoc 行（`:236`）。

- [ ] **Step 3: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/service/dns/DnsService.java src/main/java/fan/summer/hmoneta/controller/dns/entity/resp/DnsResolveUrlResp.java
git commit -m "refactor(dns): 移除 DNS 与证书的耦合(join), 证书独立成页"
```

---

## Task 11: AcmeUpdateTask 改逐证书扫描

**Files:**
- Modify: `task/acme/AcmeUpdateTask.java`

修 #3（续期传 taskId）+ 扫描对象改为证书。

- [ ] **Step 1: 重写 AcmeUpdateTask**

```java
package fan.summer.hmoneta.task.acme;

import fan.summer.hmoneta.common.annotation.ScheduledTask;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.service.acme.AcmeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ScheduledTask(name = "acmeUpdateTask", description = "ACME 证书续期任务",
        defaultCron = "0 0 0 * * ?", methodName = "acmeUpdater")
public class AcmeUpdateTask {

    private final AcmeCertificationRepository certRepository;
    private final AcmeService acmeService;

    @Value("${acme.renewal.threshold-days:30}")
    private int renewalThresholdDays;

    public AcmeUpdateTask(AcmeCertificationRepository certRepository, AcmeService acmeService) {
        this.certRepository = certRepository;
        this.acmeService = acmeService;
    }

    public void acmeUpdater() {
        String batchId = UUID.randomUUID().toString();
        List<AcmeCertificationEntity> all = certRepository.findAll();
        log.info("[ACME-Updater:{}] 开始扫描, 共 {} 张证书", batchId, all.size());

        for (AcmeCertificationEntity cert : all) {
            Date notAfter = cert.getNotAfter();
            if (notAfter == null) {
                log.warn("[ACME-Updater:{}] 证书 {} 过期时间为空, 跳过", batchId, cert.getCertId());
                continue;
            }
            long days = ChronoUnit.DAYS.between(
                    new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            if (days <= renewalThresholdDays) {
                String taskId = UUID.randomUUID().toString();
                log.info("[ACME-Updater:{}] 证书 {} 临期(剩余 {} 天), 触发续期 taskId={}",
                        batchId, cert.getCertId(), days, taskId);
                try {
                    acmeService.renewCertificate(cert.getCertId(), taskId);
                } catch (Exception e) {
                    log.error("[ACME-Updater:{}] 证书 {} 续期派发失败: {}", batchId, cert.getCertId(), e.getMessage());
                }
            }
        }
        log.info("[ACME-Updater:{}] 扫描完成", batchId);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/fan/summer/hmoneta/task/acme/AcmeUpdateTask.java
git commit -m "feat(acme): AcmeUpdateTask 改为逐证书扫描并传 taskId 续期"
```

---

## Task 12: 配置 + 全量编译 + 后端测试

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`

- [ ] **Step 1: 修 application.yml（生产 CA url + cert-dir）**

把 `acme:` 段改为：

```yaml
acme:
  url: https://acme-v02.api.letsencrypt.org/directory
  cert-dir: ${ACME_CERT_DIR:certs}
  renewal:
    threshold-days: 30
```

- [ ] **Step 2: 确认 application-dev.yml 保留 staging**

确认 `application-dev.yml` 里 `acme.url` 仍是 `https://acme-staging-v02.api.letsencrypt.org/directory`（dev 默认 staging）。无需改动。

- [ ] **Step 3: 全量编译**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行全部后端测试**

Run: `mvn -q test`
Expected: PASS（含新增 4 个测试类 + 既有 unifi/dns/user 测试）

> 若 `HMonetaApplicationTests`（上下文加载测试）因缺 DB 环境失败，属环境问题，不阻塞；重点确认 ACME/dns/user 单测通过。

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "chore(acme): 默认 CA url 改生产, 新增可配置 cert-dir"
```

---

## Task 13: 前端 certManager.vue 新页面

**Files:**
- Create: `HMfront/hm-front/src/pages/certManager.vue`
- Modify: `HMfront/hm-front/src/layouts/default.vue`

- [ ] **Step 1: 创建 certManager.vue**

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { http } from '@/common/request.js'
import { userNotificationStore } from '@/stores/app.js'
import PageHeader from '@/components/PageHeader.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const notificationStore = userNotificationStore()
const certs = ref([])
const cas = ref([])
const dnsGroups = ref([])
const loading = ref(false)
const applyDialog = ref(false)
const contentDialog = ref(false)
const contentData = ref({})
const confirmDelete = ref(false)
const pendingDeleteId = ref(null)

const applyForm = ref({ name: '', caCode: '', domains: [], dnsGroupId: '' })
const domainInput = ref('')

const headers = [
  { title: '证书名', key: 'name' },
  { title: '域名', key: 'domains' },
  { title: 'CA', key: 'caCode' },
  { title: '到期', key: 'notAfter' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'actions', sortable: false },
]

const isExpiring = (notAfter) => {
  if (!notAfter) return false
  return new Date(notAfter) <= new Date(Date.now() + 30 * 86400000)
}
const statusOf = (c) => {
  if (!c.notAfter) return { text: '未知', color: 'grey' }
  if (new Date(c.notAfter) < new Date()) return { text: '已过期', color: 'red' }
  if (isExpiring(c.notAfter)) return { text: '临期', color: 'orange' }
  return { text: '有效', color: 'green' }
}

const refresh = async () => {
  loading.value = true
  try {
    const [list, caList, groupList] = await Promise.all([
      http.get('/acme/list'),
      http.get('/acme/cas'),
      http.get('/dns/resolve_info'),
    ])
    certs.value = list || []
    cas.value = caList || []
    dnsGroups.value = flattenGroups(groupList)
  } finally {
    loading.value = false
  }
}
const flattenGroups = (resp) => {
  const out = []
  ;(resp || []).forEach(g => out.push({ id: g.groupId, name: g.groupName }))
  return out
}

const addDomain = () => {
  const d = domainInput.value.trim()
  if (d && !applyForm.value.domains.includes(d)) {
    applyForm.value.domains.push(d)
  }
  domainInput.value = ''
}
const removeDomain = (d) => {
  applyForm.value.domains = applyForm.value.domains.filter(x => x !== d)
}

const submitApply = async () => {
  if (!applyForm.value.domains.length || !applyForm.value.caCode || !applyForm.value.dnsGroupId) {
    notificationStore.showError('请填写完整')
    return
  }
  try {
    const { taskId } = await http.post('/acme/apply', applyForm.value)
    applyDialog.value = false
    notificationStore.showSuccess('申请已提交, 任务号 ' + taskId)
    pollTask(taskId)
  } catch (e) { /* request.js 已弹错 */ }
}

const renew = async (certId) => {
  const { taskId } = await http.post('/acme/renew/' + certId)
  notificationStore.showSuccess('续期已提交, 任务号 ' + taskId)
  pollTask(taskId)
}

const download = async (certId) => {
  const blob = await http.get('/acme/' + certId + '/download', null, { responseType: 'blob' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.setAttribute('download', certId + '_certificate.zip')
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

const previewContent = async (certId) => {
  contentData.value = await http.get('/acme/' + certId + '/content')
  contentDialog.value = true
}

const askDelete = (certId) => { pendingDeleteId.value = certId; confirmDelete.value = true }
const doDelete = async () => {
  confirmDelete.value = false
  await http.delete('/acme/' + pendingDeleteId.value)
  notificationStore.showSuccess('证书已删除')
  await refresh()
}

const pollTask = async (taskId) => {
  const stop = ['SUCCESS', 'FAILED']
  const tick = async () => {
    const s = await http.get('/acme/task/' + taskId)
    if (s.status === 'SUCCESS') {
      notificationStore.showSuccess('任务完成')
      await refresh()
    } else if (s.status === 'FAILED') {
      notificationStore.showError('任务失败: ' + (s.logInfo || ''))
      await refresh()
    } else {
      setTimeout(tick, 2000)
    }
  }
  setTimeout(tick, 2000)
}

onMounted(refresh)
</script>

<template>
  <PageHeader icon="mdi-certificate-outline" title="证书管理" subtitle="ACME 证书申请 / 续期 / 下载" />
  <v-card class="pa-4">
    <div class="d-flex justify-end mb-4">
      <v-btn color="primary" @click="applyDialog = true">
        <v-icon start>mdi-plus</v-icon>申请证书
      </v-btn>
    </div>
    <v-data-table :headers="headers" :items="certs" :loading="loading" item-value="certId">
      <template #item.domains="{ item }">
        <v-chip v-for="d in item.domains" :key="d" size="x-small" class="ma-1">{{ d }}</v-chip>
      </template>
      <template #item.status="{ item }">
        <v-chip :color="statusOf(item).color" size="small">{{ statusOf(item).text }}</v-chip>
      </template>
      <template #item.actions="{ item }">
        <v-icon @click="renew(item.certId)" title="续期">mdi-refresh</v-icon>
        <v-icon @click="download(item.certId)" title="下载ZIP">mdi-download</v-icon>
        <v-icon @click="previewContent(item.certId)" title="预览">mdi-eye</v-icon>
        <v-icon @click="askDelete(item.certId)" title="删除" color="red">mdi-delete</v-icon>
      </template>
    </v-data-table>
  </v-card>

  <!-- 申请对话框 -->
  <v-dialog v-model="applyDialog" max-width="600">
    <v-card class="pa-4">
      <h3 class="text-h6 mb-4">申请证书</h3>
      <v-text-field label="证书名" v-model="applyForm.name" />
      <v-select label="CA" :items="cas" item-title="displayName" item-value="code" v-model="applyForm.caCode" />
      <v-select label="DNS 解析组" :items="dnsGroups" item-title="name" item-value="id" v-model="applyForm.dnsGroupId" />
      <div class="d-flex align-center">
        <v-text-field label="域名 (回车添加, 支持 *.x)" v-model="domainInput"
                      @keydown.enter.prevent="addDomain" />
        <v-btn @click="addDomain" class="ml-2">添加</v-btn>
      </div>
      <v-chip v-for="d in applyForm.domains" :key="d" closable @click:close="removeDomain(d)" class="ma-1">{{ d }}</v-chip>
      <div class="d-flex justify-end mt-4">
        <v-btn variant="text" @click="applyDialog = false">取消</v-btn>
        <v-btn color="primary" @click="submitApply">申请</v-btn>
      </div>
    </v-card>
  </v-dialog>

  <!-- 预览对话框 (tab 绑定 4 种 PEM) -->
  <v-dialog v-model="contentDialog" max-width="800">
    <v-card class="pa-4">
      <h3 class="text-h6 mb-4">证书内容</h3>
      <v-tabs v-model="contentTab">
        <v-tab value="crt">证书</v-tab>
        <v-tab value="key">私钥</v-tab>
        <v-tab value="pem">链</v-tab>
        <v-tab value="fullchain">完整</v-tab>
      </v-tabs>
      <v-tabs-window v-model="contentTab">
        <v-tabs-window-item v-for="k in ['crt','key','pem','fullchain']" :key="k" :value="k">
          <pre class="cert-pre">{{ contentData[k] }}</pre>
        </v-tabs-window-item>
      </v-tabs-window>
      <div class="d-flex justify-end mt-2">
        <v-btn variant="text" @click="contentDialog = false">关闭</v-btn>
      </div>
    </v-card>
  </v-dialog>

  <ConfirmDialog v-model="confirmDelete" title="删除证书" message="确认删除该证书？将同时删除本地证书文件。"
                 @confirm="doDelete" />
</template>

<style scoped>
.cert-pre {
  background: rgba(0, 0, 0, 0.2);
  padding: 12px;
  border-radius: 8px;
  overflow: auto;
  max-height: 400px;
  font-size: 12px;
}
</style>
```

> 注：`<script setup>` 里需有 `const contentTab = ref('crt')`（已含在顶部 import 段，补上声明即可）。

- [ ] **Step 2: 导航加菜单项**

在 `layouts/default.vue` 的菜单列表中加一项（与现有 dns/network 等同级）：

```vue
<v-list-item to="/certManager" prepend-icon="mdi-certificate-outline" title="证书管理" />
```

- [ ] **Step 3: Lint + 构建**

Run:
```bash
cd HMfront/hm-front && yarn lint && yarn build
```
Expected: 无 lint 报错，build 成功

- [ ] **Step 4: Commit**

```bash
cd ../..
git add HMfront/hm-front/src/pages/certManager.vue HMfront/hm-front/src/layouts/default.vue
git commit -m "feat(front): 新增证书管理页 certManager.vue"
```

---

## Task 14: 前端 dns.vue 移除证书 UI + setting.vue 邮箱

**Files:**
- Modify: `HMfront/hm-front/src/pages/dns.vue`
- Modify: `HMfront/hm-front/src/pages/setting.vue`

- [ ] **Step 1: dns.vue 删除证书相关代码**

在 `HMfront/hm-front/src/pages/dns.vue`：
- 删除 `isCertificateExpiring`、`applyCertificate`、`getCertificateClass`、`getPopText` 四个函数（约 `:148-205`）。
- 删除模板里调用这些函数的按钮/tooltip/徽标（搜索 `acmeCerInfo`、`cert-valid`、`cert-expired`、`cert-unavailable`、`applyCertificate`，全删）。
- 删除 `<style>` 里 `.cert-valid / .cert-expired / .cert-unavailable` 规则。

- [ ] **Step 2: setting.vue 邮箱改新端点 + 回填**

在 `HMfront/hm-front/src/pages/setting.vue`：

`<script setup>` 的 ACME 段改为：

```js
const acmeInfo = reactive({ userEmail: null })
const acmeLoading = ref(false)
const queryAcmeEmail = async () => {
  try {
    const resp = await http.get('/acme/email')
    acmeInfo.userEmail = resp.userEmail
  } catch (_) {}
}
const onSubmit = async () => {
  acmeLoading.value = true
  try {
    await http.post('/acme/email', acmeInfo)
    userNotificationStore().showSuccess('设置保存成功')
  } finally {
    acmeLoading.value = false
  }
}
```

`onMounted` 里追加 `await queryAcmeEmail();`。卡片说明文案改为：

```
配置 Let's Encrypt 账号邮箱，该邮箱用于所有 CA 账号注册（见「证书管理」页申请证书）
```

- [ ] **Step 3: Lint + 构建**

Run:
```bash
cd HMfront/hm-front && yarn lint && yarn build
```
Expected: 成功

- [ ] **Step 4: Commit**

```bash
cd ../..
git add HMfront/hm-front/src/pages/dns.vue HMfront/hm-front/src/pages/setting.vue
git commit -m "refactor(front): dns.vue 移除证书UI, setting.vue 邮箱改打/hm/acme/email并回填"
```

---

## Task 15: 收尾验证（全量构建 + 手测清单）

- [ ] **Step 1: 后端全量构建**

Run: `mvn -q clean package`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端全量构建**

Run:
```bash
cd HMfront/hm-front && yarn build
```
Expected: 成功

- [ ] **Step 3: 数据库清理（spec 确认无需兼容旧数据）**

启动前手动执行（或通过临时 SQL）：
```sql
DELETE FROM acme_async_log;
DELETE FROM acme_certification;
DELETE FROM acme_user_Info;
```
（旧字段 `domain` 会残留为空列，无害；`ddl-auto: update` 会新增 `cert_id/name/ca_code/domains/dns_group_id` 等列与 `acme_global_config` 表。）

- [ ] **Step 4: 手测清单**

启动 `mvn spring-boot:run -Dspring-boot.run.profiles=dev`（dev 用 staging，不撞生产速率限制），逐项验证：

1. 设置页填邮箱 → 保存 → 刷新页面邮箱回填。
2. 证书管理页 → 申请证书（CA 选 Let's Encrypt Staging、选 DNS 组、加一个域名）→ 弹"任务号"→ 2s 轮询 → 任务完成后列表出现新证书、状态"有效"。
3. 点"预览"→ 四个 tab 显示 PEM。
4. 点"下载"→ 得到 `{certId}_certificate.zip`，解压有 .key/.crt/.pem/.fullchain.pem。
5. 点"续期"→ 任务完成后 `certApplyTime` 刷新。
6. 点"删除"→ 二次确认 → 列表移除、`certs/{certId}/` 目录消失。
7. 同一证书连续点两次"申请/续期"→ 第二次立即 `FAILED`，logInfo="该证书已有任务进行中"（验证并发锁）。
8. dns 页不再有任何证书按钮/徽标。

- [ ] **Step 5: 最终 Commit（若有手测中发现的小修）**

```bash
git add -A
git commit -m "chore(acme): 收尾验证修复" || echo "无需提交"
```

---

## Self-Review 结果

**Spec 覆盖**：逐条对照 spec——
- 数据模型（AcmeUserInfoEntity.caCode / AcmeCertificationEntity.SAN / AcmeAsyncLogEntity 状态 / AcmeGlobalConfigEntity）→ Task 4 ✓
- 5 组件（CaRegistry / DomainResolver / AccountService / OrderService / FileManager / CertificateService 门面）→ Task 2/3/5/6/7/8 ✓
- 并发锁快速失败 → Task 8 ✓
- 任务追踪落库 + taskId 贯穿 → Task 8 + Task 11 ✓
- 11 个 REST 端点 + 全局邮箱 → Task 9 ✓
- DNS 解耦 → Task 10 ✓
- 续期逐证书扫描 → Task 11 ✓
- 前端证书管理页 + dns 解耦 + setting 邮箱 → Task 13/14 ✓
- Bug 修复（#1 枚举 / #4 staging / #5 域名解析 + 裸 get / #6 长事务 / #7 文件 / #10 日志混用）→ Task 1/3/4/5/7/12 ✓
- 测试（DomainResolver / CaRegistry / FileManager / 并发锁）→ Task 2/3/5/8 ✓

**无占位**：每个 Step 给出的代码即为最终实现，无"先写占位再下一 Step 修正"的模式。残留的 `> 实现者注意` / `> 注` 仅是强调说明（如 `@Async` 自注入、`providerId→providerName` 转换、`ddl-auto` 不删旧列），均已落在同 Step 的代码里。

**类型一致**：`AcmeService.CertApplyContext(name, caCode, domains, dnsGroupId)` 在 Task 8 定义、Task 9 使用，签名一致；`AcmeTaskContext` 字段（taskId/certId/ca/domains/account/plugin）在 Task 7 定义、Task 8 使用，一致；`AcmeOrderService.CertIssueResult(certificate, keyPair)` 在 Task 7 定义并在 `execute`/`finalizeAndIssue`/`persistAndSaveFiles` 间传递，一致；`CertificateFileManager.saveText/readContent/packZip/delete` 在 Task 5 定义、Task 7/9 使用，一致；`AcmeCaRegistry.getByCode/list` 在 Task 2 定义、Task 6/8/9 使用，一致；`AcmeService` 构造器 7 个依赖与 `CertificateServiceConcurrencyTest` 的 `new AcmeService(null×7)` 一致。

**运行期注意（非占位，编译不报错，但实现者需知晓）**：
1. `@Async` 生效依赖 `@EnableAsync`——检查主类 `HMonetaApplication` 是否已标注；若无，Task 8 编译可通过但 async 不生效（变成同步阻塞），需在主类加 `@EnableAsync`。建议 Task 8 Step 5 编译后顺手确认。
2. `ddl-auto: update` 只增列不删列——旧 `acme_certification.domain` 列残留为空、无害；新表 `acme_global_config` 与新列由 Hibernate 自动创建。Task 15 Step 3 的 `DELETE` 语句清理旧行。
