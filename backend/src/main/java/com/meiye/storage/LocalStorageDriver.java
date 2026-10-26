package com.meiye.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class LocalStorageDriver implements StorageDriver {

    private final String baseDir;
    private final String publicBase;

    public LocalStorageDriver(String baseDir, String publicBase) {
        this.baseDir = baseDir;
        this.publicBase = publicBase;
    }

    @Override
    public String upload(InputStream in, String objectName, String contentType, long size) throws Exception {
        Path target = Path.of(baseDir, objectName).normalize();
        Path root = Path.of(baseDir).toAbsolutePath().normalize();
        if (!target.toAbsolutePath().normalize().startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return publicBase + "/" + objectName.replace('\\', '/');
    }

    @Override
    public void delete(String objectName) throws Exception {
        Files.deleteIfExists(Path.of(baseDir, objectName));
    }

    @Override
    public boolean test() {
        try {
            Path p = Path.of(baseDir);
            Files.createDirectories(p);
            return Files.isWritable(p);
        } catch (Exception e) {
            return false;
        }
    }
}
