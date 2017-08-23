package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.activities.settings.AlertActivity;
import kr.o3selab.smartlock.layouts.ResponsivenessDialog;

public class SetupActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ButterKnife.bind(this);

        LinearLayout noticeLayout = (LinearLayout) findViewById(R.id.setting_notice);
        noticeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SetupActivity.this, NoticeActivity.class);
                startActivity(intent);
            }
        });
    }

    @OnClick(R.id.setting_alert)
    void alert() {
        startActivity(new Intent(SetupActivity.this, AlertActivity.class));
    }

    @OnClick(R.id.setting_back)
    void back() {
        onBackPressed();
    }

    @OnClick(R.id.setting_info)
    void info() {

    }
}