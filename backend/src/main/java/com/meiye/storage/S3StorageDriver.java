package com.meiye.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * S3 兼容驱动：MinIO / AWS S3 / 阿里云 OSS / 腾讯云 COS。
 */
@Slf4j
public class S3StorageDriver implements StorageDriver {

    private final StorageConf conf;

    public S3StorageDriver(StorageConf conf) {
        this.conf = conf;
    }

    private MinioClient client() {
        return MinioClient.builder()
                .endpoint(conf.getEndpoint())
                .credentials(conf.getAccessKey(), conf.getSecretKey())
                .build();
    }

    @Override
    public String upload(InputStream in, String objectName, String contentType, long size) throws Exception {
        MinioClient client = client();
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(conf.getBucket()).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(conf.getBucket()).build());
        }
        client.putObject(PutObjectArgs.builder()
                .bucket(conf.getBucket())
                .object(objectName)
                .stream(in, size, -1)
                .contentType(contentType)
                .build());
        String domain = conf.getDomain();
        if (domain == null || domain.isBlank()) {
            return conf.getEndpoint() + "/" + conf.getBucket() + "/" + objectName;
        }
        return domain.replaceAll("/$", "") + "/" + objectName;
    }

    @Override
    public void delete(String objectName) throws Exception {
        client().removeObject(RemoveObjectArgs.builder()
                .bucket(conf.getBucket()).object(objectName).build());
    }

    @Override
    public boolean test() {
        try {
            MinioClient client = client();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(conf.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(conf.getBucket()).build());
            }
            return true;
        } catch (Exception e) {
            log.warn("存储连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}
