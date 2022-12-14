package caixin.android.com.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.lifecycle.ViewModelProviders;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityScanBinding;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.v3.MessageDialog;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.base.BaseViewModel;
import caixin.android.com.constant.Config;
import caixin.android.com.daomanager.FriendDaoManager;
import caixin.android.com.entity.AppDownloadUrlEntity;
import caixin.android.com.entity.FriendEntity;
import caixin.android.com.utils.GlideEngine;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.viewmodel.ContactViewModel;
import caixin.android.com.viewmodel.HomeViewModel;
import caixin.android.com.viewmodel.SignViewModel;
import caixin.android.com.zxing.qrcodecore.QRCodeView;

import static caixin.android.com.constant.Config.QR_SEPECIFIC_CODE;

public class ScanActivity extends BaseActivity<ActivityScanBinding, ContactViewModel> {
    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_scan;
    }

    @Override
    public ContactViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(ContactViewModel.class);
    }

    private String scanResult;

    @Override
    public void initData(Bundle savedInstanceState) {
        mBinding.titleBar.ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mBinding.titleBar.title.setText(getResources().getString(R.string.scan));
        mBinding.zxingview.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                Log.i(TAG, "result:" + result);
                if (TextUtils.isEmpty(result)) {
                    showShortToast(getResources().getString(R.string.scan_cannot_identify));
                } else {
                    vibrate();
                    if (result.contains(Config.QR_SEPECIFIC_CODE)) {
                        mViewModel.getAppDownLoadUrl();
                        scanResult = result;
                    } else {
                        Timer timer = new Timer();//?????????Timer???
                        timer.schedule(new TimerTask() {
                            public void run() {
                                mBinding.zxingview.startSpot(); // ????????????
                            }
                        }, 2000);
                        showShortToast(getResources().getString(R.string.only_scan_related_my_app));
                    }
                }
            }

            @Override
            public void onCameraAmbientBrightnessChanged(boolean isDark) {
                // ???????????????????????????????????????????????????????????????????????????????????????????????? isDark ?????????????????????????????????
                String tipText = mBinding.zxingview.getScanBoxView().getTipText();
                String ambientBrightnessTip = "\n?????????????????????????????????";
                if (isDark) {
                    if (!tipText.contains(ambientBrightnessTip)) {
                        mBinding.zxingview.getScanBoxView().setTipText(tipText + ambientBrightnessTip);
                    }
                } else {
                    if (tipText.contains(ambientBrightnessTip)) {
                        tipText = tipText.substring(0, tipText.indexOf(ambientBrightnessTip));
                        mBinding.zxingview.getScanBoxView().setTipText(tipText);
                    }
                }
            }

            @Override
            public void onScanQRCodeOpenCameraError() {
                Log.e(TAG, "??????????????????");
            }
        });

        mBinding.cbOpenLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mBinding.zxingview.openFlashlight();// ???????????????
                } else {
                    mBinding.zxingview.closeFlashlight();// ???????????????
                }
            }
        });

        mBinding.ivGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyPhotos.createAlbum(ScanActivity.this, false, GlideEngine.getInstance())
                        .setCount(1)
                        .filter(Type.image())
                        .setGif(false)
                        .setPuzzleMenu(false)
                        .setCleanMenu(false)
                        .start(new SelectCallback() {
                            @Override
                            public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
//                                mBinding.zxingview.startSpotAndShowRect(); // ?????????????????????????????????
                                mBinding.zxingview.decodeQRCode(paths.get(0));
                            }
                        });
            }
        });
    }

    private static final String TAG = "ScanActivity";

    @Override
    public void initViewObservable() {
        mViewModel.uc.getAppDownLoadUrl.observe(this, this::handleGetAppDownloadUrl);
        mViewModel.uc.searchFriend.observe(this, this::handleSearchFriend);
    }

    private void handleSearchFriend(FriendEntity friendEntity) {
        if (friendEntity == null) {
            showShortToast("?????????????????????????????????");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(AddFriendActivity.TAG_FRIEND, friendEntity);
        startActivity(NewFriendActivity.class, bundle);
        finish();
    }

    private void handleGetAppDownloadUrl(AppDownloadUrlEntity appDownloadUrlEntity) {
        scanResult = scanResult.replaceAll(QR_SEPECIFIC_CODE, "");
        String[] results = scanResult.split("&mobile=");
        String mobile;
        if (results.length == 2) {
            mobile = results[1];
            FriendEntity friendEntity = FriendDaoManager.getInstance().searchByAccount(mobile);
            if (friendEntity != null) {
                if (friendEntity.getIsBlack() == 2) {
                    showShortToast(getResources().getString(R.string.he_is_in_your_black_list));
                } else {
                    showShortToast(getResources().getString(R.string.already_exist_friend));
                }
            } else {
                mViewModel.searchFriend(mobile);
            }
        } else {
            dismissDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBinding.zxingview.startCamera(); // ????????????????????????????????????????????????????????????
        mBinding.zxingview.startSpotAndShowRect(); // ?????????????????????????????????
    }

    @Override
    protected void onStop() {
        mBinding.zxingview.stopCamera(); // ?????????????????????????????????????????????
        Log.e(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mBinding.zxingview.onDestroy(); // ???????????????????????????
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }
}
