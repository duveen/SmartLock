package kr.o3selab.smartlock.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.bluetooth.ShakeyReceiver;
import kr.o3selab.smartlock.common.AppConfig;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.LoadingProgressDialog;
import kr.o3selab.smartlock.layouts.OptionsDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyShare;
import kr.o3selab.smartlock.models.ValueEventAdapter;
import kr.o3selab.smartlock.services.BLEService;
import kr.o3selab.smartlock.services.ShakeyServiceConnectionCallback;


public class MainActivity extends BaseActivity {

    public static final String TAG = "MainActivity";

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
    @BindView(R.id.fragment_shakey_image)
    ImageView shakeyIconView;
    @BindView(R.id.fragment_shakey_info)
    ImageView shakeyInfoView;
    @BindView(R.id.fragment_shakey_key)
    ImageView shakeyKeyView;
    @BindView(R.id.fragment_shakey_share)
    ImageView shakeyShareView;

    Context mContext;

    private Shakey mShakey;

    private FirebaseUser mUser;
    private FirebaseDatabase mFirebaseInstance;

    private ShakeyReceiver mMainReceiver;

    private LoadingProgressDialog readDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mContext = this;

        loading = new LoadingProgressDialog(this);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseInstance = FirebaseDatabase.getInstance();
        mMainReceiver = new ShakeyReceiver(shakeyReceiverCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mMainReceiver, BLEService.getIntentFilter());
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mMainReceiver);
    }

    @OnClick(R.id.activity_main_ic_menu)
    void menu() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @OnClick(R.id.menu_add_shakey)
    void addShakey() {
        mDrawerLayout.closeDrawer(GravityCompat.START);

        Intent intent = new Intent(MainActivity.this, DeviceAddActivity.class);
        startActivityForResult(intent, Extras.ADD_SHAKEY);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Debug.d("onActivityResult");
        if (requestCode == Extras.ADD_SHAKEY && resultCode == Extras.ADD_SHAKEY_OK) {
            updateShakey((Shakey) data.getSerializableExtra(Extras.SHAKEY));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void getShakeys() {
        Debug.d("GetShakeys");

        final Vector<Shakey> shakeys = AppConfig.getInstance().getShakeys();
        shakeys.clear();

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
                                        mShakeyList.addView(view);

                                        shakeys.add(shakey);
                                    }
                                });
                    }
                }
            }
        });

        mShakeyShareView.setVisibility(View.GONE);
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
            new OptionsDialog.Builder(MainActivity.this)
                    .setTitle("알림")
                    .setMessage(shakey.getName() + "와 지금 연결하시겠습니까?")
                    .setOptions(OptionsDialog.Options.YES_NO)
                    .setOnClickListener(onClickListener)
                    .putExtras(Extras.SHAKEY, shakey)
                    .show();
        }
    }

    OptionsDialog.OnClickListener onClickListener = new OptionsDialog.OnClickListener() {
        @Override
        public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
            dialog.dismiss();
            closeMenu();

            final Shakey shakey = (Shakey) dialog.getExtras(Extras.SHAKEY);

            if (mBleService != null && mBleService.isConnected() && options.equals(OptionsDialog.ANSWER.YES)) {
                if (mBleService.getConnectedBluetoothDevice().equals(shakey.getMac())) {
                    new OptionsDialog.Builder(MainActivity.this)
                            .setOptions(OptionsDialog.Options.YES)
                            .setTitle("알림")
                            .setMessage("이미 연결되어 있는 장치입니다.")
                            .setOnClickListener(new OptionsDialog.OnClickListener() {
                                @Override
                                public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                                    dialog.dismiss();
                                }
                            }).show();
                    return;
                }

                mBleService.disconnect();
                mBleService.close();
            }

            drawShakeyInfo(shakey);

            if (options.equals(OptionsDialog.ANSWER.YES)) {
                if (!loading.isShowing()) loading.show();

                if (mBleService == null) {
                    setShakeyServiceConnectionCallback(new ShakeyServiceConnectionCallback() {
                        @Override
                        public void onServiceConnected(BLEService service) {
                            updateShakey(shakey);
                            service.connect(mShakey.getMac());
                        }

                        @Override
                        public void onServiceDisconnected() {

                        }
                    });

                    Intent bleIntent = new Intent(MainActivity.this, BLEService.class);
                    startService(bleIntent);
                    bindService(bleIntent, getServiceConnection(), BIND_AUTO_CREATE);
                } else {
                    updateShakey(shakey);
                    mBleService.connect(mShakey.getMac());
                }
            }
        }
    };

    ShakeyReceiver.Callback shakeyReceiverCallback = new ShakeyReceiver.Callback() {
        @Override
        public void onConnect() {
            Debug.d("BLE_CONNECTED");
            if (loading.isShowing()) loading.dismiss();

            mBleService.setShakey(mShakey);

            shakeyKeyView.clearColorFilter();
            shakeyKeyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openShakey();
                }
            });

            shakeyIconView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.shakey_close));
        }

        @Override
        public void onDisconnect() {
            Debug.d("BLE_DISCONNECTED");

            if (loading.isShowing()) {
                new OptionsDialog.Builder(mContext)
                        .setTitle("연결실패")
                        .setMessage("Shakey 연결에 실패했습니다.")
                        .setOptions(OptionsDialog.Options.YES)
                        .setOnClickListener(new OptionsDialog.OnClickListener() {
                            @Override
                            public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                                dialog.dismiss();
                            }
                        }).show();
                loading.dismiss();
            }

            shakeyKeyView.setOnClickListener(null);
            shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));

            shakeyIconView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.shakey_disconnected));
        }

        @Override
        public void onDataAvailable(String data) {
            Debug.d("BLE Receive: " + data);

            if (data == null) return;

            String command = data.substring(0, 2);

            if (command.equals("R2")) {
                changeShakeySensValueResponse(mBleService.getShakey(), Integer.parseInt(data.substring(2, 3)));
            } else if (command.equals("A0")) {
                removeShakeyResponse(mBleService.getShakey());
            }
        }
    };

    private void drawShakeyInfo(final Shakey shakey) {

        if (shakey == null) return;

        shakeyInfoNameView.setText(shakey.getName());
        shakeyLastOpenView.setText("마지막으로 연 시간 : " + shakey.getLastOpen(true));
        shakeyInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShakeyOptions();
            }
        });

        shakeyShareView.clearColorFilter();
        shakeyShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUser.getUid().equals(shakey.getOwner())) {
                    Intent intent = new Intent(MainActivity.this, DeviceShareActivity.class);
                    intent.putExtra(Extras.SHAKEY, shakey);
                    startActivity(intent);

                    return;
                }

                Toast.makeText(mContext, "자물쇠의 소유자만 공유가 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShakeyOptions() {
        ArrayList<String> menuList = new ArrayList<>();

        menuList.add("정보 확인");
        menuList.add("이름 변경");

        if (mBleService != null && mBleService.isConnected()) {
            menuList.add("민감도 값 설정");
            if (mBleService.getShakey().getOwner().equals(mUser.getUid()))
                menuList.add("Shakey 삭제");
        }

        new AlertDialog.Builder(this)
                .setItems(menuList.toArray(new String[menuList.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                showShakeyInfo();
                                break;
                            case 1:
                                changeShakeyName(mShakey);
                                break;
                            case 2:
                                changeShakeySensValueRequest(mShakey);
                                break;
                            case 3:
                                removeShakeyRequest(mShakey);
                                break;
                            default:
                                break;
                        }
                    }
                }).show();
    }

    private void showShakeyInfo() {
        Intent intent = new Intent(this, DeviceInfoActivity.class);
        intent.putExtra(Extras.SHAKEY, mShakey);
        startActivity(intent);
    }

    private void changeShakeyName(final Shakey shakey) {
        new OptionsDialog.Builder(this)
                .setTitle("이름 변경")
                .setType(OptionsDialog.EDIT_TEXT_TYPE)
                .setOptions(OptionsDialog.Options.YES_NO)
                .setHint(shakey.getName())
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .setOnClickListener(new OptionsDialog.OnClickListener() {
                    @Override
                    public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                        dialog.dismiss();

                        if (options.equals(OptionsDialog.ANSWER.YES)) {
                            String name = dialog.getEditTextMessage();
                            if (name == null || name.equals("")) {
                                Toast.makeText(mContext, "이름을 입력하지 않아서 변경되지 않았습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final LoadingProgressDialog loadingProgressDialog = new LoadingProgressDialog(mContext);
                            loadingProgressDialog.show();

                            shakey.setName(name);
                            shakey.updateShakey(new Shakey.ShakeyUpdateListener() {
                                @Override
                                public void onSuccessed(Object o) {
                                    loadingProgressDialog.dismiss();
                                    updateShakey(shakey);
                                    updateUI();
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    loadingProgressDialog.dismiss();
                                    Toast.makeText(mContext, "변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).show();
    }

    private void changeShakeySensValueRequest(Shakey shakey) {
        mBleService.send(shakey.readSensValueCommand());

        readDialog = new LoadingProgressDialog(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (readDialog.isShowing()) {
                    readDialog.dismiss();
                    new OptionsDialog.Builder(mContext)
                            .setTitle("알림")
                            .setMessage("데이터 수신에 실패했습니다. 연결 상태를 확인해주세요.")
                            .show();
                }
            }
        }, 5000);
    }

    private void changeShakeySensValueResponse(final Shakey shakey, int value) {
        readDialog.dismiss();

        new OptionsDialog.Builder(this)
                .setType(OptionsDialog.SEEK_BAR_TYPE)
                .setOptions(OptionsDialog.Options.YES_NO)
                .setTitle("민감도 값 설정")
                .setSeekbarMin(1)
                .setSeekbarMax(5)
                .setSeekbarValue(value)
                .setOnClickListener(new OptionsDialog.OnClickListener() {
                    @Override
                    public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                        dialog.dismiss();

                        if (options.equals(OptionsDialog.ANSWER.NO)) return;

                        int value = dialog.getSeekbar().getProgress();
                        mBleService.send(shakey.writeSensValueCommand(value));
                    }
                }).show();
    }

    private void removeShakeyRequest(final Shakey shakey) {
        new OptionsDialog.Builder(this)
                .setType(OptionsDialog.TEXT_TYPE)
                .setTitle("경고")
                .setMessage("정말로 Shakey를 삭제하시겠습니까?")
                .setOptions(OptionsDialog.Options.YES_NO)
                .setOnClickListener(new OptionsDialog.OnClickListener() {
                    @Override
                    public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                        dialog.dismiss();

                        if (options.equals(OptionsDialog.ANSWER.NO)) return;

                        readDialog = new LoadingProgressDialog(mContext);
                        readDialog.show();

                        mBleService.send(shakey.resetCommand());
                    }
                }).show();
    }

    private void removeShakeyResponse(final Shakey shakey) {
        // User 목록에서 Shakey 삭제
        final DatabaseReference ownerReference = FirebaseDatabase.getInstance().getReference("Owner/" + mUser.getUid());
        ownerReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<ArrayList<String>> indicator = new GenericTypeIndicator<ArrayList<String>>() {
                };
                ArrayList<String> list = dataSnapshot.getValue(indicator);
                list.remove(shakey.getSecret());
                ownerReference.setValue(list).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        readDialog.dismiss();

                        Intent intent = new Intent();
                        intent.setAction(BLEService.BLE_DISCONNECTED);
                        sendBroadcast(intent);

                        mBleService.disconnect();
                        mBleService.close();

                        updateShakey(null);
                        updateUI();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Shakey 삭제
        FirebaseDatabase.getInstance().getReference("Shakeys/" + shakey.getSecret()).removeValue();

        // Shakey 로그 삭제
        FirebaseDatabase.getInstance().getReference("Logs/" + shakey.getSecret()).removeValue();
    }

    @Override
    public void openShakey() {
        super.openShakey();
        drawShakeyInfo(mBleService.getShakey());
    }

    private void closeMenu() {
        mDrawerLayout.closeDrawer(Gravity.START);
    }


    public void updateUI() {

        getShakeys();

        if (mBleService != null && mBleService.isConnected()) {
            drawShakeyInfo(mBleService.getShakey());
        } else if (mShakey != null) {
            drawShakeyInfo(mShakey);
        } else {
            shakeyInfoNameView.setText("연결된 Shakey 없음");

            shakeyInfoView.setOnClickListener(null);

            shakeyIconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.shakey_disconnected));

            shakeyShareView.setOnClickListener(null);
            shakeyShareView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));

            shakeyKeyView.setOnClickListener(null);
            shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));
        }
    }

    private void updateShakey(Shakey shakey) {
        mShakey = shakey;
        if (mBleService != null && mBleService.isConnected()) mBleService.setShakey(shakey);
    }

    @Override
    protected void onDestroy() {

        if (mBleService != null && AppConfig.getInstance().isAutoStart()) {
            mBleService.startMonitorSystem();

        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) closeMenu();
        else super.onBackPressed();
    }
}
