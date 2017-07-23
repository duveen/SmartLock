package kr.o3selab.smartlock.activities;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.API;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Common;
import kr.o3selab.smartlock.common.Constants;
import kr.o3selab.smartlock.common.JSONHandler;
import kr.o3selab.smartlock.common.Shakey;
import kr.o3selab.smartlock.layout.NoShakeyLayout;
import kr.o3selab.smartlock.layout.ShakeyLayout;
import kr.o3selab.smartlock.service.BTCTemplateService;


public class MainActivity extends BaseActivity {

    Context mContext;
    private BTCTemplateService myService;
    public static final String TAG = "MainActivity";
    private ActivityHandler mActivityHandler;

    DrawerLayout drawerLayout;

    TextView shakeyInfoNameView;
    TextView shakeyLastOpenView;
    ImageView shakeyInfoView;
    ImageView shakeyKeyView;
    ImageView shakeyShareView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mActivityHandler = new ActivityHandler();

        drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer);

        shakeyInfoNameView = (TextView) findViewById(R.id.fragment_shakey_name);
        shakeyLastOpenView = (TextView) findViewById(R.id.fragment_shakey_lastopen);
        shakeyInfoView = (ImageView) findViewById(R.id.fragment_shakey_info);
        shakeyKeyView = (ImageView) findViewById(R.id.fragment_shakey_key);
        shakeyShareView = (ImageView) findViewById(R.id.fragment_shakey_share);


        LinearLayout customerCenterMenu = (LinearLayout) findViewById(R.id.drawer_customer_center);
        customerCenterMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        LinearLayout refreshMenu = (LinearLayout) findViewById(R.id.drawer_customer_refresh);
        refreshMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshShakeys();
            }
        });

        initShakeys();
        doStartService();

        LinearLayout addShakey = (LinearLayout) findViewById(R.id.drawer_customer_addShakey);
        addShakey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(MainActivity.this, ShakeyConnectActivity.class);
                startActivity(intent);
            }
        });

        ImageView menuButton = (ImageView) findViewById(R.id.activity_main_ic_menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 서비스 등록
     */
    private void doStartService() {
        Log.d(TAG, "# Activity - doStartService()");
        startService(new Intent(this, BTCTemplateService.class));
        bindService(new Intent(this, BTCTemplateService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * 서비스 중지
     */
    private void doStopService() {
        Log.d(TAG, "# Activity - doStopService()");
        myService.finalizeService();
        stopService(new Intent(this, BTCTemplateService.class));
    }

    /**
     * 서비스 연결
     */
    private ServiceConnection mServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "Activity - Service connected");

            myService = ((BTCTemplateService.ServiceBinder) binder).getService();
            common.service = myService;
            initialize();
        }

        public void onServiceDisconnected(ComponentName className) {
            myService = null;
        }
    };


    private void initialize() {
        Toast.makeText(this, "Initialize 시작", Toast.LENGTH_SHORT).show();
        //ble지원 기기 인지 확인
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        //######서비스 실행부분!!!!!!
        myService.setupService(mActivityHandler);

        //블루투스 꺼져잇을경우 켜기
        if (!myService.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
    }


    public void initShakeys() {
        Log.d(TAG, "initShakeys");
        LinearLayout shakesList = (LinearLayout) findViewById(R.id.shakey_list_layout);
        shakesList.removeAllViews();

        int index = 0;

        if (common.shakeys.size() == 0) {
            NoShakeyLayout noShakeyLayout = new NoShakeyLayout(mContext);
            shakesList.addView(noShakeyLayout);
        } else {
            for (final Shakey shakey : common.shakeys) {
                ShakeyLayout shakeyLayout = new ShakeyLayout(mContext, shakey);
                shakeyLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("연결알림")
                                .setMessage(shakey.getName() + "에 연결하시겠습니까?")
                                .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        shakeyInfoUpdate(shakey, Dialog.BUTTON_POSITIVE);
                                    }
                                })
                                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        shakeyInfoUpdate(shakey, Dialog.BUTTON_NEGATIVE);
                                    }
                                })
                                .show();
                    }
                });

                shakesList.addView(shakeyLayout, index++);

            }
        }
    }


    public void shakeyInfoUpdate(final Shakey shakey, int flag) {

        shakeyInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShakeyInfoActivity.class);
                intent.putExtra("info", shakey);

                startActivity(intent);
            }
        });
        shakeyLastOpenView.setText("마지막으로 연 시간 : " + shakey.getLastopen());

        switch(flag) {
            case Dialog.BUTTON_NEGATIVE:
                shakeyKeyView.setOnClickListener(null);
                shakeyShareView.setOnClickListener(null);
                break;

            case Dialog.BUTTON_POSITIVE:
                AppSettings.setSettingsValue(AppSettings.SETTINGS_SECRETKEY, false, 0, shakey.getMac() + "$$" + shakey.getSecret());
                myService.connectDevice(shakey.getMac());
                common.service = myService;
                shakeyKeyView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        myService.sendMessageToRemote(shakey.getSecret());
                    }
                });

                shakeyShareView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(mContext, "개발 중", Toast.LENGTH_SHORT).show();
                    }
                });
                break;

            }
        drawerLayout.closeDrawer(Gravity.START);

    }


    public void shakeyUnConnected() {
        shakeyInfoNameView.setText("연결된 Shakey 없음");
        shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));
        shakeyShareView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));
        shakeyInfoView.setOnClickListener(null);
    }

    public void refreshShakeys() {

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("로딩중");
        pd.setCancelable(false);
        pd.setMessage("정보를 가져오고 있습니다.");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();

        Log.d(TAG, "refreshShakeys");
        common.shakeys.clear();
        SharedPreferences sharedPreferences = common.getSharedPreferences();
        String phoneId = sharedPreferences.getString(Common.NAVER_ID, "null");
        Log.d(TAG, phoneId);
        if (!phoneId.equals("null")) {
            try {
                String param = "userId=" + phoneId;
                String result = new JSONHandler(API.GET_SHAKEY_LIST, param).execute().get();

                Log.d(TAG, result);

                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArray = jsonObject.getJSONArray("result");

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject row = jsonArray.getJSONObject(i);
                    Log.d(TAG, row.toString());

                    String userId = row.getString("userId");
                    String secret = row.getString("secret");
                    String name = row.getString("name");
                    String mac = row.getString("mac");
                    String firstregister = row.getString("firstregister");
                    String lastopen = row.getString("lastopen");

                    common.shakeys.add(new Shakey(userId, secret, name, mac, firstregister, lastopen));
                }

                initShakeys();

            } catch (Exception e) {
                // Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        pd.dismiss();
    }


    /*****************************************************
     * Handler, Callback, Sub-classes
     ******************************************************/

    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_BT_STATE_INITIALIZED:
                    Toast.makeText(mContext, new String("초기화"), Toast.LENGTH_LONG).show();
                    break;

                case Constants.MESSAGE_BT_STATE_LISTENING:
                    Toast.makeText(mContext, new String("대기중"), Toast.LENGTH_LONG).show();
                    break;

                case Constants.MESSAGE_BT_STATE_CONNECTING:
                    Toast.makeText(mContext, new String("연결중"), Toast.LENGTH_LONG).show();
                    break;

                case Constants.MESSAGE_BT_STATE_CONNECTED:
                    if (myService != null) {
                        String deviceName = myService.getDeviceName();
                        if (deviceName != null) {
                            Toast.makeText(mContext, new String("블루투스 장치 : " + "연결됨" + " " + deviceName), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mContext, new String("블루투스 장치 : " + "연결됨" + " " + "No Name"), Toast.LENGTH_LONG).show();
                        }
                    }
                    break;

                case Constants.MESSAGE_BT_STATE_ERROR:
                    Toast.makeText(mContext, new String("에러"), Toast.LENGTH_LONG).show();
                    break;

                case Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED:
                    Toast.makeText(mContext, new String("커맨드 에러"), Toast.LENGTH_LONG).show();
                    break;

                case Constants.MESSAGE_READ_CHAT_DATA:
                    if (msg.obj != null) {
                        Log.d(TAG, (String) msg.obj);
                        Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG).show();
                    } else {
                        Log.d(TAG, "수신 메세지 널");
                    }
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
