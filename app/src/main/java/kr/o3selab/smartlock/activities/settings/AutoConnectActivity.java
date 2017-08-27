package kr.o3selab.smartlock.activities.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.AppConfig;
import kr.o3selab.smartlock.models.Shakey;

public class AutoConnectActivity extends AppCompatActivity {

    @BindView(R.id.setting_auto_connect_option1)
    View mOptions1;
    @BindView(R.id.setting_auto_connect_option1_switch)
    Switch mOptions1Switch;

    @BindView(R.id.setting_auto_connect_option2)
    View mOptions2;
    @BindView(R.id.setting_auto_connect_option2_background)
    View mOptions2Background;
    @BindView(R.id.setting_auto_connect_option2_text)
    TextView mOptions2Text;

    private Vector<Shakey> shakeys;
    private HashMap<String, Shakey> items;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_connect);

        ButterKnife.bind(this);

        updateUI();
    }

    void updateUI() {
        boolean autoStart = AppConfig.getInstance().isAutoStart();

        if (autoStart) {
            mOptions1Switch.setChecked(true);
            mOptions2Background.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        } else {
            mOptions1Switch.setChecked(false);
            mOptions2Background.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_100));
        }

        try {
            Shakey shakey = AppConfig.getInstance().getAutoConnectedDevice();
            mOptions2Text.setText(shakey.getName());
        } catch (Exception e) {
            mOptions2Text.setText("설정된 장치 없음");
        }
    }


    @OnClick(R.id.setting_auto_connect_option1)
    void options1() {
        AppConfig.getInstance().setAutoStart(!AppConfig.getInstance().isAutoStart());
        updateUI();
    }

    @OnClick(R.id.setting_auto_connect_option2)
    void options2() {

        shakeys = AppConfig.getInstance().getShakeys();
        items = new HashMap<>();

        for (Shakey shakey : shakeys) {
            items.put(shakey.getName(), shakey);
        }

        final String[] list = new String[items.size()];
        new AlertDialog.Builder(this)
                .setItems(items.keySet().toArray(list), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Shakey shakey = items.get(list[which]);

                        AppConfig.getInstance().setAutoConnectedDevice(shakey);
                        mOptions2Text.setText(shakey.getName());
                    }
                })
                .setCancelable(true)
                .show();
    }

    @OnClick(R.id.setting_auto_connect_back)
    void back() {
        onBackPressed();
    }
}
