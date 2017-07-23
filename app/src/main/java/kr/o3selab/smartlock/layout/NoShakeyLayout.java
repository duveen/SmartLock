package kr.o3selab.smartlock.layout;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import kr.o3selab.smartlock.R;

/**
 * Created by mingyupark on 2016. 10. 29..
 */

public class NoShakeyLayout extends LinearLayout {
    public NoShakeyLayout(Context context) {
        super(context);

        View view = inflate(context, R.layout.drawer_menu_no_shakey, this);
    }
}
