package com.meiye.storage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meiye.common.crypto.AesKeyHolder;
import com.meiye.common.exception.BizException;
import com.meiye.domain.entity.StorageConfig;
import com.meiye.domain.mapper.StorageConfigMapper;
import com.meiye.domain.entity.Attachment;
import com.meiye.domain.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageManager {

    private final StorageConfigMapper storageConfigMapper;
    private final AttachmentMapper attachmentMapper;
    private final ObjectMapper objectMapper;

    @Value("${meiye.storage.local-dir:./data/uploads}")
    private String defaultLocalDir;
    @Value("${meiye.storage.public-base:/api/files}")
    private String defaultPublicBase;
    @Value("${meiye.minio.endpoint:http://127.0.0.1:9000}")
    private String defaultEndpoint;
    @Value("${meiye.minio.access-key:admin}")
    private String defaultAccessKey;
    @Value("${meiye.minio.secret-key:}")
    private String defaultSecretKey;
    @Value("${meiye.minio.bucket:meiye}")
    private String defaultBucket;

    public StorageConfig record(Long tenantId) {
        return storageConfigMapper.selectOne(new LambdaQueryWrapper<StorageConfig>()
                .eq(StorageConfig::getTenantId, tenantId).last("limit 1"));
    }

    public StorageDriver.StorageConf load(Long tenantId) {
        StorageConfig rec = record(tenantId);
        StorageDriver.StorageConf conf = new StorageDriver.StorageConf();
        if (rec == null) {
            conf.setDriver("LOCAL");
            conf.setLocalDir(defaultLocalDir);
            conf.setPublicBase(defaultPublicBase);
            return conf;
        }
        conf.setDriver(rec.getDriver());
        if (rec.getConfigJson() != null && !rec.getConfigJson().isBlank()) {
            try {
                conf = objectMapper.readValue(rec.getConfigJson(), StorageDriver.StorageConf.class);
                conf.setDriver(rec.getDriver());
            } catch (Exception e) {
                log.warn("存储配置解析失败: {}", e.getMessage());
            }
        }
        conf.setAccessKey(AesKeyHolder.decrypt(conf.getAccessKey()));
        conf.setSecretKey(AesKeyHolder.decrypt(conf.getSecretKey()));
        if (conf.getBucket() == null || conf.getBucket().isBlank()) {
            conf.setBucket(defaultBucket);
        }
        if (conf.getLocalDir() == null || conf.getLocalDir().isBlank()) {
            conf.setLocalDir(defaultLocalDir);
        }
        if (conf.getPublicBase() == null || conf.getPublicBase().isBlank()) {
            conf.setPublicBase(defaultPublicBase);
        }
        return conf;
    }

    public StorageDriver driver(Long tenantId) {
        StorageDriver.StorageConf conf = load(tenantId);
        return switch (conf.getDriver()) {
            case "MINIO", "S3", "OSS", "COS" -> new S3StorageDriver(conf);
            default -> new LocalStorageDriver(conf.getLocalDir(), conf.getPublicBase());
        };
    }

    public String upload(Long tenantId, InputStream in, String filename, String contentType, long size, String dir, String bizType) {
        String ext = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
        String objectName = dir + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            String url = driver(tenantId).upload(in, objectName, contentType == null ? "application/octet-stream" : contentType, size);
            Attachment att = new Attachment();
            att.setTenantId(tenantId);
            att.setBizType(bizType);
            att.setName(filename);
            att.setUrl(url);
            att.setSize(size);
            att.setMime(contentType);
            att.setStorageDriver(load(tenantId).getDriver());
            attachmentMapper.insert(att);
            return url;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BizException(500, "文件上传失败：" + e.getMessage());
        }
    }

    public boolean test(StorageDriver.StorageConf conf) {
        conf.setAccessKey(AesKeyHolder.decrypt(conf.getAccessKey()));
        conf.setSecretKey(AesKeyHolder.decrypt(conf.getSecretKey()));
        return switch (conf.getDriver()) {
            case "MINIO", "S3", "OSS", "COS" -> new S3StorageDriver(conf).test();
            default -> new LocalStorageDriver(conf.getLocalDir(), conf.getPublicBase()).test();
        };
    }

    public StorageConfig save(Long tenantId, String driver, Map<String, Object> config) {
        StorageDriver.StorageConf old = load(tenantId);
        StorageDriver.StorageConf conf = new StorageDriver.StorageConf();
        conf.setDriver(driver);
        conf.setEndpoint(str(config.get("endpoint"), old.getEndpoint()));
        conf.setAccessKey(maskKeep(str(config.get("accessKey"), null), old.getAccessKey()));
        conf.setSecretKey(maskKeep(str(config.get("secretKey"), null), old.getSecretKey()));
        conf.setBucket(str(config.get("bucket"), old.getBucket()));
        conf.setRegion(str(config.get("region"), old.getRegion()));
        conf.setDomain(str(config.get("domain"), old.getDomain()));
        conf.setLocalDir(str(config.get("localDir"), old.getLocalDir()));
        conf.setPublicBase(str(config.get("publicBase"), old.getPublicBase()));
        try {
            StorageDriver.StorageConf stored = new StorageDriver.StorageConf();
            stored.setDriver(conf.getDriver());
            stored.setEndpoint(conf.getEndpoint());
            stored.setAccessKey(AesKeyHolder.encrypt(conf.getAccessKey()));
            stored.setSecretKey(AesKeyHolder.encrypt(conf.getSecretKey()));
            stored.setBucket(conf.getBucket());
            stored.setRegion(conf.getRegion());
            stored.setDomain(conf.getDomain());
            stored.setLocalDir(conf.getLocalDir());
            stored.setPublicBase(conf.getPublicBase());
            StorageConfig rec = record(tenantId);
            if (rec == null) {
                rec = new StorageConfig();
                rec.setTenantId(tenantId);
            }
            rec.setDriver(driver);
            rec.setConfigJson(objectMapper.writeValueAsString(stored));
            rec.setTestedAt(java.time.LocalDateTime.now());
            if (rec.getId() == null) {
                storageConfigMapper.insert(rec);
            } else {
                storageConfigMapper.updateById(rec);
            }
            return rec;
        } catch (Exception e) {
            throw new BizException(500, "存储配置保存失败");
        }
    }

    public Map<String, Object> maskedConfig(Long tenantId) {
        StorageDriver.StorageConf conf = load(tenantId);
        Map<String, Object> m = new HashMap<>();
        boolean local = "LOCAL".equals(conf.getDriver());
        m.put("driver", conf.getDriver());
        m.put("endpoint", local ? null : conf.getEndpoint());
        m.put("accessKey", conf.getAccessKey() == null || conf.getAccessKey().isBlank() ? "" : "******");
        m.put("secretKey", conf.getSecretKey() == null || conf.getSecretKey().isBlank() ? "" : "******");
        m.put("bucket", conf.getBucket());
        m.put("region", conf.getRegion());
        m.put("domain", conf.getDomain());
        m.put("localDir", conf.getLocalDir());
        m.put("publicBase", conf.getPublicBase());
        m.put("configured", record(tenantId) != null);
        return m;
    }

    private String str(Object v, String def) {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }

    private String maskKeep(String v, String old) {
        return v == null || v.contains("******") ? old : v;
    }
}
