package hr.algebra.camelle4.security;

import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class PayloadSecurityConfig {

    @Value("${app.security.aes-key}")
    private String aesKeyBase64;

    @Value("${app.security.aes-iv}")
    private String aesIvBase64;

    @Bean
    public CryptoDataFormat orderCryptoDataFormat() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        byte[] ivBytes  = Base64.getDecoder().decode(aesIvBase64);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        CryptoDataFormat crypto = new CryptoDataFormat("AES/CTR/NoPadding", secretKey);
        crypto.setInitializationVector(ivBytes);
        crypto.setShouldInlineInitializationVector(true);
        return crypto;
    }
}