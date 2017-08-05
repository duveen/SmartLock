package kr.o3selab.smartlock.activities;


import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Constants;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.OptionsDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyLog;
import kr.o3selab.smartlock.models.ShakeyShare;
import kr.o3selab.smartlock.models.ValueEventAdapter;
import kr.o3selab.smartlock.service.BTCTemplateService;


public class MainActivity extends BaseActivity {

    public static final String TAG = "MainActivity";

    Context mContext;
    private BTCTemplateService myService;
    private ActivityHandler mActivityHandler;

    private FirebaseUser mUser;
    private FirebaseDatabase mFirebaseInstance;

    @BindView(R.id.main_drawer)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.menu_shakey_list)
    LinearLayout mShakeyList;
    @BindView(R.id.menu_shakey_share)
    View mShakeyShareView;
    @BindView(R.id.menu_shakey_share_list)
    LinearLayout mShakeyShareList;

    @BindView(R.id.fragment_shakey_name)
    TextView shakeyInfoNameView;
    @BindView(R.id.fragment_shakey_lastopen)
    TextView shakeyLastOpenView;
    @BindView(R.id.fragment_shakey_info)
    ImageView shakeyInfoView;
    @BindView(R.id.fragment_shakey_key)
    ImageView shakeyKeyView;
    @BindView(R.id.fragment_shakey_share)
    ImageView shakeyShareView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mContext = this;
        mActivityHandler = new ActivityHandler();

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseInstance = FirebaseDatabase.getInstance();

        shakeyUnConnected();

        getShakeys();
        // doStartService();
    }

    @OnClick(R.id.activity_main_ic_menu)
    void menu() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @OnClick(R.id.menu_add_shakey)
    void addShakey() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        Intent intent = new Intent(MainActivity.this, ShakeyConnectActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.menu_refresh_shakey)
    void refreshShakeys() {
        Debug.d("RefreshShakeys");
        getShakeys();
    }

    @OnClick(R.id.menu_setup)
    void setup() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        startActivity(new Intent(MainActivity.this, SetupActivity.class));
    }

    public void getShakeys() {
        Debug.d("GetShakeys");

        mShakeyList.removeAllViewsInLayout();

        DatabaseReference reference = mFirebaseInstance.getReference("Owner/" + mUser.getUid());
        reference.addListenerForSingleValueEvent(new ValueEventAdapter(this) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> list = (ArrayList<String>) dataSnapshot.getValue();
                if (list == null) {
                    View view = getLayoutInflater().inflate(R.layout.drawer_menu_no_shakey, null);
                    mShakeyList.addView(view);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        final int index = i;
                        mFirebaseInstance
                                .getReference("Shakeys/" + list.get(i))
                                .addListenerForSingleValueEvent(new ValueEventAdapter(MainActivity.this) {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Shakey shakey = dataSnapshot.getValue(Shakey.class);
                                        View view = getShakeyItemView(shakey);
                                        mShakeyList.addView(view, index);
                                    }
                                });
                    }
                }
            }
        });

        mShakeyShareList.removeAllViewsInLayout();
        reference = mFirebaseInstance.getReference("Shares/" + mUser.getUid());
        reference.addListenerForSingleValueEvent(new ValueEventAdapter(this) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    mShakeyShareView.setVisibility(View.GONE);
                    return;
                }

                mShakeyShareView.setVisibility(View.VISIBLE);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ShakeyShare shakeyShare = snapshot.getValue(ShakeyShare.class);
                    shakeyShare.asReceiveData(MainActivity.this, new ShakeyShare.ShakeyCallBack() {
                        @Override
                        public void onSuccess(Shakey shakey) {
                            View view = getShakeyItemView(shakey);
                            mShakeyShareList.addView(view);
                        }
                    });
                }
            }
        });

        /*if

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
            }
        }*/
    }

    private View getShakeyItemView(Shakey shakey) {
        View view = getLayoutInflater().inflate(R.layout.drawer_menu_shakey, null);

        view.setOnClickListener(new ShakeyClickListener(shakey));
        view.setClickable(true);

        TextView name = (TextView) view.findViewById(R.id.menu_shakey_name_text);
        name.setText(shakey.getName());

        return view;
    }

    private class ShakeyClickListener implements View.OnClickListener {

        private Shakey shakey;

        private ShakeyClickListener(Shakey shakey) {
            this.shakey = shakey;
        }

        @Override
        public void onClick(View v) {
            new OptionsDialog.Builder(MainActivity.this, shakey)
                    .setTitle("알림")
                    .setMessage(shakey.getName() + "와 지금 연결하시겠습니까?")
                    .setOptions(OptionsDialog.Options.YES_NO)
                    .setOnClickListener(optionsDialogClickListener)
                    .show();
        }
    }

    OptionsDialog.OptionsDialogClickListener optionsDialogClickListener = new OptionsDialog.OptionsDialogClickListener() {
        @Override
        public void onClick(Dialog v, Shakey shakey, OptionsDialog.ANSWER options) {
            v.dismiss();
            closeMenu();

            drawShakeyInfo(shakey.getSecret());

            if (options.equals(OptionsDialog.ANSWER.YES)) {
                // 확인 눌렸을때
            }
        }
    };

    private void drawShakeyInfo(String secret) {
        FirebaseDatabase.getInstance().getReference("Shakeys/" + secret).addListenerForSingleValueEvent(new ValueEventAdapter(this) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Shakey shakey = dataSnapshot.getValue(Shakey.class);

                if (shakey == null) {
                    Toast.makeText(MainActivity.this, "Shakey 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    shakeyUnConnected();
                    return;
                }

                drawShakeyInfo(shakey);
            }
        });
    }

    private void drawShakeyInfo(final Shakey shakey) {

        shakeyInfoNameView.setText(shakey.getName());
        shakeyLastOpenView.setText("마지막으로 연 시간 : " + shakey.getLastOpen(true));
        shakeyInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShakeyInfoActivity(shakey);
            }
        });

        shakeyKeyView.clearColorFilter();
        shakeyKeyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateShakeyInfo(shakey);
            }
        });

        shakeyShareView.clearColorFilter();
        shakeyShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "개발중", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void updateShakeyInfo(Shakey shakey) {

        final long openTime = System.currentTimeMillis();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Logs/" + shakey.getSecret()).push();
        ShakeyLog log = new ShakeyLog();

        log.setWho(mUser.getUid());
        log.setEmail(mUser.getEmail());
        log.setRegdate(openTime);

        reference.setValue(log);

        shakey.setLastOpen(openTime);
        FirebaseDatabase.getInstance().getReference("Shakeys/" + shakey.getSecret()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Shakey shakey = mutableData.getValue(Shakey.class);
                if (shakey == null) {
                    return Transaction.success(mutableData);
                }

                shakey.setLastOpen(openTime);

                mutableData.setValue(shakey);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        drawShakeyInfo(shakey);
    }

    private void startShakeyInfoActivity(Shakey shakey) {
        Intent intent = new Intent(this, ShakeyInfoActivity.class);
        intent.putExtra(Extras.SHAKEY, shakey);
        startActivity(intent);
    }

    private void closeMenu() {
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) closeMenu();
        else super.onBackPressed();
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
            // common.service = myService;
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


    public void shakeyInfoUpdate(final Shakey shakey, int flag) {

        shakeyInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShakeyInfoActivity.class);
                intent.putExtra("info", shakey);

                startActivity(intent);
            }
        });

        shakeyLastOpenView.setText("마지막으로 연 시간 : " + shakey.getLastOpen());

        switch (flag) {
            case Dialog.BUTTON_NEGATIVE:
                shakeyKeyView.setOnClickListener(null);
                shakeyShareView.setOnClickListener(null);
                break;

            case Dialog.BUTTON_POSITIVE:
                AppSettings.setSettingsValue(AppSettings.SETTINGS_SECRETKEY, false, 0, shakey.getMac() + "$$" + shakey.getSecret());
                myService.connectDevice(shakey.getMac());
                // common.service = myService;
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
        mDrawerLayout.closeDrawer(Gravity.START);

    }


    public void shakeyUnConnected() {
        shakeyInfoNameView.setText("연결된 Shakey 없음");

        shakeyInfoView.setOnClickListener(null);

        shakeyShareView.setOnClickListener(null);
        shakeyShareView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));

        shakeyKeyView.setOnClickListener(null);
        shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));
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
