package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;

import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.activities.settings.AlertActivity;
import kr.o3selab.smartlock.activities.settings.AutoConnectActivity;

public class SetupActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.setting_notice)
    void notice() {
        startActivity(new Intent(this, NoticeActivity.class));
    }

    @OnClick(R.id.setting_alert)
    void alert() {
        startActivity(new Intent(this, AlertActivity.class));
    }

    @OnClick(R.id.setting_auto_connect)
    void auto() {
        startActivity(new Intent(this, AutoConnectActivity.class));
    }

    @OnClick(R.id.setting_back)
    void back() {
        onBackPressed();
    }

    @OnClick(R.id.setting_info)
    void info() {

    }
}