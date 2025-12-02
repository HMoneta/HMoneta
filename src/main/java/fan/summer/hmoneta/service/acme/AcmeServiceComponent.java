package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeAsyncLogRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.database.repository.acme.AcmeUserInfoRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeNetworkException;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Acme证书申请类
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/18
 */
@Log4j2
@Component
public class AcmeServiceComponent {
    private static final Logger logger = LoggerFactory.getLogger(AcmeService.class);

    final int maxAttempts = 10;  // 最大尝试次数

    @Value("${acme.url}")
    private String acmeUri;


    private final AcmeCertificationRepository acmeAcmeCertificationRepository;
    private final AcmeUserInfoRepository acmeUserInfoRepository;
    private final AcmeAsyncLogRepository acmeAsyncLogRepository;


    public AcmeServiceComponent(
            AcmeCertificationRepository acmeAcmeCertificationRepository,
            AcmeUserInfoRepository acmeUserInfoRepository,
            AcmeAsyncLogRepository acmeAsyncLogRepository) {
        this.acmeAcmeCertificationRepository = acmeAcmeCertificationRepository;
        this.acmeUserInfoRepository = acmeUserInfoRepository;
        this.acmeAsyncLogRepository = acmeAsyncLogRepository;

    }


    @Transactional(rollbackOn = Exception.class)
    protected AcmeUserInfoEntity findOrCreateAcmeUser(String domain) {
        AcmeUserInfoEntity acmeUserInfo = null;
        boolean needCreateAcmeUser = false;
        List<AcmeUserInfoEntity> allAcmeUserInfo = null;
        try {
            allAcmeUserInfo = acmeUserInfoRepository.findAll();
        } catch (Exception e) {
            logger.error("查询ACME用户信息失败: ", e);
            throw new HMException(AcmeExceptionEnum.ACME_ACCOUNT_NOT_EXIST_ERROR);
        }

        if (ObjectUtils.isNotEmpty(allAcmeUserInfo)) {
            acmeUserInfo = allAcmeUserInfo.getFirst();
            if (ObjectUtils.isEmpty(acmeUserInfo.getPrivateKey()) && ObjectUtils.isEmpty(acmeUserInfo.getPublicKey())) {
                needCreateAcmeUser = true;
            }
        } else {
            throw new HMException(AcmeExceptionEnum.ACME_ACCOUNT_NOT_EXIST_ERROR);
        }
        // 创建Session并设置超时时间
        Session session = new Session(acmeUri);
        session.networkSettings().setTimeout(java.time.Duration.ofSeconds(120)); // 设置网络超时120秒
        if (needCreateAcmeUser) {
            // 创建用户
            KeyPair keyPair;
            String accountEmail = "mailto:" + acmeUserInfo.getUserEmail();
            keyPair = KeyPairUtils.createKeyPair(2048);
            try {
                Account account = new AccountBuilder()
                        .addContact(accountEmail)
                        .agreeToTermsOfService()
                        .useKeyPair(keyPair)
                        .create(session);
                if (ObjectUtils.isNotEmpty(account)) {
                    acmeUserInfo.setUserEmail(acmeUserInfo.getUserEmail());
                    acmeUserInfo.saveKeyPair(keyPair);
                    acmeUserInfoRepository.save(acmeUserInfo);
                } else {
                    throw new RuntimeException("[ACME-Task]创建账户失败");
                }
            } catch (AcmeException e) {
                throw new RuntimeException(e.getMessage(), e);

            }
        }
        return acmeUserInfo;
    }


