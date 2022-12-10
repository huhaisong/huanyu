package caixin.android.com.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import caixin.android.com.base.BaseModel;
import caixin.android.com.base.BaseViewModel;
import caixin.android.com.entity.MyPlatformEntity;
import caixin.android.com.entity.SendMessageResponse;
import caixin.android.com.http.UserCenterModel;
import caixin.android.com.utils.MMKVUtil;

import java.util.List;

public class ConversationViewModel extends BaseViewModel<UserCenterModel> {

    public ConversationViewModel.UIChangeObservable uc = new ConversationViewModel.UIChangeObservable();

    public class UIChangeObservable {
        public MutableLiveData<List<SendMessageResponse>> init = new MutableLiveData<>();
        public MutableLiveData<Object> deleteConversation = new MutableLiveData<>();
        public MutableLiveData<MyPlatformEntity> mPlatformsLiveData = new MutableLiveData<>();
    }

    public ConversationViewModel(@NonNull Application application, UserCenterModel model) {
        super(application, model);
    }

    public void deleteConversation(int id, int type,int messageId) {
        showDialog();
        mModel.httpDeleteConversation(this, id, type,messageId, new BaseModel.Callback() {
            @Override
            public void onSuccess(Object data, String mes) {
                uc.deleteConversation.postValue(data);
                dismissDialog();
            }

            @Override
            public void onFailure(String msg) {
                showShortToast(msg);
                dismissDialog();
            }

            @Override
            public void onDisconnected(String msg) {
                dismissDialog();
                showShortToast(msg);
            }
        });
    }

    public void init() {
        mModel.httpGetConversation(this, MMKVUtil.getUserInfo().getId(), 200, new BaseModel.Callback() {
            @Override
            public void onSuccess(Object data, String mes) {
                uc.init.postValue((List<SendMessageResponse>) data);
                dismissDialog();
            }

            @Override
            public void onFailure(String msg) {
                showShortToast(msg);
                dismissDialog();
            }

            @Override
            public void onDisconnected(String msg) {
                dismissDialog();
                showShortToast(msg);
            }
        });
    }



    public void getPlatUrl() {
        mModel.getPlatforms(this, MMKVUtil.getUserInfo().getId(), new BaseModel.Callback<MyPlatformEntity>() {
            @Override
            public void onSuccess(MyPlatformEntity data, String msg) {
                uc.mPlatformsLiveData.postValue(data);
                showShortToast(msg);
            }

            @Override
            public void onFailure(String msg) {
                showShortToast(msg);
            }

            @Override
            public void onDisconnected(String msg) {
            }
        });
    }
}
