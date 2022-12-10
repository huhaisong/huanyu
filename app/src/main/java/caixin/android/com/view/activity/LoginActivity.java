package caixin.android.com.view.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityLoginBinding;
import com.tencent.mmkv.MMKV;

import java.util.concurrent.TimeUnit;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.constant.Extras;
import caixin.android.com.entity.CaptchaCheckIt;
import caixin.android.com.entity.CaptchaGetIt;
import caixin.android.com.entity.VerifyCodeRequest;
import caixin.android.com.entity.base.VerificationImgBaseResponse;
import caixin.android.com.utils.CommonUtils;
import caixin.android.com.Application;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.ToastUtils;
import caixin.android.com.viewmodel.LoginViewModel;
import caixin.android.com.widget.verificationcode.BlockPuzzleDialog;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;


public class LoginActivity extends BaseActivity<ActivityLoginBinding, LoginViewModel> {


    private boolean isCodeLogin = false;

    private boolean isLogin = false;
    private BlockPuzzleDialog blockPuzzleDialog;
    private CaptchaGetIt captchaGetIt;

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_login;
    }

    @Override
    public LoginViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(LoginViewModel.class);
    }

    private static final String TAG = "LoginActivity";

    @Override
    public void initData(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra(Extras.INTENT_LOGIN_AGAIN, false)) {
                showShortToast(intent.getStringExtra(Extras.INTENT_LOGIN_MESSAGE));
            }
        }

        blockPuzzleDialog = new BlockPuzzleDialog(this);
        blockPuzzleDialog.setOnResultsListener(new BlockPuzzleDialog.OnResultsListener() {
            @Override
            public void onResultsClick(String result) {

            }

            @Override
            public void onReInit() {
                mViewModel.getVerificationImg();
            }

            @Override
            public void checkCaptcha(double sliderXMoved) {
                mViewModel.checkCaptcha(sliderXMoved, captchaGetIt.getToken(), captchaGetIt.getSecretKey());
            }
        });

        mBinding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.btnGetCode.setEnabled(s.length() >= 11);
                mBinding.login.setEnabled(s.length() >= 11 &&
                        !TextUtils.isEmpty(mBinding.etCodeAndPassword.getText()));
                mBinding.ibtnDeletePhone.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mBinding.etCodeAndPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.ibtnDeleteCodeAndPassword.setVisibility(!TextUtils.isEmpty(s) ? View.VISIBLE : View.GONE);
                mBinding.login.setEnabled(mBinding.etPhone.getText().length() >= 11 &&
                        !TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mBinding.btnGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new com.adorkable.iosdialog.AlertDialog(LoginActivity.this)
                        .init()
                        .setTitle("确认电话号码")
                        .setMsg("我们将发送验证码到这个号码 \n" + mBinding.etPhone.getText().toString())
                        .setPositiveButton("好", v1 -> {
                            isLogin = false;
                            if (!isLogin) {
                                mViewModel.getLoginVerifyCode(mBinding.etPhone.getText().toString());
                            } else {
                                login();
                            }
                        })
                        .setNegativeButton("取消", v12 -> {
                        }).show();
            }
        });

        mBinding.ibtnDeleteCodeAndPassword.setOnClickListener(v -> mBinding.etCodeAndPassword.setText(""));
        mBinding.cbPwdShow.setOnCheckedChangeListener((buttonView, isChecked) ->
                mBinding.etCodeAndPassword.setInputType(!isChecked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
        mBinding.cbPwdShow.setChecked(true);

        mBinding.register.setOnClickListener(v -> {
            Log.e(TAG, "initData: ");
            startActivityForResult(RegisterActivity.class, null, 1000);
        });
        mBinding.tvForget.setOnClickListener(v -> startActivity(ForgetPasswordActivity.class));
        mBinding.ibtnDeletePhone.setOnClickListener(v -> mBinding.etPhone.setText(""));
        mBinding.login.setOnClickListener(v -> {
            login();
        });
        String loginPhone = MMKVUtil.getLoginPhone();
        mBinding.etPhone.setText(loginPhone);
        if (!TextUtils.isEmpty(MMKVUtil.getLoginPassword()) && !TextUtils.isEmpty(MMKVUtil.getLoginPhone())) {
            mBinding.etCodeAndPassword.setText(MMKVUtil.getLoginPassword());
        }

        mBinding.tvSwitchLoginMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBinding.etCodeAndPassword.setText("");
                if (isCodeLogin) {
                    isCodeLogin = false;
                    mBinding.tvSwitchLoginMethod.setText(getResources().getString(R.string.to_use_verify_code_introduce));
                    mBinding.cbPwdShow.setVisibility(View.VISIBLE);
                    mBinding.btnGetCode.setVisibility(View.GONE);
                    mBinding.tvPassIntroduce.setText(getResources().getString(R.string.password));
                    mBinding.etCodeAndPassword.setHint(getResources().getString(R.string.password_input_introduce));
                    mBinding.etCodeAndPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mBinding.cbPwdShow.setChecked(true);
                } else {
                    isCodeLogin = true;
                    mBinding.tvSwitchLoginMethod.setText(getResources().getString(R.string.to_use_password_introduce));
                    mBinding.cbPwdShow.setVisibility(View.GONE);
                    mBinding.btnGetCode.setVisibility(View.VISIBLE);
                    mBinding.tvPassIntroduce.setText(getResources().getString(R.string.verify_code));
                    mBinding.etCodeAndPassword.setHint(getResources().getString(R.string.verify_code_input_introduce));
                    mBinding.etCodeAndPassword.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
            }
        });
    }

    private void login() {
        if (!CommonUtils.isNetWorkConnected(Application.getInstance())) {
            ToastUtils.show(getResources().getString(R.string.network_isnot_available));
            return;
        }
        String currentUsername = mBinding.etPhone.getText().toString();
        String currentPassword = mBinding.etCodeAndPassword.getText().toString();
        if (TextUtils.isEmpty(currentUsername)) {
            ToastUtils.show(getResources().getString(R.string.User_name_cannot_be_empty));
            return;
        }
        if (currentUsername.length() != 11) {
            ToastUtils.show("请输入正确的11位手机号！");
        }
        if (TextUtils.isEmpty(currentPassword)) {
            ToastUtils.show(getResources().getString(R.string.Password_cannot_be_empty));
            return;
        }
        if (isCodeLogin) {
            mViewModel.httpLoginWithCode(currentUsername, currentPassword);
        } else {
            mViewModel.httpLogin(currentUsername, currentPassword);
        }
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.login.observe(this, this::handleLogin);
        mViewModel.uc.getLoginVerifyCode.observe(this, this::handleGetLoginVerifyCode);
        mViewModel.uc.getVerificationImg.observe(this, this::handleGetVerifyImg);
        mViewModel.uc.checkItMutableLiveData.observe(this, this::handleCheckVerify);
    }

    private void handleCheckVerify(VerificationImgBaseResponse<CaptchaCheckIt> captchaCheckItVerificationImgBaseResponse) {
        if (captchaCheckItVerificationImgBaseResponse.isSuccess()) {
            blockPuzzleDialog.OK();
            blockPuzzleDialog.dismiss();
            if (!isLogin) {
                mViewModel.getLoginVerifyCode(mBinding.etPhone.getText().toString());
            } else {
                login();
            }
        } else {
            blockPuzzleDialog.FAILED();
            mViewModel.getVerificationImg();
        }
    }

    private void handleGetVerifyImg(CaptchaGetIt captchaGetIt) {
        this.captchaGetIt = captchaGetIt;
        blockPuzzleDialog.init(captchaGetIt);
        blockPuzzleDialog.show();
    }

    private Disposable mRegisterDisposable;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRegisterDisposable != null && mRegisterDisposable.isDisposed()) {
            mRegisterDisposable.dispose();
        }
    }

    private void handleGetLoginVerifyCode(Object o) {
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

    private void handleLogin(Object o) {
        startActivity(MainActivity.class);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mBinding.etPhone.setText(MMKVUtil.getLoginPhone());
    }
}
