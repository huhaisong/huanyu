package caixin.android.com.view.activity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import com.caixin.huanyu.R;
import com.caixin.huanyu.databinding.ActivityAddFriendBinding;
import com.caixin.huanyu.databinding.ActivityCreateGroupBinding;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.v3.MessageDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.daomanager.FriendDaoManager;
import caixin.android.com.entity.FriendEntity;
import caixin.android.com.utils.GlideEngine;
import caixin.android.com.utils.ImgLoader;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.viewmodel.ContactViewModel;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class CreateGroupActivity extends BaseActivity<ActivityCreateGroupBinding, ContactViewModel> {

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_create_group;
    }

    @Override
    public ContactViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(ContactViewModel.class);
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        
        mBinding.etGroupName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mBinding.btnCreate.setEnabled(!TextUtils.isEmpty(s) && localAvatarFile != null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBinding.btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewModel.publishPicture(localAvatarFile);
            }
        });

        mBinding.ivGroupAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyPhotos.createAlbum(CreateGroupActivity.this, false, GlideEngine.getInstance())
                        .filter(Type.image())
                        .setGif(false)
                        .isCrop(true)
                        .setPuzzleMenu(false)
                        .setCleanMenu(false)
                        .start(new SelectCallback() {
                            @Override
                            public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
                                new Thread(() -> zipPic(paths)).start();
                            }
                        });
            }
        });

        mBinding.ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private File localAvatarFile;

    private void zipPic(List<String> list) {
        Luban.with(this)
                .load(list)
                .ignoreBy(100)
                .setFocusAlpha(false)
                .filter(path -> !(TextUtils.isEmpty(path)))
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(file.getAbsolutePath(), options); // 此时返回的bitmap为null
                        localAvatarFile = file;
                        ImgLoader.normalGlide(mBinding.ivGroupAvatar, localAvatarFile, R.mipmap.add);
                        mBinding.btnCreate.setEnabled(!TextUtils.isEmpty(mBinding.etGroupName.getText().toString()) && localAvatarFile != null);
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissDialog();
                    }
                }).launch();
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.publishImg.observe(this, this::handlePublishImg);
        mViewModel.uc.createGroup.observe(this, this::handleCreateGroup);
    }

    private void handleCreateGroup(Object o) {
        showShortToast("创建成功");
    }

    private void handlePublishImg(List<String> strings) {
        mViewModel.createGroup(mBinding.etGroupName.getText().toString(), strings.get(0));
    }

}
