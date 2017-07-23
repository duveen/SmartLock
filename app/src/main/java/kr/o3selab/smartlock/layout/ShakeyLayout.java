package kr.o3selab.smartlock.layout;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import kr.o3selab.smartlock.common.Shakey;
import kr.o3selab.smartlock.R;

/**
 * Created by mingyupark on 2016. 10. 29..
 */

public class ShakeyLayout extends LinearLayout {
    public ShakeyLayout(Context context, Shakey shakey) {
        super(context);

        View view = inflate(context, R.layout.drawer_menu_shakey, this);

        TextView text = (TextView) view.findViewById(R.id.menu_shakey_name_line);
        text.setText(shakey.getName());
    }
}
