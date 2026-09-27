package com.securehook.service;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SignatureServiceTest {

    @Test
    void generateSignature_validInputs_matchesExpectedHex() {
        SignatureService signatureService = new SignatureService();
        
        // 32 null bytes representing a raw secret key
        byte[] rawKey = new byte[32];
        String base64UrlSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(rawKey);
        
        String payload = "{\"test\":\"data\"}";
        
        // Expected HMAC-SHA256 for {"test":"data"} using 32 null bytes, prefixed with sha256=
        String expectedSignature = "sha256=8e15aab23f79b5231c6eb1919fbbaf5a2539cc451400593f77dcc97cc5f10948";
        
        String actualSignature = signatureService.generateSignature(payload, base64UrlSecret);
        
        assertEquals(expectedSignature, actualSignature);
    }
}
