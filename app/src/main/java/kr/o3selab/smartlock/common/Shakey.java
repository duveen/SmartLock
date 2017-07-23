package kr.o3selab.smartlock.common;

import java.io.Serializable;

/**
 * Created by mingyupark on 2016. 10. 29..
 */

public class Shakey implements Serializable {
    private String userId;
    private String secret;
    private String name;
    private String mac;
    private String firstregister;
    private String lastopen;

    public Shakey(){
    }

    public Shakey(String userId, String secret, String name, String mac, String firstregister, String lastopen) {
        this.userId = userId;
        this.secret = secret;
        this.name = name;
        this.mac = mac;
        this.firstregister = firstregister;
        this.lastopen = lastopen;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getFirstregister() {
        return firstregister;
    }

    public void setFirstregister(String firstregister) {
        this.firstregister = firstregister;
    }

    public String getLastopen() {
        return lastopen;
    }

    public void setLastopen(String lastopen) {
        this.lastopen = lastopen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
