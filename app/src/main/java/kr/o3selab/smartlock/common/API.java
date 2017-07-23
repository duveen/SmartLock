package kr.o3selab.smartlock.common;

/**
 * Created by duvee on 2016-10-24.
 */

public class API {
    public static final String BASE_URL = "http://smartlock.o3selab.kr/";
    public static final String API_URL = BASE_URL + "api/";
    public static final String SET_USER_INFO = API_URL + "setSmartLockRegister.php";
    public static final String GET_SHAKEY_LIST = API_URL + "getShakeyList.php";
    public static final String FIRST_SHAKEY_REGISTER = API_URL + "firstShakeyRegister.php";
    public static final String SEND_LOG = API_URL + "sendLog.php";

}
