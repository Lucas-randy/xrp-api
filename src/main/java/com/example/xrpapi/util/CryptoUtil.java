package com.example.xrpapi.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtil {

    // Méthode pour chiffrer avec une clé AES donnée
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // Méthode pour déchiffrer avec une clé AES donnée
    public static String decrypt(String cipherText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        byte[] decrypted = cipher.doFinal(decodedBytes);

        return new String(decrypted, "UTF-8");
    }
    public static SecretKey getFixedTestKey() throws Exception {
        byte[] keyBytes = "12345678901234561234567890123456".getBytes(); // 32 bytes pour AES-256
        return new SecretKeySpec(keyBytes, "AES");
    }

}
