package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import kr.o3selab.smartlock.common.Shakey;
import kr.o3selab.smartlock.R;

public class ShakeyInfoActivity extends BaseActivity {

    public static final String TAG = "ShakeyInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_info);

        Intent intent = getIntent();
        Shakey shakey = (Shakey) intent.getSerializableExtra("info");

        TextView infoNameView = (TextView) findViewById(R.id.shakey_info_name);
        infoNameView.setText(shakey.getName());

        TextView infoMacView = (TextView) findViewById(R.id.shakey_info_mac);
        infoMacView.setText(shakey.getMac());

        TextView infoRegisterView = (TextView) findViewById(R.id.shakey_info_register);
        infoRegisterView.setText(shakey.getFirstregister());

        TextView infoLastOpenView = (TextView) findViewById(R.id.shakey_info_lastopen);
        infoLastOpenView.setText(shakey.getLastopen());


    }
}
