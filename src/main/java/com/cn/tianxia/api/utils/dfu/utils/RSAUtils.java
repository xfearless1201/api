package com.cn.tianxia.api.utils.dfu.utils;

/**
 * @author Vicky
 * @version 1.0.0
 * @ClassName RSAUtils
 * @Description
 * @Date 2019/6/8 20 59
 **/

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RSAUtils {
    private static final String CHARSET = "UTF-8";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String RSA_PUBLIC_KEY = "RSAPublicKey";
    private static final String RSA_PRIVATE_KEY = "RSAPrivateKey";
    private static final String MD5_WITH_RSA_ALGORITHM = "MD5withRSA";
    private static final String SHA1_WITH_RSA_ALGORITHM = "SHA1withRSA";

    public RSAUtils() {
    }

    private static Map<String, Object> genKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
        Map<String, Object> keyMap = new HashMap(2);
        keyMap.put("RSAPublicKey", publicKey);
        keyMap.put("RSAPrivateKey", privateKey);
        return keyMap;
    }

    private static String getPrivateKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key)keyMap.get("RSAPrivateKey");
        return Base64.getEncoder().encodeToString(key.getEncoded()).replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
    }

    private static String getPublicKey(Map<String, Object> keyMap) throws Exception {
        Key key = (Key)keyMap.get("RSAPublicKey");
        return Base64.getEncoder().encodeToString(key.getEncoded()).replaceAll("\r\n", "").replaceAll("\r", "").replaceAll("\n", "");
    }

    private static String signByPrivateKey(String data, String privateKey, String algorithmMethod) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(algorithmMethod);
        signature.initSign(privateK);
        signature.update(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(signature.sign()).replaceAll("\n", "").replaceAll("\r\n", "");
    }

    public static String signByPrivateKey(String data, String privateKey) throws Exception {
        return signByPrivateKey(data, privateKey, "MD5withRSA");
    }

    private static boolean validateSignByPublicKey(String paramStr, String publicKey, String signedData, String algorithmMethod) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(algorithmMethod);
        signature.initVerify(publicK);
        signature.update(paramStr.getBytes("UTF-8"));
        return signature.verify(Base64.getDecoder().decode(signedData));
    }

    public static boolean validateSignByPublicKey(String paramStr, String publicKey, String signedData) throws Exception {
        return validateSignByPublicKey(paramStr, publicKey, signedData, "MD5withRSA");
    }

    private static String encryptByPublicKey(String data, String publicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey);
        byte[] dataBytes = data.getBytes("UTF-8");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(1, publicK);
        int inputLen = dataBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;

        for(int i = 0; inputLen - offSet > 0; offSet = i * 116) {
            byte[] cache;
            if (inputLen - offSet > 116) {
                cache = cipher.doFinal(dataBytes, offSet, 116);
            } else {
                cache = cipher.doFinal(dataBytes, offSet, inputLen - offSet);
            }

            out.write(cache, 0, cache.length);
            ++i;
        }

        byte[] encryptedData = out.toByteArray();
        out.close();
        return Base64.getEncoder().encodeToString(encryptedData).replaceAll("\n", "").replaceAll("\r\n", "");
    }

    public static String decryptByPrivateKey(String encryptedData, String privateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(2, privateK);
        int inputLen = encryptedBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;

        for(int i = 0; inputLen - offSet > 0; offSet = i * 128) {
            byte[] cache;
            if (inputLen - offSet > 128) {
                cache = cipher.doFinal(encryptedBytes, offSet, 128);
            } else {
                cache = cipher.doFinal(encryptedBytes, offSet, inputLen - offSet);
            }

            out.write(cache, 0, cache.length);
            ++i;
        }

        byte[] decryptedData = out.toByteArray();
        out.close();
        return new String(decryptedData, "UTF-8");
    }
}
