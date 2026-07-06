package server;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
    private static final String KEY = "MiniProjectKey12";
    private static final String ALGORITHM = "AES";

    public static String encrypt(String data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }
}