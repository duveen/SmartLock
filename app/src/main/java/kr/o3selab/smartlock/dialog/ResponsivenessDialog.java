package kr.o3selab.smartlock.dialog;

/**
 * Created by LGY on 2016-10-31.
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import kr.o3selab.smartlock.bluetooth.BleManager;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Common;
import kr.o3selab.smartlock.R;

public class ResponsivenessDialog extends Dialog {

    private TextView mTitleView;
    private String mTitle;
    private Context mContext;
    int step = 1;
    int max = 100;
    int min = 1;

    private BleManager mBleManager;
    private TextView mContentView;
    private int prog;
    private int progressValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.activity_responsivenessdialog);

        mTitleView = (TextView) findViewById(R.id.txt_title);
        mContentView = (TextView) findViewById(R.id.txt_respon);
        SeekBar seekBar = (SeekBar) findViewById(R.id.respon_seekbar);
        mBleManager = BleManager.getInstance(mContext, null);
        int re = AppSettings.getResponsiveness();
        Log.d("민감도다이얼로그","민감도불러옴"+re);

        mContentView.setText("");
        mTitleView.setText(mTitle);
        seekBar.setProgress(AppSettings.getResponsiveness());
        seekBar.setMax(max);
        seekBar.setMinimumWidth(0);
        seekBar.setProgress(re);
        seekBar.incrementProgressBy(step);
        if(re >=0 &&  re <= 19){
            mContentView.setText("매우 민감함");
            prog = 1;
        }
        else if(re >= 20 && re <= 39){
            mContentView.setText("민감함");
            prog = 2;
        }
        else if(re >= 40 && re <= 59){
            mContentView.setText("보통");
            prog = 3;
        }
        else if(re >= 60 && re <= 79){
            mContentView.setText("둔함");
            prog = 4;
        }
        else if(re >= 80 && re <= 100){
            mContentView.setText("매우 둔함");
            prog = 5;
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValues = progress;
                if(progress >=0 &&  progress <= 19){
                    mContentView.setText("매우 민감함");
                    prog = 1;
                    return;
                }
                else if(progress >= 20 && progress <= 39){
                    mContentView.setText("민감함");
                    prog = 2;
                    return;
                }
                else if(progress >= 40 && progress <= 59){
                    mContentView.setText("보통");
                    prog = 3;
                    return;
                }
                else if(progress >= 60 && progress <= 79){
                    mContentView.setText("둔함");
                    prog = 4;
                    return;
                }
                else if(progress >= 80 && progress <= 100){
                    mContentView.setText("매우 둔함");
                    prog = 5;
                    return;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                String ss = "Sens"+ prog;
                Log.d("민감도 설정",ss+"보내짐");
                AppSettings.setSettingsValue(AppSettings.SETTINGS_RESPONSIVENESS,false,progressValues,null);

                Common.getCommon().service.sendMessageToRemote(ss);
            }
        });

    }

    // 클릭버튼이 하나일때 생성자 함수로 클릭이벤트를 받는다.
    public ResponsivenessDialog(Context context, String title) {
        super(context, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        mContext = context;
        this.mTitle = title;
    }

}