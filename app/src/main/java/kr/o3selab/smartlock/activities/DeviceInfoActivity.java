package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyLog;

public class DeviceInfoActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "DeviceInfoActivity";

    private FirebaseUser mUser;
    private DatabaseReference mReference;

    @BindView(R.id.shakey_info_title)
    TextView mTitleView;

    @BindView(R.id.shakey_info_refresh)
    SwipeRefreshLayout mRefreshLayout;
    @BindView(R.id.shakey_info_name)
    TextView mNameView;
    @BindView(R.id.shakey_info_mac)
    TextView mMacView;
    @BindView(R.id.shakey_info_register)
    TextView mRegisterView;
    @BindView(R.id.shakey_info_lastopen)
    TextView mLastOpenView;
    @BindView(R.id.shakey_info_owner)
    TextView mOwnerView;
    @BindView(R.id.shakey_info_logs)
    LinearLayout mLogsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_info);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        Shakey shakey = (Shakey) intent.getSerializableExtra(Extras.SHAKEY);

        if (shakey == null) {
            this.finish();
            return;
        }

        mTitleView.setText(shakey.getName());

        mNameView.setText(shakey.getName());
        mMacView.setText(shakey.getMac());
        mRegisterView.setText(shakey.getRegdate(true));
        mLastOpenView.setText(shakey.getLastOpen(true));
        mOwnerView.setText(shakey.getOwnerEmail());

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mReference = FirebaseDatabase.getInstance().getReference("Logs/" + shakey.getSecret());
        mRefreshLayout.setOnRefreshListener(this);

        getShakeyLogs(0, 10);
    }

    private void getShakeyLogs(int start, int end) {
        mRefreshLayout.setRefreshing(true);
        mLogsListView.removeAllViewsInLayout();
        mReference.limitToLast(end).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRefreshLayout.setRefreshing(false);

                if (dataSnapshot.getValue() == null) {
                    View view = getLayoutInflater().inflate(R.layout.item_shakey_log_no, null);
                    mLogsListView.addView(view);
                    return;
                }

                LinkedList<ShakeyLog> list = new LinkedList<>();
                for (DataSnapshot key : dataSnapshot.getChildren()) {
                    ShakeyLog log = key.getValue(ShakeyLog.class);
                    list.add(log);
                }
                Collections.reverse(list);

                for (ShakeyLog log : list) {
                    View view = getLayoutInflater().inflate(R.layout.item_shakey_log, null);
                    if (!log.getWho().equals(mUser.getUid())) {
                        View background = view.findViewById(R.id.shakey_log);
                        background.setBackgroundColor(ContextCompat.getColor(DeviceInfoActivity.this, R.color.green_100));
                    }

                    TextView emailView = (TextView) view.findViewById(R.id.shakey_log_who);
                    emailView.setText(log.getEmail());

                    TextView dateView = (TextView) view.findViewById(R.id.shakey_log_regdate);
                    dateView.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date(log.getRegdate())));

                    mLogsListView.addView(view);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Debug.d(databaseError.getMessage());
            }
        });
    }

    @Override
    public void onRefresh() {
        getShakeyLogs(0, 10);
    }

    @OnClick(R.id.shakey_info_undo)
    void back() {
        onBackPressed();
    }
}
