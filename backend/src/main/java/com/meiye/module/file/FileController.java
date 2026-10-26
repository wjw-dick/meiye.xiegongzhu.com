package com.meiye.module.file;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.R;
import com.meiye.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final StorageManager storageManager;

    @Value("${meiye.storage.local-dir:./data/uploads}")
    private String localDir;

    @PostMapping("/api/files/upload")
    @Audit(module = "文件管理", action = "上传文件")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "common") String dir,
                                         @RequestParam(defaultValue = "OTHER") String bizType) throws Exception {
        Long tenantId = TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId();
        try (InputStream in = file.getInputStream()) {
            String url = storageManager.upload(tenantId, in, file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), dir, bizType);
            return R.ok(Map.of("url", url, "name", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                    "size", file.getSize()));
        }
    }

    @GetMapping("/api/files/**")
    public org.springframework.http.ResponseEntity<byte[]> get(jakarta.servlet.http.HttpServletRequest request) throws Exception {
        String uri = request.getRequestURI();
        String relative = uri.substring("/api/files/".length());
        Path root = Path.of(localDir).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.exists(target) || Files.isDirectory(target)) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(target);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                .header("Cache-Control", "public, max-age=86400")
                .body(Files.readAllBytes(target));
    }
}
