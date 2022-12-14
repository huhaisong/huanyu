package caixin.android.com.view.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.lifecycle.ViewModelProviders;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.entity.ImageResponse;
import caixin.android.com.entity.UserInfoEntity;
import caixin.android.com.utils.ImgLoader;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.widget.EditNoteDialog;
import caixin.android.com.widget.GlideEngine;
import caixin.android.com.viewmodel.UserInfoViewModel;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityUserinfoBinding;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class UserInfoActivity extends BaseActivity<ActivityUserinfoBinding, UserInfoViewModel> {
    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_userinfo;
    }

    @Override
    public UserInfoViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(UserInfoViewModel.class);
    }

    private File headFile;

    @Override
    protected void onResume() {
        super.onResume();
        update(MMKVUtil.getUserInfo());
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        mBinding.rlHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyPhotos.createAlbum(UserInfoActivity.this, true, GlideEngine.getInstance())
                        .setCount(1)
                        .isCrop(true)
                        .setFreeStyleCropEnabled(true)
                        .enableSingleCheckedBack(true)
                        .filter(Type.image())
                        .setGif(true)
                        .setPuzzleMenu(false)
                        .setCleanMenu(false)
                        .start(new SelectCallback() {
                            @Override
                            public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
                                headFile = null;
                                Luban.with(UserInfoActivity.this)
                                        .load(paths)
                                        .ignoreBy(100)
                                        .setFocusAlpha(false)
                                        .filter(path -> !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif")))
                                        .setCompressListener(new OnCompressListener() {
                                            @Override
                                            public void onStart() {
                                            }

                                            @Override
                                            public void onSuccess(File file) {
                                                headFile = file;
                                                mViewModel.publishPicture(file);
//                                                mViewModel.modifyUserHead(MMKVUtil.getUserInfo().getId(), ImageUtil.imageFileToString(file.getAbsolutePath()));
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                            }
                                        }).launch();
                            }
                        });
            }
        });
        mBinding.rlNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(EditNickNameActivity.class);
            }
        });
        mBinding.titleBar.ivClose.setOnClickListener(v -> onBackPressed());
        mBinding.rlQrCode.setOnClickListener(v -> startActivity(MyQRCodeActivity.class));
        mBinding.rlSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(EditSignatureActivity.class);
            }
        });
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.editHeader.observe(this, this::handlerEditHeader);
        mViewModel.uc.publishImg.observe(this, this::handlePublishImg);
    }

    private void handlePublishImg(List<String> strings) {
        mViewModel.modifyUserHead(strings.get(0));
    }

    private void handlerEditHeader(Object o) {
        if (headFile != null) {
            ImgLoader.GlideLoadCircle(mBinding.ivAvatar, headFile, R.mipmap.img_user_head);
        }
    }

    private void update(UserInfoEntity userResponse) {
        ImgLoader.GlideLoadCircle(mBinding.ivAvatar, userResponse.getImg(), R.mipmap.img_user_head);
        mBinding.tvNickName.setText(userResponse.getNikeName());
        mBinding.tvMobile.setText(userResponse.getMobile());
        mBinding.tvInvitationCode.setText(userResponse.getIncode());
        mBinding.tvMySignature.setText(userResponse.getSignature());
    }
}
