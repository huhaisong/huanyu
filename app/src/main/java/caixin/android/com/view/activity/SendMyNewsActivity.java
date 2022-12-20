package caixin.android.com.view.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.ViewModelProviders;

import com.caixin.huanyu.R;
import com.caixin.huanyu.databinding.ActivitySendMyNewsBinding;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.huantansheng.easyphotos.utils.bitmap.BitmapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.entity.AddMyNewsRequest;
import caixin.android.com.utils.ActionUtil;
import caixin.android.com.utils.FileUtil;
import caixin.android.com.utils.GlideEngine;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.viewmodel.FriendCommunityViewModel;
import caixin.android.com.widget.UploadImage;
import caixin.android.com.widget.UploadPictureContainer;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class SendMyNewsActivity extends BaseActivity<ActivitySendMyNewsBinding, FriendCommunityViewModel> {

    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_send_my_news;
    }

    @Override
    public FriendCommunityViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(FriendCommunityViewModel.class);
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        StatusBarUtils.immersive(this, this.getResources().getColor(R.color.colorPrimary));
        mBinding.ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mBinding.upcImgContainer.setMaxCount(9);
        mBinding.upcImgContainer.setOnUploadPictureListener(new UploadPictureContainer.OnUploadPictureListener() {
            @Override
            public void onDeleteClick(UploadImage uploadImage) {
                mBinding.upcImgContainer.removeImage(uploadImage);
            }

            @Override
            public void onAddClick() {
                EasyPhotos.createAlbum(SendMyNewsActivity.this, true, GlideEngine.getInstance())
                        .setCount(9)
                        .filter(Type.image())
                        .setGif(false)
                        .setPuzzleMenu(false)
                        .setCleanMenu(false)
                        .start(new SelectCallback() {
                            @Override
                            public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
                                new Thread(() -> zipPic(paths)).start();
                            }
                        });
            }

            @Override
            public void onPicClick() {

            }
        });

        mBinding.tvSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
                if (addMyNewsRequest == null)
                    addMyNewsRequest = new AddMyNewsRequest();
                addMyNewsRequest.setContent(mBinding.etContent.getText().toString());
                addMyNewsRequest.setToken(MMKVUtil.getToken());
                List<File> bitmaps = new ArrayList<>();
                if (mBinding.upcImgContainer.getUploadImages() != null && !mBinding.upcImgContainer.getUploadImages().isEmpty()) {
                    for (int i = 0; i < mBinding.upcImgContainer.getUploadImages().size(); i++) {
                        bitmaps.add(new File(mBinding.upcImgContainer.getUploadImages().get(i).getLocalImg()));
                    }
                    mViewModel.publishPictures(bitmaps);
                } else {
                    mViewModel.addMyNews(addMyNewsRequest);
                }
            }
        });
    }

    private AddMyNewsRequest addMyNewsRequest;

    private List<String> picPath = new ArrayList<>();

    private void zipPic(List<String> list) {
        picPath.clear();
        Luban.with(this)
                .load(list)
                .ignoreBy(200)
                .setFocusAlpha(false)
                .filter(path -> !(TextUtils.isEmpty(path)))
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        picPath.add(file.getAbsolutePath());
                        if (picPath.size() == list.size()) {
                            mBinding.upcImgContainer.addImages(SendMyNewsActivity.this, picPath);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                }).launch();
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.publishPics.observe(this, this::handlePublishPics);
        mViewModel.uc.addMyNews.observe(this, this::handleAddMyNews);
    }

    private static final String TAG = "SendMyNewsActivity";

    private void handleAddMyNews(Object o) {
        finish();
    }

    private void handlePublishPics(List<String> strings) {
        addMyNewsRequest.setImgs(strings);
        mViewModel.addMyNews(addMyNewsRequest);
    }
}
