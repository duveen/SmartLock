/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.o3selab.smartlock.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.StringTokenizer;

import kr.o3selab.smartlock.bluetooth.ConnectionInfo;

public class AppSettings {

    // Constants
    public static final int SETTINGS_BACKGROUND_SERVICE = 1;
    public static final int SETTINGS_SECRETKEY = 2;
    public static final int SETTINGS_AUTOCONNECT = 3;
    public static final int SETTINGS_RESPONSIVENESS = 4;
    public static final int SETTINGS_NOTI=5;

    public static int GATT_SUCCEESS = 0;
    public static int GATT_SENDMESSAGE_RECEIVED = 0;


    private static boolean mIsInitialized = false;
    private static Context mContext;

    // Setting values
    private static boolean mUseBackgroundService;
    private static boolean mUseAutoConnect;
    private static boolean mUseNoti;


    public static void initializeAppSettings(Context c) {
        if (mIsInitialized)
            return;
        mContext = c;

        // 환경 설정 값 로드
        mUseBackgroundService = loadBgService();
        mUseNoti = loadNotiSetting();

        mIsInitialized = true;
    }

    // 세팅 값 저장
    public static void setSettingsValue(int type, boolean boolValue, int intValue, String stringValue) {
        if (mContext == null)
            return;

        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        switch (type) {

            case SETTINGS_BACKGROUND_SERVICE://백그라운드 서비스 등록 저장
                editor.putBoolean(Constants.PREFERENCE_KEY_BG_SERVICE, boolValue);
                editor.commit();
                mUseBackgroundService = boolValue;
                break;

            case SETTINGS_SECRETKEY://시크릿키 저장?
                Log.d("secret", stringValue);
                StringTokenizer str = new StringTokenizer(stringValue, "$$");
                Log.d("strcount", str.countTokens()+"");
                if(str.countTokens() == 2) {
                    String mac = str.nextToken();
                    Log.d("mac", mac);
                    String key = str.nextToken();
                    Log.d("key", key);
                    editor.putString(mac, key);
                    editor.commit();

                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("config", Context.MODE_PRIVATE);
                    String userId = sharedPreferences.getString(Common.NAVER_ID, "error");

                    String param = "uId=" + userId + "&secret=" + key + "&name=" + "name" + "&mac=" + mac;
                    Common.registerShakeyParam = param;

                    Log.d("AppSetting", "시크릿키 정상 저장됨");
                } else {
                    Log.e("AppSetting", "시크릿키 저장 실패");
                }


                break;
            case SETTINGS_NOTI:
                editor.putBoolean(Constants.PREFERENCE_KEY_NOTI,boolValue);
                editor.commit();
                mUseNoti = boolValue;
                break;
            // 자동연결 셋 저장
            case SETTINGS_AUTOCONNECT:
                editor.putBoolean(Constants.PREFERENCE_KEY_AUTO_CONNECT, boolValue);
                editor.commit();
                mUseAutoConnect = boolValue;
                break;

            // 민감도 설정
            case SETTINGS_RESPONSIVENESS:
                editor.putInt(Constants.PREFERENCE_RESPONSIVENESS,intValue);
                editor.commit();
                break;

            default:
                editor.commit();
                break;
        }
    }

    /**
     * Load 'Run in background' setting value from preferences
     *
     * @return boolean        is true
     */
    public static boolean loadBgService() {
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        boolean isTrue = prefs.getBoolean(Constants.PREFERENCE_KEY_BG_SERVICE, false);
        return isTrue;
    }

    public static boolean loadAutoConnectSetting() {
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        boolean isTrue = prefs.getBoolean(Constants.PREFERENCE_KEY_AUTO_CONNECT, false);
        return isTrue;
    }

    public static String getSecretkey() {
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        ConnectionInfo con = ConnectionInfo.getInstance(mContext);
        if (con != null) {
            String key = prefs.getString(con.getDeviceAddress(), null);
            Log.d("AppSetting", "시크릿키 정상 로드됨");
            return key;
        }
        return null;
    }
    public static boolean loadNotiSetting() {
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        boolean isTrue = prefs.getBoolean(Constants.PREFERENCE_KEY_NOTI, false);
        return isTrue;
    }
    public static int getResponsiveness(){
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
        int respon = prefs.getInt(Constants.PREFERENCE_RESPONSIVENESS,0);

        return respon;
    }

    /**
     * Returns 'Run in background' setting value
     *
     * @return boolean        is true
     */
    public static boolean getBgService() {
        return mUseBackgroundService;
    }

    public static boolean getAutoConnectSetting() {
        return mUseAutoConnect;
    }

    public static boolean getNotiSetting() {return  mUseNoti;}

}
