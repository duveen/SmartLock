package kr.o3selab.smartlock.activities.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Utils;

public class AlertActivity extends AppCompatActivity {

    @BindView(R.id.setting_alert_option1_switch)
    Switch mOptionSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        ButterKnife.bind(this);

        SharedPreferences sharedPreferences = Utils.getSharedPreferences(this);
        boolean options1 = sharedPreferences.getBoolean(Extras.SETTING_ALERT_OPTIONS1, false);

        mOptionSwitch.setChecked(options1);
    }

    @OnClick(R.id.setting_alert_option1)
    void options1() {
        boolean options1 = mOptionSwitch.isChecked();

        SharedPreferences.Editor editor = Utils.getEditor(this);
        editor.putBoolean(Extras.SETTING_ALERT_OPTIONS1, !options1);

        if (options1) {
            // true -> false
        } else {
            // false -> true
        }
    }

    @OnClick(R.id.setting_alert_back)
    void back() {
        onBackPressed();
    }
}
