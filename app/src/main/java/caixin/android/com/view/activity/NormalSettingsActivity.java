package caixin.android.com.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.lifecycle.ViewModelProviders;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityNormalSettingBinding;
import com.caixin.toutiao.databinding.ActivitySecuritySettingsBinding;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.entity.UserInfoEntity;
import caixin.android.com.http.basic.config.HttpConfig;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.viewmodel.UserInfoViewModel;

public class NormalSettingsActivity extends BaseActivity<ActivityNormalSettingBinding, UserInfoViewModel> {

    @Override
    public int initContentView(Bundle savedInstanceState) {
        StatusBarUtils.immersive(this, getResources().getColor(R.color.colorPrimary));
        return R.layout.activity_normal_setting;
    }

    @Override
    public UserInfoViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(UserInfoViewModel.class);
    }

    private int remind;

    @Override
    public void initData(Bundle savedInstanceState) {
        mBinding.titleBar.title.setText(getResources().getString(R.string.normal_setting));
        mBinding.titleBar.ivClose.setOnClickListener(v -> onBackPressed());
        mBinding.tvIpAddress.setText(HttpConfig.HOST);
        if (MMKVUtil.getUserInfo().getRemind() == 1) {  //1代表开启，2代表关闭
            mBinding.voiceSwitch.setChecked(true);
            remind = 1;
        } else {
            mBinding.voiceSwitch.setChecked(false);
            remind = 2;
        }
        mBinding.voiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mViewModel.setRedmin(1);
                    remind = 1;
                } else {
                    mViewModel.setRedmin(2);
                    remind = 2;
                }
            }
        });
        mBinding.rlPicChannel.setOnClickListener(v -> startActivity(PicChannelSelectActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MMKVUtil.getSelectedPicChannel() == 0) {
            mBinding.tvPicChannal.setText("默认线路");
        } else {
            mBinding.tvPicChannal.setText("图片线路" + MMKVUtil.getSelectedPicChannel());
        }
    }


    @Override
    public void initViewObservable() {
        mViewModel.uc.setRemind.observe(this, this::handleSetRemind);
    }

    private void handleSetRemind(Boolean b) {
        if (b) {
            if (remind == 1) {
                mBinding.voiceSwitch.setChecked(true);
            } else if (remind == 2) {
                mBinding.voiceSwitch.setChecked(false);
            }
        } else {
            if (remind == 1) {
                remind = 2;
//                mBinding.voiceSwitch.setChecked(false);
            } else if (remind == 2) {
                remind = 1;
//                mBinding.voiceSwitch.setChecked(true);
            }
        }
        UserInfoEntity userInfoEntity = MMKVUtil.getUserInfo();
        userInfoEntity.setRemind(remind);
        MMKVUtil.setUserInfo(userInfoEntity);
    }
}
