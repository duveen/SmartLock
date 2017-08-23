package kr.o3selab.smartlock.layouts;

import android.app.Dialog;
import android.content.Context;

import kr.o3selab.smartlock.R;

public class LoadingProgressDialog extends Dialog {

    public LoadingProgressDialog(Context context) {
        super(context, R.style.AppTheme_TransparentDialog);
        setContentView(R.layout.dialog_loading);
        setCancelable(false);
    }
}
