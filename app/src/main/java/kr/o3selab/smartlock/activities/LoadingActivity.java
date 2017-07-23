package kr.o3selab.smartlock.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.o3selab.smartlock.common.API;
import kr.o3selab.smartlock.common.JSONHandler;
import kr.o3selab.smartlock.common.Shakey;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.Utils;

public class LoadingActivity extends BaseActivity {

    public static final String TAG = "LoadingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        new TedPermission(LoadingActivity.this)
                .setPermissionListener(permissionlistener)
                .setRationaleMessage("SHAKEY 서비스를 이용하기 위한 권한입니다.")
                .setDeniedMessage("권한 미제공시 서비스 제공이 어렵습니다. 프로그램을 종료합니다. 다시 실행해주세요!")
                .setPermissions(Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE
                ).check();
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {

            SharedPreferences sharedPreferences = Utils.getSharedPreferences(LoadingActivity.this);
            Boolean register = sharedPreferences.getBoolean(common.REGISTER, false);

            if (register) {
                getShakeys();
            } else {
                startActivity(new Intent(LoadingActivity.this, RegisterActivity.class));
                LoadingActivity.this.finish();
            }

        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(LoadingActivity.this, "프로그램을 실행 할 수 없습니다. 다시 실행해주세요!", Toast.LENGTH_SHORT).show();
            LoadingActivity.this.finish();
        }
    };



    /* 자물쇠 정보 가져오기 */
    private void getShakeys() {
        SharedPreferences sharedPreferences = Utils.getSharedPreferences(LoadingActivity.this);
        String phoneId = sharedPreferences.getString(common.NAVER_ID, "null");
        if(!phoneId.equals("null")) {
            try {
                String param = "userId=" + phoneId;
                String result = new JSONHandler(API.GET_SHAKEY_LIST, param).execute().get();

                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArray = jsonObject.getJSONArray("result");

                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject row = jsonArray.getJSONObject(i);

                    String userId = row.getString("userId");
                    String secret = row.getString("secret");
                    String name = row.getString("name");
                    String mac = row.getString("mac");
                    String firstregister = row.getString("firstregister");
                    String lastopen = row.getString("lastopen");

                    common.shakeys.add(new Shakey(userId, secret, name, mac, firstregister, lastopen));
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        startActivity(new Intent(LoadingActivity.this, MainActivity.class));
        LoadingActivity.this.finish();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
