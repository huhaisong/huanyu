package caixin.android.com.view.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityRegisterBinding;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.utils.ToastUtils;
import caixin.android.com.viewmodel.RegisterViewModel;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.SoftKeyBroadManager;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RegisterActivity extends BaseActivity<ActivityRegisterBinding, RegisterViewModel> {
    private Disposable mRegisterDisposable;

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_register;
    }

    @Override
    public RegisterViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(RegisterViewModel.class);
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        StatusBarUtils.immersive(this, getResources().getColor(R.color.white));
        mBinding.btnGetCode.setOnClickListener(view -> onClick());
        mBinding.cbPwdShow.setOnCheckedChangeListener((buttonView, isChecked) ->
                mBinding.editPwd.setInputType(isChecked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        mBinding.btnRegist.setOnClickListener(v -> {

            String currentUsername = mBinding.editPhone.getText().toString();
            String currentPassword = mBinding.editPwd.getText().toString();
            String currentCode = mBinding.editCode.getText().toString();

            if (TextUtils.isEmpty(currentUsername)) {
                caixin.android.com.utils.ToastUtils.show("?????????????????????");
                return;
            }
            if (TextUtils.isEmpty(currentCode)) {
                caixin.android.com.utils.ToastUtils.show("?????????????????????");
                return;
            }
            if (currentUsername.length() != 11) {
                caixin.android.com.utils.ToastUtils.show("??????????????????11???????????????");
            }
            if (TextUtils.isEmpty(currentPassword)) {
                caixin.android.com.utils.ToastUtils.show(getResources().getString(R.string.Password_cannot_be_empty));
                return;
            }

            if (!mBinding.checkbox.isChecked()) {
                ToastUtils.show("??????????????????????????????????????????");
                return;
            }

            mViewModel.register(currentUsername,
                    "",
                    currentPassword,
                    currentCode,
                    "");
        });
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.getVerifyCode.observe(this, this::handleGetVerifyCode);
        mViewModel.uc.register.observe(this, this::handlerRegister);
    }

    private void handlerRegister(Object o) {
        showShortToast("???????????????");
        MMKVUtil.setLoginPhone(mBinding.editPhone.getText().toString());
        MMKVUtil.setLoginPassword("");
        finish();
    }

    private static final String TAG = "RegisterActivity";

    private void handleGetVerifyCode(Object o) {
        Observable.intervalRange(1, 60, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mRegisterDisposable = d;
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onNext(Long aLong) {
                        mBinding.btnGetCode.setText("?????????" + "(" + (60L - aLong) + ")");
                        mBinding.btnGetCode.setClickable(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mBinding.btnGetCode.setText("???????????????");
                        mBinding.btnGetCode.setClickable(true);
                    }

                    @Override
                    public void onComplete() {
                        mBinding.btnGetCode.setText("???????????????");
                        mBinding.btnGetCode.setClickable(true);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRegisterDisposable != null && mRegisterDisposable.isDisposed()) {
            mRegisterDisposable.dispose();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void onClick() {
        new com.adorkable.iosdialog.AlertDialog(RegisterActivity.this)
                .init()
                .setTitle("??????????????????")
                .setMsg("??????????????????????????????????????? \n" + mBinding.editPhone.getText().toString())
                .setPositiveButton("???", v1 -> mViewModel.getVerifyCode(mBinding.editPhone.getText().toString(), "1"))
                .setNegativeButton("??????", v12 -> {
                }).show();
    }
}