package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.google.android.gms.common.api.GoogleApiClient;

import butterknife.ButterKnife;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.layouts.ResponsivenessDialog;

public class SetupActivity extends BaseActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ButterKnife.bind(this);

        ImageView undoButton = (ImageView) findViewById(R.id.setting_back);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetupActivity.this.finish();
            }
        });

        LinearLayout noticeLayout = (LinearLayout) findViewById(R.id.setting_notice);
        noticeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SetupActivity.this, NoticeActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout responsivenessLayout = (LinearLayout) findViewById(R.id.setting_sensitive);
        responsivenessLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResponsivenessDialog mCustomDialog = new ResponsivenessDialog(SetupActivity.this,
                        "민감도 설정");
                mCustomDialog.show();
            }
        });

        Switch togglebtn = (Switch) findViewById(R.id.noti_toggle);
        togglebtn.setChecked(AppSettings.getNotiSetting());
        togglebtn.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    AppSettings.setSettingsValue(AppSettings.SETTINGS_NOTI, b, 0, null);
                } else {
                    AppSettings.setSettingsValue(AppSettings.SETTINGS_NOTI, b, 0, null);
                }
            }
        });

    }
}