package com.de0.paselist;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import android.util.Base64;



// http://zuccyimemo.blog.fc2.com/blog-entry-1.html

public class Encryptor {
//	private static final String LOG_TAG = "Encryptor";

	/** 鍵長（byte単位） */
//	private static final int KEY_LENGTH_BYTES = 16;

	/** 鍵長（bit単位） */
//	private static final int KEY_LENGTH_BITS = KEY_LENGTH_BYTES * 8;

	/**
	 * ランダムキーを生成する
	 */
	/** ソルト値 */
	private static final byte[] SALT = new byte[] {
		-87, 26, 102, -120, -2, -1, 29, 69, 50, 15, -31, 22, 74, -106};

	/**
	 * 共通鍵の生成
	 */
	private static SecretKey generateKey(String keyStr) throws  NoSuchAlgorithmException, InvalidKeySpecException {

		/* 共通鍵を作成するためのパスワード */
		final String passWordStr = "P.A32]/SWb94>;;u0" +keyStr+ "13f[[322]gaaOR7D";
		char[] password = passWordStr.toCharArray();

		/* 鍵仕様を生成 */
		KeySpec keySpec = new PBEKeySpec(password, SALT, 1024, 256);

		/* 秘密鍵ファクトリ */
		SecretKeyFactory factory
		= SecretKeyFactory.getInstance("PBEWITHSHAAND256BITAES-CBC-BC");

		/* 共通鍵を取得 */
		SecretKey secretKey = factory.generateSecret(keySpec);

		return secretKey;
	}




    /** 初期ベクトル */
    private static final byte[] INITAIL_VECTOR = {
        16, 74, 71, -80, 32, 101, -47, 72, 117, -14, 0, -29, 70, 65, -12};
    /** 暗号アルゴリズム */
    private static final String CRYPER_ALGORITM = "AES/CBC/PKCS5Padding";

    /**
     * 暗号化した文字列を取得する
     */
    public static String getEncryptedStr(final String inStr,final String keyStr){

        /* 暗号済文字列 */
        String encryptedStr = "";
        /* 共通鍵 */
        SecretKey secretKey = null;
        try {
            /* 共通鍵の生成 */
            secretKey = generateKey(keyStr);

            /* 共通鍵を使って暗号化 */
            byte[] encrypted = encrypt(inStr.getBytes(), secretKey);

            /* BASE64でエンコードする */
            encryptedStr
                     = Base64.encodeToString(encrypted, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeySpecException e) {
        } catch (InvalidKeyException e) {
        } catch (NoSuchPaddingException e) {
        } catch (InvalidAlgorithmParameterException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (BadPaddingException e) {
        } finally {
            /* 処理なし */
        }
        return encryptedStr;
    }

    /**
     * 暗号化処理
     */
    private static byte[] encrypt(final byte[] src, final SecretKey key)
                            throws  NoSuchAlgorithmException,
                                    NoSuchPaddingException,
                                    InvalidKeyException,
                                    InvalidAlgorithmParameterException,
                                    IllegalBlockSizeException,
                                    BadPaddingException {

        /* 暗号アルゴリズムにAESを指定 */
        Cipher cipher = Cipher.getInstance(CRYPER_ALGORITM);
        /* 暗号化モードに設定し、Keyを指定 */
        cipher.init(Cipher.ENCRYPT_MODE,
                        key,
                        new IvParameterSpec(INITAIL_VECTOR));
        /* 暗号の実行 */
        byte[] encrypted = cipher.doFinal(src);
        return encrypted;
    }


	//複合ここから
/*
	// 初期ベクトル
	private static final byte[] INITAIL_VECTOR = {
		116, 84, 11, -8, 12, 19, -47, -72, 119, -104, 0, 69, 27, 95, -122};
*/
    /**
     * 復号した文字列を取得する
     */
    public static String getDecryptStr(final String encryptedStr,final String keyStr) {

        /* 復号済文字列 */
        String decryptedStr = "";
        /* 共通鍵 */
        SecretKey secretKey = null;

        try {
            /* 共通鍵の生成 */
            secretKey = generateKey(keyStr);

            /* BASE64でデコード */
            byte[] crypted = null;
            crypted = Base64.decode(encryptedStr, Base64.DEFAULT);

            /* 復号 */
            byte[] decrypted = decrypt(crypted, secretKey);
            decryptedStr = new String(decrypted);

        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeySpecException e) {
        } catch (InvalidKeyException e) {
        } catch (NoSuchPaddingException e) {
        } catch (InvalidAlgorithmParameterException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (BadPaddingException e) {
        } finally {
            /* 処理なし */
        }
        return decryptedStr;
    }


    /**
     * 復号処理
     */
    private static byte[] decrypt(byte[] src, SecretKey secretKey)
                            throws NoSuchAlgorithmException,
                                    NoSuchPaddingException,
                                    InvalidKeyException,
                                    InvalidAlgorithmParameterException,
                                    IllegalBlockSizeException,
                                    BadPaddingException {

        /* 復号アルゴリズムにAESを設定 */
        Cipher cipher = Cipher.getInstance(CRYPER_ALGORITM);

        /* 復号モードに設定し、鍵を指定 */
        cipher.init(Cipher.DECRYPT_MODE,
                        secretKey,
                        new IvParameterSpec(INITAIL_VECTOR));

        /* 復号 */
        byte[] decrypted = cipher.doFinal(src);

        return decrypted;
    }


}