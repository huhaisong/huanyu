package caixin.android.com.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import caixin.android.com.utils.ClickUtil;

/**
 * Created by cxf on 2018/9/29.
 */

public abstract class AbsDialogFragment extends DialogFragment {

    protected Context mContext;
    protected View mRootView;
    public Dialog dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = getActivity();
        mRootView = LayoutInflater.from(mContext).inflate(getLayoutId(), null);
        dialog = new Dialog(mContext, getDialogStyle());
        dialog.setContentView(mRootView);
        dialog.setCancelable(canCancel());
        dialog.setCanceledOnTouchOutside(canCancel());
        setWindowAttributes(dialog.getWindow());
        return dialog;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected abstract int getLayoutId();

    protected abstract int getDialogStyle();

    protected abstract boolean canCancel();

    protected abstract void setWindowAttributes(Window window);

    protected boolean canClick() {
        return ClickUtil.canClick();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
