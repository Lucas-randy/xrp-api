package com.example.xrpapi.util;

import org.stellar.sdk.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtil {

    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        System.out.println("🔐 ENCRYPT - Input: " + plainText);
        System.out.println("🔐 ENCRYPT - Input length: " + plainText.length());

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(bytes);
        String result = Base64.getEncoder().encodeToString(encrypted);

        System.out.println("🔐 ENCRYPT - Output: " + result);
        return result;
    }

    public static String decrypt(String cipherText, SecretKey secretKey) throws Exception {
        System.out.println("🔓 DECRYPT - Input: " + cipherText);

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decodedBytes);

            // Tester différents encodages
            String seedUTF8 = new String(decrypted, StandardCharsets.UTF_8);
            String seedASCII = new String(decrypted, StandardCharsets.US_ASCII);
            String seedISO = new String(decrypted, StandardCharsets.ISO_8859_1);

            System.out.println("🔓 DECRYPT - UTF-8: '" + seedUTF8 + "' (length: " + seedUTF8.length() + ")");
            System.out.println("🔓 DECRYPT - ASCII: '" + seedASCII + "' (length: " + seedASCII.length() + ")");
            System.out.println("🔓 DECRYPT - ISO: '" + seedISO + "' (length: " + seedISO.length() + ")");

            // Tester chaque version
            String[] candidates = {
                    seedUTF8.trim(),
                    seedASCII.trim(),
                    seedISO.trim(),
                    seedUTF8.replaceAll("\\s+", ""),
                    seedASCII.replaceAll("\\s+", ""),
                    seedISO.replaceAll("\\s+", "")
            };

            for (int i = 0; i < candidates.length; i++) {
                String candidate = candidates[i];
                System.out.println("🧪 Testing candidate " + i + ": '" + candidate + "' (length: " + candidate.length() + ")");

                if (candidate.startsWith("S") && candidate.length() == 56) {
                    try {
                        org.stellar.sdk.KeyPair.fromSecretSeed(candidate);
                        System.out.println("✅ VALID SEED FOUND: " + candidate);
                        return candidate;
                    } catch (Exception e) {
                        System.out.println("❌ Candidate " + i + " failed: " + e.getMessage());
                    }
                }
            }

            throw new Exception("Aucune seed valide trouvée parmi les candidats");

        } catch (Exception e) {
            System.err.println("❌ DECRYPT ERROR: " + e.getMessage());
            throw e;
        }
    }

    public static SecretKey getFixedTestKey() throws Exception {
        byte[] keyBytes = "12345678901234561234567890123456".getBytes();
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Méthode pour tester le déchiffrement d'une seed spécifique
    public static void testDecryption(String encryptedSeed) {
        try {
            SecretKey key = getFixedTestKey();
            System.out.println("🔍 Testing decryption for: " + encryptedSeed);
            String result = decrypt(encryptedSeed, key);
            System.out.println("✅ Decryption successful: " + result);
        } catch (Exception e) {
            System.err.println("❌ Decryption failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

