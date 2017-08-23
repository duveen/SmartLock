package kr.o3selab.smartlock.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
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
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.bluetooth.ShakeyReceiver;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.OptionsDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyAlert;
import kr.o3selab.smartlock.models.ShakeyLog;
import kr.o3selab.smartlock.models.ShakeyShare;
import kr.o3selab.smartlock.models.ValueEventAdapter;
import kr.o3selab.smartlock.services.BLEService;
import kr.o3selab.smartlock.services.ShakeyServiceConnectionCallback;


public class MainActivity extends BaseActivity {

    public static final String TAG = "MainActivity";

    Context mContext;

    private Shakey mShakey;

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

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseInstance = FirebaseDatabase.getInstance();

        getShakeys();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
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

            mShakey = (Shakey) dialog.getExtras(Extras.SHAKEY);
            drawShakeyInfo(mShakey.getSecret());

            if (options.equals(OptionsDialog.ANSWER.YES)) {
                setShakeyServiceConnectionCallback(new ShakeyServiceConnectionCallback() {
                    @Override
                    public void onServiceConnected(BLEService service) {
                        service.setShakeyReceiver(new ShakeyReceiver(shakeyReceiverCallback));
                        service.connect(mShakey.getMac());
                    }

                    @Override
                    public void onServiceDisconnected() {

                    }
                });

                Intent bleIntent = new Intent(MainActivity.this, BLEService.class);
                bindService(bleIntent, getServiceConnection(), BIND_AUTO_CREATE);
            }
        }
    };

    ShakeyReceiver.Callback shakeyReceiverCallback = new ShakeyReceiver.Callback() {
        @Override
        public void onConnect() {
            Debug.d("BLE_CONNECTED");

            shakeyKeyView.clearColorFilter();
            shakeyKeyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openShakey();
                }
            });
        }

        @Override
        public void onDisconnect() {
            Debug.d("BLE_DISCONNECTED");
            shakeyKeyView.setOnClickListener(null);
            shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));

            unbindService(getServiceConnection());
        }

        @Override
        public void onDataAvailable(String data) {
            Debug.d("BLE_DATA_AVAILABLE");
        }
    };

    private void drawShakeyInfo(String secret) {
        FirebaseDatabase.getInstance().getReference("Shakeys/" + secret).addListenerForSingleValueEvent(new ValueEventAdapter(this) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Shakey shakey = dataSnapshot.getValue(Shakey.class);

                if (shakey == null) {
                    Toast.makeText(MainActivity.this, "Shakey 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    updateUI();
                    return;
                }

                drawShakeyInfo();
            }
        });
    }

    private void drawShakeyInfo() {
        shakeyInfoNameView.setText(mShakey.getName());
        shakeyLastOpenView.setText("마지막으로 연 시간 : " + mShakey.getLastOpen(true));
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
                Toast.makeText(mContext, "개발중", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShakeyOptions() {

        LinkedList<String> menuList = new LinkedList<>();
        menuList.add("정보 확인");
        menuList.add("이름 변경");

        if (mBleService != null) {
            menuList.add("민감도 값 설정");
            menuList.add("Shakey 삭제");
        }

        new AlertDialog.Builder(this)
                .setSingleChoiceItems((CharSequence[]) menuList.toArray(), 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                showShakeyInfo();
                                break;
                            case 1:
                                changeShakeyName();
                                break;
                            case 2:
                                changeShakeySensValue();
                                break;
                            case 3:
                                removeShakey();
                                break;
                            default:
                                break;
                        }
                    }
                }).show();
    }

    private void showShakeyInfo() {
        Intent intent = new Intent(this, ShakeyInfoActivity.class);
        intent.putExtra(Extras.SHAKEY, mShakey);
        startActivity(intent);
    }

    private void changeShakeyName() {

    }

    private void changeShakeySensValue() {

    }

    private void removeShakey() {

    }

    private void openShakey() {

        final long openTime = System.currentTimeMillis();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Logs/" + mShakey.getSecret()).push();
        ShakeyLog log = new ShakeyLog();

        log.setWho(mUser.getUid());
        log.setEmail(mUser.getEmail());
        log.setRegdate(openTime);

        reference.setValue(log);

        mShakey.setLastOpen(openTime);
        FirebaseDatabase.getInstance().getReference("Shakeys/" + mShakey.getSecret()).runTransaction(new Transaction.Handler() {
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

        if (!mUser.getUid().equals(mShakey.getOwner())) {
            ShakeyAlert.getInstance().alert(this, mShakey);
        }

        mBleService.send(mShakey.unlockCommand());
        drawShakeyInfo();
    }

    private void closeMenu() {
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) closeMenu();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void updateUI() {
        if (mBleService.isConnecting()) {
            drawShakeyInfo();
        } else {
            shakeyInfoNameView.setText("연결된 Shakey 없음");

            shakeyInfoView.setOnClickListener(null);

            shakeyShareView.setOnClickListener(null);
            shakeyShareView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));

            shakeyKeyView.setOnClickListener(null);
            shakeyKeyView.setColorFilter(ContextCompat.getColor(mContext, R.color.gray9));
        }
    }
}
