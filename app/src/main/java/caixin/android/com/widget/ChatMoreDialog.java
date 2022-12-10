package caixin.android.com.widget;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.PopupWindow;

import com.caixin.toutiao.R;

import caixin.android.com.utils.DpUtil;


/**
 * Created by cxf on 2018/11/7.
 * 聊天更多弹窗
 */

public class ChatMoreDialog extends PopupWindow {

    private View mParent;
    private View mContentView;
    private ActionListener mActionListener;

    public ChatMoreDialog(View parent, View contentView, boolean needAnim, ActionListener actionListener) {
        mParent = parent;
        mActionListener = actionListener;
        ViewParent viewParent = contentView.getParent();
        if (viewParent != null) {
            ((ViewGroup) viewParent).removeView(contentView);
        }
        mContentView = contentView;
        setContentView(contentView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(DpUtil.dp2px(200));
        setOutsideTouchable(false);
        if (needAnim) {
            setAnimationStyle(R.style.bottomToTopAnim2);
        }
        setOnDismissListener(() -> {
            ViewParent viewParent1 = mContentView.getParent();
            if (viewParent1 != null) {
                ((ViewGroup) viewParent1).removeView(mContentView);
            }
            mContentView = null;
            if (mActionListener != null) {
                mActionListener.onMoreDialogDismiss();
            }
            mActionListener = null;
        });
    }


    public void show() {
        showAtLocation(mParent, Gravity.BOTTOM, 0, 0);
    }


    public interface ActionListener {
        void onMoreDialogDismiss();
    }
}
