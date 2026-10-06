package com.jk.nihongolock;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class ApiKeyStore {
    private static final String PREFS = "api_secret_v1";
    private static final String ALIAS = "nihongo_lock_openai_key_v1";
    private static final String K_CIPHER = "cipher";
    private static final String K_IV = "iv";
    private static final String K_MODEL = "model";
    private final Context context;
    private final SharedPreferences prefs;

    public ApiKeyStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    public synchronized void saveApiKey(String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            deleteApiKey();
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
        prefs.edit()
                .putString(K_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(K_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    public synchronized String getApiKey() {
        try {
            String enc = prefs.getString(K_CIPHER, null);
            String iv = prefs.getString(K_IV, null);
            if (enc == null || iv == null) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP));
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec);
            byte[] plain = cipher.doFinal(Base64.decode(enc, Base64.NO_WRAP));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void deleteApiKey() {
        prefs.edit().remove(K_CIPHER).remove(K_IV).apply();
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS);
        } catch (Exception ignored) {}
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }

    public void setModel(String model) {
        String m = model == null ? "" : model.trim();
        if (m.isEmpty()) m = "gpt-6-luna";
        prefs.edit().putString(K_MODEL, m).apply();
    }

    public String getModel() {
        return prefs.getString(K_MODEL, "gpt-6-luna");
    }

    public String maskedKey() {
        String k = getApiKey();
        if (k == null || k.length() < 10) return "저장된 키 없음";
        return k.substring(0, Math.min(7, k.length())) + "••••••" + k.substring(k.length() - 4);
    }
}
