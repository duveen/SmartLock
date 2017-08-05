package kr.o3selab.smartlock.models;

public class ShakeyLog {

    private String who;
    private String email;
    private Long regdate;

    public ShakeyLog() {

    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public Long getRegdate() {
        return regdate;
    }

    public void setRegdate(Long regdate) {
        this.regdate = regdate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
