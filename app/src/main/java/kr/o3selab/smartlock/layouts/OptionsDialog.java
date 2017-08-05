package kr.o3selab.smartlock.layouts;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.models.Shakey;


public class OptionsDialog extends Dialog {

    private WindowManager.LayoutParams mLayoutParams;
    private Shakey shakey;

    @BindView(R.id.custom_dialog_title)
    TextView mTitleView;
    @BindView(R.id.custom_dialog_content)
    TextView mContentView;
    @BindView(R.id.custom_dialog_yes)
    TextView mYesTextView;
    @BindView(R.id.custom_dialog_no)
    TextView mNoTextView;

    OptionsDialogClickListener listener;

    public static class Builder {
        OptionsDialog dialog;

        public Builder(Context context, Shakey shakey) {
            dialog = new OptionsDialog(context, shakey);
        }

        public Builder setTitle(String title) {
            dialog.setTitle(title);
            return this;
        }

        public Builder setMessage(String message) {
            dialog.setMessage(message);
            return this;
        }

        public Builder setMessage(@StringRes int resId) {
            dialog.setMessage(resId);
            return this;
        }

        public Builder setCancelable(boolean flag) {
            dialog.setCancelable(flag);
            return this;
        }

        public Builder setOptions(Options options) {
            dialog.setOptions(options);
            return this;
        }

        public Builder setOnClickListener(OptionsDialogClickListener listener) {
            dialog.setOnClickListener(listener);
            return this;
        }

        public void show() {
            dialog.show();
        }

        public OptionsDialog create() {
            return dialog;
        }
    }

    private OptionsDialog(@NonNull Context context, Shakey shakey) {
        super(context);
        this.shakey = shakey;

        View view = getLayoutInflater().inflate(R.layout.dialog_options, null);
        this.setContentView(view);

        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.9);

        ButterKnife.bind(this, view);

        if (getWindow() != null) {
            mLayoutParams = this.getWindow().getAttributes();
            mLayoutParams.width = width;
            mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null && mLayoutParams != null)
            getWindow().setAttributes(mLayoutParams);
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        mTitleView.setVisibility(View.VISIBLE);
        mTitleView.setText(title);
    }

    public void setMessage(@Nullable CharSequence message) {
        mContentView.setText(message);
    }

    public void setMessage(@StringRes int resId) {
        mContentView.setText(resId);
    }

    public void setOnClickListener(OptionsDialogClickListener listener) {
        this.listener = listener;
    }

    public void setOptions(Options options) {
        switch (options) {
            case YES:
                mNoTextView.setVisibility(View.GONE);
                break;
            case YES_NO:
                mNoTextView.setVisibility(View.VISIBLE);
                break;
        }
    }

    @OnClick(R.id.custom_dialog_yes)
    void yesOnClick() {
        if (listener != null) listener.onClick(this, shakey, ANSWER.YES);
    }

    @OnClick(R.id.custom_dialog_no)
    void noOnClick() {
        if (listener != null) listener.onClick(this, shakey, ANSWER.NO);
    }

    public interface OptionsDialogClickListener {
        void onClick(Dialog v, Shakey shakey, ANSWER options);
    }

    public enum Options {YES, YES_NO}

    public enum ANSWER {YES, NO}
}