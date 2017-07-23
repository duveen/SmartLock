package kr.o3selab.smartlock.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;

import org.json.JSONObject;

import kr.o3selab.smartlock.common.API;
import kr.o3selab.smartlock.common.JSONHandler;
import kr.o3selab.smartlock.R;

public class RegisterActivity extends BaseActivity {

    public static final String TAG = "RegisterActivity";

    private TextSwitcher textSwitcher;
    private FrameLayout naverLoginButton;
    private Context mContext;

    private static String OAUTH_CLIENT_ID = "UVKcQ8o3zSbjWeVsVJm6";
    private static String OAUTH_CLIENT_SECRET = "KJb5YfZF9k";
    private static String OAUTH_CLIENT_NAME = "SHAKEY:스마트자물쇠";

    private OAuthLogin mOAuthLoginInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mContext = this;

        naverInitData();

        naverLoginButton = (FrameLayout) findViewById(R.id.register_naver_login);
        textSwitcher = (TextSwitcher) findViewById(R.id.register_text_switcher);
        textSwitcher.setInAnimation(this, R.anim.slide_in_left);
        textSwitcher.setOutAnimation(this, R.anim.slide_out_right);

        TextView inView = new TextView(this);
        inView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        inView.setTextColor(Color.WHITE);
        inView.setGravity(Gravity.CENTER);

        TextView outView = new TextView(this);
        outView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        outView.setTextColor(Color.WHITE);
        outView.setGravity(Gravity.CENTER);

        textSwitcher.addView(inView);
        textSwitcher.addView(outView);

        new TextThread().start();
    }

    class TextThread extends Thread {
        @Override
        public void run() {
            String[] array = {"SHAKEY에 오신것을 환영합니다!", "여러분을 편리함의 세계로 모시겠습니다!", "신기한 경험 지금 시작하세요!"};
            for (final String anArray : array) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textSwitcher.setText(anArray);
                    }
                });

                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    naverLoginButton.setVisibility(View.VISIBLE);
                    naverLoginButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mOAuthLoginInstance.startOauthLoginActivity(RegisterActivity.this, mOAuthLoginHandler);
                        }
                    });
                }
            });
        }
    }

    private void naverInitData() {
        mOAuthLoginInstance = OAuthLogin.getInstance();
        mOAuthLoginInstance.init(this, OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET, OAUTH_CLIENT_NAME);
    }

    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            if (success) {
                try {
                    String userInfo = new RequestNaverApi().execute().get();
                    JSONObject jsonObject = new JSONObject(userInfo);
                    JSONObject jsonResult = jsonObject.getJSONObject("response");

                    String phone = getPhoneInfo();
                    String phoneId = jsonResult.getString("id");
                    Log.d(TAG, phone + ", " + phoneId);

                    String param = "mPhone=" + phone + "&mPhoneId=" + phoneId + "&type=NAVER";
                    String result = new JSONHandler(API.SET_USER_INFO, param).execute().get();
                    Log.d(TAG, result);

                    if (result.contains("TRUE")) {
                        SharedPreferences.Editor editor = common.getEditor();
                        editor.putBoolean(common.REGISTER, true);
                        editor.putString(common.NAVER_ID, phoneId);
                        editor.commit();

                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        RegisterActivity.this.finish();
                    } else if (result.contains("Duplicate")) {
                        SharedPreferences.Editor editor = common.getEditor();
                        editor.putBoolean(common.REGISTER, true);
                        editor.putString(common.NAVER_ID, phoneId);
                        editor.commit();

                        Toast.makeText(mContext, "알림: 이미 가입되어 있는 아이디 입니다.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        RegisterActivity.this.finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "에러 발생! 관리자에게 문의하세요!", Toast.LENGTH_SHORT).show();
                        RegisterActivity.this.finish();
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

            } else {
                // 로그인 실패시
                Log.e(TAG, "로그인 실패");
            }
        }
    };

    private class RequestNaverApi extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String url = "https://openapi.naver.com/v1/nid/me";
            String at = mOAuthLoginInstance.getAccessToken(mContext);
            String result = mOAuthLoginInstance.requestApi(mContext, at, url);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    private String getPhoneInfo() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String phone = telephonyManager.getLine1Number();

        if (phone == null) {
            if (common.debug) phone = "01000000000";
            else {
                Toast.makeText(mContext, "USIM이 장착되지 않은 휴대폰은 사용이 어렵습니다. 죄송합니다.", Toast.LENGTH_SHORT).show();
                RegisterActivity.this.finish();
            }
        }

        if (phone != null && phone.contains("+82")) phone = phone.replace("+82", "0");

        return phone;
    }
}