    @Async
    @Transactional(rollbackOn = Exception.class)
    protected void useDnsChallengeGetCertification(AcmeTaskContext acmeTaskContext) {
        logger.info("=============开始申请证书=============");
        // 获取证书
        try {
            if (ObjectUtils.isNotEmpty(acmeAcmeCertificationRepository.findOneByDomain(acmeTaskContext.getDomain()))) {
                acmeAcmeCertificationRepository.deleteByDomain(acmeTaskContext.getDomain());
            }
        } catch (Exception e) {
            logger.error("[ACME-Task:{}]删除旧证书信息失败: {}", acmeTaskContext.getTaskId(), e.getMessage());
        }

        Session session = new Session(acmeUri);
        session.networkSettings().setTimeout(java.time.Duration.ofSeconds(120));
        try {
            KeyPair keyPair = acmeTaskContext.getAcmeUserInfoEntity().generateKeyPair();
            Login login = null;
            int loginAttempts = 0;
            final int maxLoginAttempts = 3;

            // 登录重试机制
            while (login == null && loginAttempts < maxLoginAttempts) {
                try {
                    logger.info("[ACME-Task:{}]开始登录", acmeTaskContext.getTaskId());
                    login = new AccountBuilder().onlyExisting().agreeToTermsOfService().useKeyPair(keyPair).createLogin(session);
                } catch (AcmeNetworkException e) {
                    loginAttempts++;
                    logger.warn("ACME登录尝试第{}次失败: {}, 5秒后重试", loginAttempts, e.getMessage());
                    if (loginAttempts >= maxLoginAttempts) {
                        throw e;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("登录重试过程被中断", ie);
                    }
                }
            }
            // 发起订单
            logger.info("[ACME-Task:{}]创建订单", acmeTaskContext.getTaskId());
            Order order = login.newOrder().domain(acmeTaskContext.getDomain()).create();
            Order bindOrder = login.bindOrder(order.getLocation());
            Authorization authorization = bindOrder.getAuthorizations().getFirst();
            logger.info("[ACME-Task:{}]发起DNS-01挑战", acmeTaskContext.getTaskId());

            // 添加对授权获取的异常处理
            authorization.findChallenge(Dns01Challenge.class).ifPresentOrElse(challenge -> {
                String digest = challenge.getDigest();
                logger.info("[ACME-Task:{}]获取到DNS挑战内容:{}", acmeTaskContext.getTaskId(), digest);
                logger.info("[ACME-Task:{}]修改DNS", acmeTaskContext.getTaskId());
                String subDomain = acmeTaskContext.getDomain().substring(0, acmeTaskContext.getDomain().indexOf('.'));
                String mainDomain = acmeTaskContext.getDomain().substring(acmeTaskContext.getDomain().indexOf('.') + 1);
                // 创建DNS解析记录
                boolean status = acmeTaskContext.getHmDnsProviderPlugin().modifyDns(mainDomain, "_acme-challenge." + subDomain, "TXT", digest);
                logger.info("[ACME-Task:{}]DNS修改状态:{}", acmeTaskContext.getTaskId(), status);
                if (status & waitForDnsPropagation("_acme-challenge." + acmeTaskContext.getDomain(), digest)) {
                    try {
                        logger.info("[ACME-Task:{}]开启挑战", acmeTaskContext.getTaskId());
                        challenge.trigger();
                        logger.info("[ACME-Task:{}]挑战结果验证", acmeTaskContext.getTaskId());
                        while (!EnumSet.of(Status.VALID, Status.INVALID).contains(authorization.getStatus())) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(5000);
                                logger.info("[ACME-Task:{}]再次验证", acmeTaskContext.getTaskId());
                                authorization.fetch();
                            } catch (InterruptedException ignored) {
                                logger.error("[ACME-Task:{}]线程意外中断:{}", acmeTaskContext.getTaskId(), ignored.getMessage());
                                break;
                            } catch (AcmeException e) {
                                logger.error("[ACME-Task:{}]验证授权时发生错误: {}", acmeTaskContext.getTaskId(), e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                        logger.info("[ACME-Task:{}]挑战结果:{}", acmeTaskContext.getTaskId(), authorization.getStatus());
                        if (authorization.getStatus() == Status.VALID) {
                            logger.info("[ACME-Task:{}]验证通过", acmeTaskContext.getTaskId());
                            removeTxtDnsInfo(acmeTaskContext.getHmDnsProviderPlugin(), mainDomain, subDomain);
                            // 获取证书
                            try {
                                logger.info("[ACME-Task:{}]获取证书", acmeTaskContext.getTaskId());
                                KeyPair cerKeyPair = KeyPairUtils.createKeyPair(2048);
                                order.execute(cerKeyPair);
                                while (!EnumSet.of(Status.VALID, Status.INVALID).contains(order.getStatus())) {
                                    TimeUnit.MILLISECONDS.sleep(5000);
                                    logger.info("[ACME-Task:{}]继续验证证书签发状态", acmeTaskContext.getTaskId());
                                    order.fetch();
                                }
                                if (order.getStatus() == Status.VALID) {
                                    logger.info("[ACME-Task:{}]订单确认完成，开始下载证书", acmeTaskContext.getTaskId());
                                    Certificate cert = order.getCertificate();
                                    saveCertificateFiles(cerKeyPair, cert, acmeTaskContext.getDomain(), acmeTaskContext.getTaskId());
                                    AcmeCertificationEntity acmeCertificationEntity = new AcmeCertificationEntity();
                                    // 获取证书到期日
                                    X509Certificate certificate = cert.getCertificate();
                                    acmeCertificationEntity.setNotBefore(certificate.getNotBefore());
                                    acmeCertificationEntity.setNotAfter(certificate.getNotAfter());
                                    acmeCertificationEntity.saveKeyPair(keyPair);
                                    acmeCertificationEntity.setCertApplyTime(LocalDateTime.now());
                                    acmeCertificationEntity.setDomain(acmeTaskContext.getDomain());
                                    acmeCertificationEntity.setProviderName(acmeTaskContext.getHmDnsProviderPlugin().providerName());
                                    try {
                                        acmeAcmeCertificationRepository.save(acmeCertificationEntity);
                                    } catch (Exception e) {
                                        logger.error("[ACME-Task:{}]保存证书信息失败: {}", acmeTaskContext.getTaskId(), e.getMessage());
                                    }
                                } else {
                                    logger.error("[ACME-Task:{}]订单确认失败", acmeTaskContext.getTaskId());
                                }
                            } catch (AcmeException | InterruptedException | CertificateEncodingException |
                                     IOException e) {
                                logger.error("[ACME-Task:{}]获取证书过程中发生错误: {}", acmeTaskContext.getTaskId(), e.getMessage());
                                throw new RuntimeException(e);
                            }
                        } else {
                            removeTxtDnsInfo(acmeTaskContext.getHmDnsProviderPlugin(), mainDomain, subDomain);
                            logger.error("[ACME-Task:{}]验证未通过", acmeTaskContext.getTaskId());
                        }
                    } catch (AcmeException e) {
                        logger.error("[ACME-Task:{}]处理DNS挑战时发生错误: {}", acmeTaskContext.getTaskId(), e.getMessage());
                        removeTxtDnsInfo(acmeTaskContext.getHmDnsProviderPlugin(), mainDomain, subDomain);
                        throw new RuntimeException(e);
                    }
                } else {
                    logger.error("[ACME-Task:{}]未通过DNS记录验证", acmeTaskContext.getTaskId());
                    if (status) {
                        removeTxtDnsInfo(acmeTaskContext.getHmDnsProviderPlugin(), mainDomain, subDomain);
                    }
                }
            }, () -> {
                logger.error("[ACME-Task:{}]未正常获取到Dns01Challenge对象", acmeTaskContext.getTaskId());
            });
        } catch (AcmeException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("[ACME-Task:{}]证书申请过程中发生错误: {}", acmeTaskContext.getTaskId(), e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("[ACME-Task:{}]证书申请过程中发生未知错误: {}", acmeTaskContext.getTaskId(), e.getMessage());
            throw new RuntimeException(e);
        } finally {
//            if (ObjectUtils.isNotEmpty(acmeTaskContext)) {
//                if (ObjectUtils.isEmpty(acmeTaskContext.getStatusInfo())) {
//                    acmeTaskContext.setStatusInfo("-1");
//                }
//                acmeChallengeInfoRepository.save(acmeTaskContext);
//            }
            logger.info("[ACME-Task:{}]结束证书申请服务", acmeTaskContext.getTaskId());
        }

    }


    private boolean waitForDnsPropagation(String domain, String expectedTxtRecord) {
        log.info("开始验证域名{}的TXT解析是否生效", domain);
        try {
            Lookup lookup = new Lookup(domain, Type.TXT);
            for (int attempt = 1; this.maxAttempts >= attempt; attempt++) {
                lookup.run();
                if (lookup.getResult() == Lookup.SUCCESSFUL) {
                    log.info("DNS记录查询成功，开始验证");
                    org.xbill.DNS.Record[] answers = lookup.getAnswers();
                    for (org.xbill.DNS.Record record : answers) {
                        if (record instanceof TXTRecord txtRecord) {
                            for (String txt : txtRecord.getStrings()) {
                                if (txt.equals(expectedTxtRecord)) {
                                    log.info("通过解析验证，DNS记录正确，开始下一步");
                                    return true;
                                }
                            }
                        }
                    }
                    log.info("未匹配上指定的DNS记录，10秒后重新尝试。第{}次尝试", attempt);
                    TimeUnit.MILLISECONDS.sleep(10000);
                } else {
                    log.info("DNS记录查询未成功，10秒后重新尝试。第{}次尝试", attempt);
                    TimeUnit.MILLISECONDS.sleep(10000);
                    if (attempt == maxAttempts) {
                        return false;
                    }
                }
            }
        } catch (TextParseException | InterruptedException e) {
            log.error(e.toString());
            return false;
        }
        return false;
    }

    private void removeTxtDnsInfo(HmDnsProviderPlugin ddnsProvider, String domain, String subDomain) {
        log.info("开始清理用于验证的DNS记录");
        try {
            ddnsProvider.deleteDns(domain, "_acme-challenge." + subDomain, "TXT");
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            log.info("结束清理");
        }
    }

    private void saveCertificateFiles(KeyPair keyPair, Certificate cert, String domain, String taskId) throws IOException, CertificateEncodingException {
        logger.info("开始保存证书文件，域名: {}, 任务ID: {}", domain, taskId);

        // 创建证书存储目录
        String certPath = "certs/" + domain;
        File certDir = new File(certPath);
        // 删除已存在目录并重新创建
        boolean successMkDir = false;
        if (certDir.exists()) {
            if (deleteRecursively(certDir)) {
                successMkDir = certDir.mkdirs();
            }
        } else {
            successMkDir = certDir.mkdirs();
        }
        if (!successMkDir) {
            throw new IOException("无法创建证书目录: " + certPath);
        }
        // 保存私钥 (KEY文件)
        try (FileWriter fw = new FileWriter(new File(certDir, domain + ".key"))) {
            KeyPairUtils.writeKeyPair(keyPair, fw);
            logger.info("私钥文件已保存: {}", domain + ".key");
        }

        X509Certificate certificate = cert.getCertificate();
        List<X509Certificate> chain = cert.getCertificateChain();

        // 保存证书 (CRT文件)
        try (FileOutputStream fos = new FileOutputStream(new File(certDir, domain + ".crt"))) {
            writeCertificate(certificate, fos);
            logger.info("证书文件已保存: {}", domain + ".crt");
//            log.info("证书文件已保存: {}", domain + ".crt");
        }

        // 保存完整证书链 (PEM文件)
        try (FileOutputStream fos = new FileOutputStream(new File(certDir, domain + ".pem"))) {
            writeCertificate(certificate, fos);

            // 添加中间证书
            for (int i = 1; i < chain.size(); i++) {
                fos.write('\n');
                writeCertificate(chain.get(i), fos);
            }
            logger.info("完整证书链文件已保存: {}", domain + ".pem");
//            log.info("完整证书链文件已保存: {}", domain + ".pem");
        }

        // 保存完整证书链和私钥 (FULLCHAIN文件) - 修复后的版本
        File fullchainFile = new File(certDir, domain + ".fullchain.pem");

        // 先将私钥写入字符串
        StringWriter keyWriter = new StringWriter();
        KeyPairUtils.writeKeyPair(keyPair, keyWriter);
        String privateKeyPem = keyWriter.toString();

        // 使用单个输出流写入所有内容
        try (FileOutputStream fos = new FileOutputStream(fullchainFile)) {
            // 写入私钥
            fos.write(privateKeyPem.getBytes(StandardCharsets.UTF_8));
            fos.write('\n');

            // 写入证书
            writeCertificate(certificate, fos);

            // 写入证书链
            for (int i = 1; i < chain.size(); i++) {
                fos.write('\n');
                writeCertificate(chain.get(i), fos);
            }
            logger.info("完整证书链和私钥文件已保存: {}", domain + ".fullchain.pem");
//            log.info("完整证书链和私钥文件已保存: {}", domain + ".fullchain.pem");
        }
        logger.info("所有证书文件保存完成");
//        log.info("所有证书文件保存完成");
    }

    private void writeCertificate(X509Certificate certificate, OutputStream out) throws IOException, CertificateEncodingException {
        byte[] encoded = certificate.getEncoded();
        String encoded64 = Base64.getEncoder().encodeToString(encoded);

        // 按照PEM格式写入
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.US_ASCII), false);
        writer.println("-----BEGIN CERTIFICATE-----");

        // 每64个字符一行
        for (int i = 0; i < encoded64.length(); i += 64) {
            writer.println(encoded64.substring(i, Math.min(i + 64, encoded64.length())));
        }

        writer.println("-----END CERTIFICATE-----");
        writer.flush();
        // 注意：这里不关闭writer，因为我们使用的是外部传入的OutputStream
    }


    private static boolean deleteRecursively(File file) {
        // 如果是文件或空文件夹，直接删除
        if (file == null || !file.exists()) {
            return false;
        }
        if (file.isFile() || Objects.requireNonNull(file.list()).length == 0) {
            return file.delete();
        }

        // 如果是文件夹，先删除其内容
        File[] files = file.listFiles();
        if (files != null) {
            for (File subFile : files) {
                if (!deleteRecursively(subFile)) {
                    return false; // 删除失败时停止
                }
            }
        }

        // 最后删除文件夹本身
        return file.delete();
    }

}
