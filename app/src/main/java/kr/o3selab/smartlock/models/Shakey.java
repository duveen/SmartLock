package kr.o3selab.smartlock.models;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Shakey implements Serializable {
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

    private static String toDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        return sdf.format(new Date(time));
    }
}
