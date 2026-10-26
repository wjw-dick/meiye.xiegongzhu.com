package com.meiye.storage;

import lombok.Data;

import java.io.InputStream;

public interface StorageDriver {

    String upload(InputStream in, String objectName, String contentType, long size) throws Exception;

    void delete(String objectName) throws Exception;

    boolean test();

    @Data
    class StorageConf {
        private String driver = "LOCAL";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket = "meiye";
        private String region;
        private String domain;
        private String localDir = "./data/uploads";
        private String publicBase = "/api/files";
    }
}
