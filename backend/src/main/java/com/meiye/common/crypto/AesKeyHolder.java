package com.meiye.common.crypto;

import cn.hutool.crypto.SecureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 敏感字段 AES 加解密（确定性加密，支持手机号精确查询）。
 */
@Component
public class AesKeyHolder {

    private static volatile byte[] key;

    public AesKeyHolder(@Value("${meiye.data.aes-key:MeiyeDataAesKey2026!}") String aesKey) {
        byte[] raw = aesKey.getBytes(StandardCharsets.UTF_8);
        key = SecureUtil.md5(new String(raw, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || plain.startsWith("ENC:")) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return "ENC:" + Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || !cipherText.startsWith("ENC:")) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            byte[] data = Base64.getDecoder().decode(cipherText.substring(4));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }
}
