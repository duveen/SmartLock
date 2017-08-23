package kr.o3selab.smartlock.models;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import kr.o3selab.smartlock.common.ByteArrayBuffer;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.common.utils.HexAsciiHelper;

public class Shakey implements Serializable {

    private static String REQUEST_SECRET_KEY = "K0";
    private static String RESPONSE_RECEIVE_SECRET_KEY = "K1";
    private static String REQUEST_UNLOCK = "U0";
    private static String REQUEST_RESET = "U1";
    private static String SET_SENS_VALUE = "S0";

    private String owner;
    private String ownerEmail;
    private String secret;
    private String name;
    private String mac;
    private Long regdate;
    private Long lastOpen;

    public Shakey() {

    }

    @Override
    public String toString() {
        return "Shakey {" +
                "owner='" + owner + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", secret='" + secret + '\'' +
                ", name='" + name + '\'' +
                ", mac='" + mac + '\'' +
                ", regdate=" + regdate +
                ", lastOpen=" + lastOpen +
                '}';
    }

    public JSONObject toJSON() {
        HashMap<String, Object> json = new HashMap<>();
        json.put("owner", owner);
        json.put("ownerEmail", ownerEmail);
        json.put("secret", secret);
        json.put("name", name);
        json.put("mac", mac);
        json.put("regdate", regdate);
        json.put("lastOpen", lastOpen);

        return new JSONObject(json);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Long getRegdate() {
        return regdate;
    }

    public String getRegdate(boolean flag) {
        return toDate(getRegdate());
    }

    public void setRegdate(Long regdate) {
        this.regdate = regdate;
    }

    public Long getLastOpen() {
        return lastOpen;
    }

    public String getLastOpen(boolean flag) {
        return toDate(getLastOpen());
    }

    public void setLastOpen(Long lastOpen) {
        this.lastOpen = lastOpen;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public byte[] getENCSecretKey(String publicKey) {
        if (secret == null) return null;
        try {

            String signKey = "smunlockshakey" + publicKey;
            Debug.d(signKey);
            Debug.d(HexAsciiHelper.bytesToHex(signKey.getBytes()));
            SecretKeySpec secretKeySpec = new SecretKeySpec(signKey.getBytes(), "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] content = cipher.doFinal(secret.getBytes());
            Debug.d(String.valueOf(content.length));

            String aesKey = HexAsciiHelper.bytesToHex(content).replace("FFFFFF", "");
            Debug.d(aesKey);

            return content;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] requestSecretCommand() {
        return REQUEST_SECRET_KEY.getBytes();
    }

    public static byte[] responseReceiveSecretKeyCommand() {
        return RESPONSE_RECEIVE_SECRET_KEY.getBytes();
    }

    public byte[] unlockCommand() {
        String publicKey = String.format(Locale.KOREA, "%02d", new Random().nextInt(100));
        return ByteArrayBuffer.getBuffer().append(REQUEST_UNLOCK.getBytes()).append(publicKey.getBytes()).append(getENCSecretKey(publicKey)).toByteArray();
    }

    public byte[] resetCommand() {
        String publicKey = String.format(Locale.KOREA, "%02d", new Random().nextInt(100));
        return ByteArrayBuffer.getBuffer().append(REQUEST_RESET.getBytes()).append(publicKey.getBytes()).append(getENCSecretKey(publicKey)).toByteArray();
    }

    private static String toDate(Long time) {
        if (time == null) return "정보없음";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        return sdf.format(new Date(time));
    }
}
