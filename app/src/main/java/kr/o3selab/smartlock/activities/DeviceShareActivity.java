package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.layouts.LoadingProgressDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyShare;

public class DeviceShareActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {

    @BindView(R.id.share_user_image)
    ImageView userProfileView;
    @BindView(R.id.share_user_text)
    TextView userNameView;

    @BindView(R.id.share_options1)
    Spinner options1;
    int op1Which;
    @BindView(R.id.share_options2)
    Spinner options2;
    int op2Which;

    Shakey mShakey;

    public static final int USER_NAME = 0;
    public static final int USER_PROFILE = 1;
    public static final int USER_UID = 2;
    String[] mUserContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_share);

        ButterKnife.bind(this);

        mShakey = (Shakey) getIntent().getSerializableExtra(Extras.SHAKEY);

        ArrayAdapter<CharSequence> options1Menu = ArrayAdapter.createFromResource(this, R.array.unlock_number, android.R.layout.simple_spinner_item);
        options1Menu.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        options1.setAdapter(options1Menu);
        options1.setTag("OP1");
        options1.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> options2Menu = ArrayAdapter.createFromResource(this, R.array.time, android.R.layout.simple_spinner_item);
        options2Menu.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        options2.setAdapter(options2Menu);
        options2.setTag("OP2");
        options2.setOnItemSelectedListener(this);

        loading = new LoadingProgressDialog(this);
    }

    @OnClick(R.id.share_user)
    void selectShareUser() {
        Intent intent = new Intent(this, FindUserActivity.class);
        startActivityForResult(intent, Extras.FIND_USER);
    }

    @OnClick(R.id.share_ok)
    void ok() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (mUserContent == null || user == null) {
            Toast.makeText(this, "공유 대상을 선택해주세요", Toast.LENGTH_SHORT).show();
            selectShareUser();

            return;
        }

        ShakeyShare shakeyShare = new ShakeyShare();
        shakeyShare.setSid(mShakey.getSecret());
        shakeyShare.setFrom(user.getUid());
        shakeyShare.setTo(mUserContent[USER_UID]);
        shakeyShare.setRemain(ShakeyShare.getRemainFromIndex(op1Which));

        loading.show();

        FirebaseDatabase.getInstance().getReference("Shares/" + mUserContent[USER_UID]).push().setValue(shakeyShare).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                loading.dismiss();
                DeviceShareActivity.this.finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Extras.FIND_USER && resultCode == Extras.FIND_USER_OK) {
            mUserContent = data.getStringArrayExtra(Extras.FIND_USER_CONTENT);
            userNameView.setText(mUserContent[USER_NAME]);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == options1) {
            op1Which = position;
        } else if (parent == options2) {
            op2Which = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @OnClick(R.id.share_back)
    void back() {
        finish();
    }
}
