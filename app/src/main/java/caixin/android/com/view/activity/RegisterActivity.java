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

import com.blankj.utilcode.util.ToastUtils;
import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityRegisterBinding;

import caixin.android.com.base.AppViewModelFactory;
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
        mBinding.titleBar.setLeftLayoutClickListener(v -> onBackPressed());
        mBinding.titleBar.setTitle("注册");
        mBinding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.btnGetCode.setEnabled(s.length() >= 11);
                updateRegisterButton();
                mBinding.ibtnDeletePhone.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mBinding.etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegisterButton();
                mBinding.ibtnDeleteCode.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.etPwd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.ibtnDeletePwd.setVisibility(!TextUtils.isEmpty(s) ? View.VISIBLE : View.GONE);
                updateRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.etPwdEnsure.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.ibtnDeletePwdEnsure.setVisibility(!TextUtils.isEmpty(s) ? View.VISIBLE : View.GONE);
                updateRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.ibtnDeleteNickname.setVisibility(!TextUtils.isEmpty(s) ? View.VISIBLE : View.GONE);
                updateRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.ibtnDeletePwd.setOnClickListener(v -> mBinding.etPwd.setText(""));
        mBinding.ibtnDeletePwdEnsure.setOnClickListener(v -> mBinding.etPwdEnsure.setText(""));
        mBinding.ibtnDeletePhone.setOnClickListener(v -> mBinding.etPhone.setText(""));
        mBinding.ibtnDeleteCode.setOnClickListener(v -> mBinding.etCode.setText(""));
        mBinding.ibtnDeleteNickname.setOnClickListener(v -> mBinding.etNickname.setText(""));
        mBinding.btnGetCode.setOnClickListener(view -> onClick());
        mBinding.cbPwdShow.setOnCheckedChangeListener((buttonView, isChecked) ->
                mBinding.etPwd.setInputType(isChecked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        mBinding.cbPwdShowEnsure.setOnCheckedChangeListener((buttonView, isChecked) ->
                mBinding.etPwdEnsure.setInputType(isChecked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        mBinding.btnRegist.setOnClickListener(v -> {
            JSONObject json = new JSONObject();
            if (!mBinding.etPwdEnsure.getText().toString().equals(mBinding.etPwd.getText().toString())) {
                ToastUtils.showShort(R.string.Two_input_password);
                return;
            }
            mViewModel.register(mBinding.etPhone.getText().toString(),
                    mBinding.etNickname.getText().toString(),
                    mBinding.etPwd.getText().toString(),
                    mBinding.etCode.getText().toString(),
                    mBinding.etInviteCode.getText().toString());
        });

        SoftKeyBroadManager softKeyBroadManager = new SoftKeyBroadManager(mBinding.contentContainer);
        softKeyBroadManager.addSoftKeyboardStateListener(softKeyboardStateListener);
    }

    SoftKeyBroadManager.SoftKeyboardStateListener softKeyboardStateListener = new SoftKeyBroadManager.SoftKeyboardStateListener() {

        @Override
        public void onSoftKeyboardOpened(int keyboardHeightInPx) {
            Log.e(TAG, "onSoftKeyboardOpened: " + keyboardHeightInPx);
            mBinding.contentContainer.scrollTo(0,keyboardHeightInPx);
        }

        @Override
        public void onSoftKeyboardClosed() {
            mBinding.contentContainer.scrollTo(0,0);
        }
    };

    @Override
    public void initViewObservable() {
        mViewModel.uc.getVerifyCode.observe(this, this::handleGetVerifyCode);
        mViewModel.uc.register.observe(this, this::handlerRegister);
    }

    private void handlerRegister(Object o) {
        showShortToast("注册成功！");
        MMKVUtil.setLoginPhone(mBinding.etPhone.getText().toString());
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
                        mBinding.btnGetCode.setEnabled(false);
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onNext(Long aLong) {
                        mBinding.btnGetCode.setText("已发送" + "(" + (60L - aLong) + ")");
                    }

                    @Override
                    public void onError(Throwable e) {
                        mBinding.btnGetCode.setText("获取验证码");
                        mBinding.btnGetCode.setClickable(true);
                    }

                    @Override
                    public void onComplete() {
                        mBinding.btnGetCode.setText("获取验证码");
                        mBinding.btnGetCode.setEnabled(true);
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

    private void updateRegisterButton() {
        mBinding.btnRegist.setEnabled(mBinding.etPhone.getText().length() >= 11 &&
                !TextUtils.isEmpty(mBinding.etCode.getText().toString()) && mBinding.etCode.getText().toString().length() >= 4 &&
                !TextUtils.isEmpty(mBinding.etPwd.getText()) &&
                !TextUtils.isEmpty(mBinding.etPwdEnsure.getText()) &&
                !TextUtils.isEmpty(mBinding.etNickname.getText()) &&
                mBinding.etPwdEnsure.getText().length() == mBinding.etPwd.getText().length() &&
                mBinding.etPwd.length() >= 6);
    }

    private void onClick() {
        new com.adorkable.iosdialog.AlertDialog(RegisterActivity.this)
                .init()
                .setTitle("确认电话号码")
                .setMsg("我们将发送验证码到这个号码 \n" + mBinding.etPhone.getText().toString())
                .setPositiveButton("好", v1 -> mViewModel.getVerifyCode(mBinding.etPhone.getText().toString(), "1"))
                .setNegativeButton("取消", v12 -> {
                }).show();
    }
}