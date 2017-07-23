package kr.o3selab.smartlock.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import kr.o3selab.smartlock.common.API;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Common;
import kr.o3selab.smartlock.common.Constants;
import kr.o3selab.smartlock.common.Logs;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.service.BTCTemplateService;

public class ShakeyConnectActivity extends BaseActivity {

    private BTCTemplateService myService;
    public static final String TAG = "ShakeyConnectActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_connect);

        myService = common.service;

        Button registerButton = (Button) findViewById(R.id.shakey_register_button);
        Button nextButton = (Button) findViewById(R.id.shakey_next_button);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ShakeyConnectActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShakeyConnectActivity.this.finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logs.d(TAG, "onActivityResult " + resultCode);
        AppSettings.GATT_SUCCEESS = 0;

        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                // 디바이스 리스트에서 요청된 디바이스로 연결
                if (resultCode == Activity.RESULT_OK) {
                    // 디바이스 어드레스로 인텐트에서 값 추출
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Logs.d(TAG, "연결할 mac 주소 :  " + address);
                    // 장치 연결 시도
                    if (myService == null) {
                        Log.d(TAG, "myService NULL임 ");
                    }
                    if (address != null && myService != null) {
                        Log.d(TAG, "Service 디바이스 연결 호출");
                        AppSettings.GATT_SUCCEESS = 0;
                        myService.connectDevice(address);
                        new GATTCheckTask().execute();
                    }
                }
                break;

            case Constants.REQUEST_ENABLE_BT:
                // 블루투스 반환을 요청하는 경우
                if (resultCode == Activity.RESULT_OK) {
                    // 블루투스는 지금, 사용하도록 설정된 BT 세션을 설정
                    myService.setupBLE();
                } else {

                    Logs.e(TAG, "블루투스가 꺼져있습니다");
                    Toast.makeText(this, "블루투스가 꺼져있습니다 설정에서 블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public class GATTCheckTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog pd;

        public GATTCheckTask() {
            pd = new ProgressDialog(ShakeyConnectActivity.this);
            pd.setTitle("연결중");
            pd.setMessage("자물쇠와 연결중입니다. 조금만 기다려주세요!");
            // pd.setCancelable(false);
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "pre");
            pd.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(TAG, "post");
            pd.dismiss();
            ShakeyConnectActivity.this.finish();
        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.d(TAG, "sendMessage");

            while (true) {
                if (AppSettings.GATT_SUCCEESS == 1) {
                    Log.d(TAG, "G" + AppSettings.GATT_SUCCEESS);
                    myService.sendMessageToRemote("s");
                    break;
                }
                if (AppSettings.GATT_SUCCEESS == 2) {
                    break;
                }
            }

            while (true) {
                if (Common.registerShakeyParam == null) continue;
                Log.d(TAG, Common.registerShakeyParam);

                if (Common.registerShakeyParam.equals("already")) {
                    Common.addShakeyStatus = 1;
                    break;
                }

                try {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                String url = API.FIRST_SHAKEY_REGISTER;
                                String param = Common.registerShakeyParam;

                                Log.d(TAG, API.FIRST_SHAKEY_REGISTER);
                                Log.d(TAG, Common.registerShakeyParam);

                                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                                con.setRequestMethod("POST");
                                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                                con.setDoOutput(true);
                                con.setDoInput(true);

                                if (param != null) {
                                    OutputStream os = con.getOutputStream();
                                    os.write(param.getBytes("euc-kr"));
                                    os.flush();
                                    os.close();
                                }

                                InputStream is = con.getInputStream();

                                BufferedReader br = new BufferedReader(new InputStreamReader(is, "euc-kr"));
                                StringBuilder sb = new StringBuilder();
                                String line;

                                con.getResponseCode();

                                while ((line = br.readLine()) != null) {
                                    sb.append(line);
                                }

                                String result = br.toString();

                                Common.registerShakeyParam = null;
                                Common.addShakeyStatus = 2;

                                is.close();
                                br.close();
                                Log.d(TAG, result);

                                if (result.contains("TRUE")) {
                                    Common.registerShakeyParam = null;
                                    Common.addShakeyStatus = 2;
                                } else if (result.contains("Error")) {
                                    Common.registerShakeyParam = null;
                                    Common.addShakeyStatus = 1;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }.start();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            while (true) {
                if (Common.addShakeyStatus == 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ShakeyConnectActivity.this, "이미 등록되어 있는 자물쇠 입니다. 다른 자물쇠를 등록해주세요!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                } else if (Common.addShakeyStatus == 2) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ShakeyConnectActivity.this, "등록 성공!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                }
            }

            return null;
        }
    }
}
