package kr.o3selab.smartlock.common;

import java.util.Vector;

import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.services.BTCTemplateService;

public class Common {

    // 개발자 모드
    public boolean debug = true;

    // 버전 정보
    public double version = 1.0000;

    // 인스턴스 정보
    static Common common;

    public static void setCommon(Common inst) {
        common = inst;
    }

    public static Common getCommon() {
        return common;
    }

    // Shakey 정보
    public Vector<Shakey> shakeys = new Vector<>();

    public static final String REGISTER = "register"; // 가입여부
    public static final String NAVER_ID = "naverid"; // 네이버 아이디

    public BTCTemplateService service;

    // 쉐이키 등록 모드 = -1 초기상태, 0 진행상태, 1 등록실패, 2 등록성공
    public static int addShakeyStatus = -1;
    public static String registerShakeyParam;
}
