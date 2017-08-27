package kr.o3selab.smartlock.layouts;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;


public class OptionsDialog extends Dialog {

    private WindowManager.LayoutParams mLayoutParams;
    private HashMap<String, Object> extras;

    @BindView(R.id.custom_dialog_title)
    TextView mTitleView;
    @BindView(R.id.custom_dialog_content_text)
    TextView mContentTextView;
    @BindView(R.id.custom_dialog_content_edit_text)
    EditText mContentEditTextView;
    @BindView(R.id.custom_dialog_content_seekbar)
    DiscreteSeekBar mContentSeekbar;
    @BindView(R.id.custom_dialog_yes)
    TextView mYesTextView;
    @BindView(R.id.custom_dialog_no)
    TextView mNoTextView;

    OnClickListener listener;

    public static class Builder {
        OptionsDialog dialog;

        public Builder(Context context) {
            dialog = new OptionsDialog(context);
        }

        public Builder setType(int type) {
            dialog.setType(type);
            return this;
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

        public Builder setHint(String hint) {
            dialog.setHint(hint);
            return this;
        }

        public Builder setInputType(int inputType) {
            dialog.setInputType(inputType);
            return this;
        }

        public Builder setSeekbarMin(int min) {
            dialog.setMin(min);
            return this;
        }

        public Builder setSeekbarMax(int max) {
            dialog.setMax(max);
            return this;
        }

        public Builder setSeekbarValue(int value) {
            dialog.setValue(value);
            return this;
        }

        public Builder putExtras(String key, Object value) {
            dialog.putExtras(key, value);
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

        public Builder setOnClickListener(OnClickListener listener) {
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

    public static final int TEXT_TYPE = 0;
    public static final int EDIT_TEXT_TYPE = 1;
    public static final int SEEK_BAR_TYPE = 2;

    private OptionsDialog(@NonNull Context context) {
        super(context);

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

        extras = new HashMap<>();

        setType(TEXT_TYPE);
        setOptions(Options.YES);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(OptionsDialog dialog, ANSWER options) {
                dialog.dismiss();
            }
        });
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

    private void setType(Integer type) {
        switch (type) {
            case TEXT_TYPE:
                mContentTextView.setVisibility(View.VISIBLE);
                mContentEditTextView.setVisibility(View.GONE);
                mContentSeekbar.setVisibility(View.GONE);
                break;
            case EDIT_TEXT_TYPE:
                mContentTextView.setVisibility(View.GONE);
                mContentEditTextView.setVisibility(View.VISIBLE);
                mContentSeekbar.setVisibility(View.GONE);
                break;
            case SEEK_BAR_TYPE:
                mContentTextView.setVisibility(View.GONE);
                mContentEditTextView.setVisibility(View.GONE);
                mContentSeekbar.setVisibility(View.VISIBLE);
                break;
            default:
                mContentTextView.setVisibility(View.GONE);
                mContentEditTextView.setVisibility(View.GONE);
                mContentSeekbar.setVisibility(View.GONE);
                break;
        }
    }

    private void putExtras(String key, Object value) {
        extras.put(key, value);
    }

    public Object getExtras(String key) {
        return extras.get(key);
    }

    private void setMessage(@Nullable CharSequence message) {
        mContentTextView.setText(message);
    }

    private void setMessage(@StringRes int resId) {
        mContentTextView.setText(resId);
    }

    private void setHint(@Nullable CharSequence hint) {
        mContentEditTextView.setHint(hint);
    }

    private void setInputType(int inputType) {
        mContentEditTextView.setInputType(inputType);
    }

    public String getEditTextMessage() {
        return mContentEditTextView.getText().toString();
    }

    private void setMin(int min) {
        mContentSeekbar.setMin(min);
    }

    private void setMax(int max) {
        mContentSeekbar.setMax(max);
    }

    private void setValue(int value) {
        mContentSeekbar.setProgress(value);
    }

    public DiscreteSeekBar getSeekbar() {
        return mContentSeekbar;
    }

    private void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    private void setOptions(Options options) {
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
        if (listener != null) listener.onClick(this, ANSWER.YES);
    }

    @OnClick(R.id.custom_dialog_no)
    void noOnClick() {
        if (listener != null) listener.onClick(this, ANSWER.NO);
    }

    public interface OnClickListener {
        void onClick(OptionsDialog dialog, ANSWER options);
    }

    public enum Options {YES, YES_NO}

    public enum ANSWER {YES, NO}
}