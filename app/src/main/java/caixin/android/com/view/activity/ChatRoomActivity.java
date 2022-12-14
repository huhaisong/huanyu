package caixin.android.com.view.activity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.ToastUtils;

import caixin.android.com.adapter.ImRoomAdapter;
import caixin.android.com.adapter.LikeEmojiAdapter;
import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.constant.Contact;
import caixin.android.com.constant.Extras;
import caixin.android.com.daomanager.ConversationDaoManager;
import caixin.android.com.entity.CollectRequest;
import caixin.android.com.entity.DeleteMessageResponse;
import caixin.android.com.entity.LikeEmojiEntity;
import caixin.android.com.entity.MemberEntity;
import caixin.android.com.entity.base.BaseWebSocketResponse;
import caixin.android.com.entity.chatroom.MessageResponse;
import caixin.android.com.http.UserCenterModel;
import caixin.android.com.utils.ActionUtil;
import caixin.android.com.utils.FaceUtil;
import caixin.android.com.utils.ImgLoader;
import caixin.android.com.utils.StatusBarUtils;
import caixin.android.com.view.fragment.LiveRedPackRobDialogFragment;
import caixin.android.com.widget.TopGradual;
import caixin.android.com.adapter.ImChatFacePagerAdapter;
import caixin.android.com.base.BaseActivity;
import caixin.android.com.entity.GroupAdEntity;
import caixin.android.com.entity.SendMessageResponse;
import caixin.android.com.http.WebSocketManager;
import caixin.android.com.utils.AdvertisingUtil;
import caixin.android.com.utils.ClickUtil;
import caixin.android.com.utils.DpUtil;
import caixin.android.com.utils.GlideEngine;
import caixin.android.com.utils.KeyBoardHeightUtil;
import caixin.android.com.utils.MMKVUtil;
import caixin.android.com.utils.TextRender;
import caixin.android.com.viewmodel.ChatViewModel;
import caixin.android.com.widget.ChatFaceDialog;
import caixin.android.com.widget.ChatMoreDialog;
import caixin.android.com.widget.KeyBoardHeightChangeListener;
import caixin.android.com.widget.MyImageView;
import caixin.android.com.widget.OnFaceClickListener;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.ActivityChatRoomBinding;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.constant.Capture;
import com.huantansheng.easyphotos.constant.Type;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.kongzue.dialog.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialog.util.BaseDialog;
import com.kongzue.dialog.util.DialogSettings;
import com.kongzue.dialog.v3.CustomDialog;
import com.kongzue.dialog.v3.MessageDialog;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import caixin.android.com.widget.ait.MyTextSpan;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

@SuppressLint("ClickableViewAccessibility")
public class ChatRoomActivity extends BaseActivity<ActivityChatRoomBinding, ChatViewModel> implements View.OnClickListener, KeyBoardHeightChangeListener, OnFaceClickListener, ChatFaceDialog.ActionListener, ChatMoreDialog.ActionListener, ImRoomAdapter.ActionListener, DataCallback, WebSocketManager.ServerMessage {

    public static final String CHATROOM_TYPE = "ChatRoomActivity_type";
    public static final String TAG_FRIEND = "ChatRoomActivity_friend";
    public static final String TAG_GROUP = "ChatRoomActivity_group";
    public static final String TAG_IS_NO_TALK = "ChatRoomActivity_TAG_IS_NO_TALK";
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_FRIEND = 2;
    public static final String TAG_TITLE = "ChatRoomActivity_TITLE";
    public static final String TAG_HEAD_IMAGE = "ChatRoomActivity_HEAD_IMAGE";
    public static final int REQUEST_CODE_GROUP_DETAIL = 100;
    private static final int REQUEST_CODE_SEND_RED_PACK = 0x3;
    private InputMethodManager imm;
    private ChatFaceDialog mChatFaceDialog;//表情弹窗
    private ChatMoreDialog mMoreDialog;//更多弹窗
    private View mMoreView;//更多控件
    private View mFaceView;//表情控件
    protected KeyBoardHeightUtil mKeyBoardHeightUtil;
    private int mFaceViewHeight;//表情控件高度
    private int mMoreViewHeight;//更多控件的高度
    private ImRoomAdapter mAdapter;
    public static boolean isFront = false;
    public static ChatRoomActivity INSTANCE;
    private int groupId;
    private int friendId;
    private String otherHeadImage;

    public static ChatRoomActivity getInstance() {
        return INSTANCE;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getFriendId() {
        return friendId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        INSTANCE = null;
        isFront = false;
        if (mType == TYPE_FRIEND) {
            WebSocketManager.getInstance().removeServerReceive("FRIEND" + friendId);
        } else if (mType == TYPE_GROUP) {
            WebSocketManager.getInstance().removeServerReceive("GROUP" + groupId);
        }
        mKeyBoardHeightUtil.release();
    }

    private int mType;  //群聊还是单聊

    public int getmType() {
        return mType;
    }

    private String chatName;

    @Override
    public void initData(Bundle savedInstanceState) {
        INSTANCE = this;
        
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Intent intent = getIntent();
        mType = intent.getIntExtra(ChatRoomActivity.CHATROOM_TYPE, 0);
        otherHeadImage = intent.getStringExtra(TAG_HEAD_IMAGE);
        if (mType == TYPE_FRIEND) {
            friendId = intent.getIntExtra(TAG_FRIEND, -1);
        } else if (mType == TYPE_GROUP) {
            groupId = intent.getIntExtra(TAG_GROUP, -1);
            mViewModel.getGroupAd(groupId);
        } else {
            caixin.android.com.utils.ToastUtils.show("打开异常！");
            finish();
        }
        chatName = intent.getStringExtra(ChatRoomActivity.TAG_TITLE);
        mBinding.title.setText(chatName);
//        skipIds = new TextView[]{mBinding.skip1, mBinding.skip2, mBinding.skip3};
        mBinding.recyclerView.setHasFixedSize(true);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBinding.recyclerView.addItemDecoration(new TopGradual());
        mBinding.recyclerView.setVerticalScrollBarEnabled(true);
        mBinding.ivClose.setOnClickListener(v -> onBackPressed());
        mBinding.ivScrollToBottom.setOnClickListener(v -> chatScrollToBottom());
        mBinding.edit.setOnClickListener(this);
        mBinding.edit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String content = mBinding.edit.getText().toString().trim();
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return true;
                }
                if (TextUtils.isEmpty(content)) {
                    ToastUtils.showShort(R.string.content_empty);
                    return true;
                }
                if (mType == TYPE_FRIEND) {
                    mViewModel.sendMessageToFriend(content, friendId);
                } else if (mType == TYPE_GROUP) {
                    mViewModel.sendMessageToGroup(content, groupId, mBinding.edit.getUserIdString());
                }
                mBinding.edit.setText("");
                return true;
            }
            return false;
        });
        mBinding.btnAdd.setOnClickListener(this);
        mBinding.btnFace.setOnClickListener(this);
        mBinding.recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                return hideSoftInput() || hideFace() || hideMore();
            }
            return false;
        });
        mKeyBoardHeightUtil = new KeyBoardHeightUtil(this, findViewById(android.R.id.content), this);
        mBinding.container.postDelayed(() -> {
            if (mKeyBoardHeightUtil != null) {
                mKeyBoardHeightUtil.start();
            }
        }, 500);
        mAdapter = new ImRoomAdapter(this);
        mAdapter.setActionListener(this);
        mBinding.recyclerView.setAdapter(mAdapter);
        mBinding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int lastItemPosition = ((LinearLayoutManager) mBinding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                Log.e(TAG, "onScrolled: size = " + mAdapter.getList().size() + ",lastItemPosition = " + lastItemPosition);
                if (lastItemPosition >= mAdapter.getList().size() - 1) {
                    mBinding.ivScrollToBottom.setVisibility(View.GONE);
                    unreadMessage = 0;
                    mBinding.ivScrollToBottom.setUnreadCount(unreadMessage);
                } else {
                    mBinding.ivScrollToBottom.setVisibility(View.VISIBLE);
                    if (unreadMessage > 0) {
                        if (unreadMessage > mAdapter.getList().size() - 1 - lastItemPosition) {
                            unreadMessage = mAdapter.getList().size() - 1 - lastItemPosition;
                            if (unreadMessage < 0)
                                unreadMessage = 0;
                            mBinding.ivScrollToBottom.setUnreadCount(unreadMessage);
                        }
                    }
                }
            }
        });
        mBinding.tvNotify.setSelected(true);
        ClassicsHeader classicsHeader = new ClassicsHeader(this);
        classicsHeader.setTextSizeTitle(12.0f);
        classicsHeader.setTextSizeTime(10.0f);
        mBinding.srlRefresh.setRefreshHeader(classicsHeader);
        mBinding.srlRefresh.setHeaderHeight(30);
        mBinding.srlRefresh.setEnableRefresh(true);
        mBinding.srlRefresh.setOnRefreshListener(refreshLayout -> {
            if (mAdapter != null && mAdapter.getOldestMessage() != null) {
                if (mAdapter.getOldestMessage() == null) {
                    refreshLayout.finishRefresh();
                }
                refreshLayout.finishRefresh(5000); //这个记得设置，否则一直转圈
                if (mType == TYPE_FRIEND) {
                    mViewModel.getMoreFriendMessage(friendId, Contact.HISTORY_MESSAGE_SIZE, mAdapter.getOldestMessage().getId().intValue());
                } else if (mType == TYPE_GROUP) {
                    mViewModel.getMoreGroupMessage(groupId, Contact.HISTORY_MESSAGE_SIZE, mAdapter.getOldestMessage().getId().intValue());
                }
            }
        });
        mBinding.edit.setOnTouchListener((v, event) -> {
            clickInput();
            return false;
        });
        mBinding.tvSend.setOnClickListener(v -> {
            String content = mBinding.edit.getText().toString().trim();
            if (ActionUtil.isLogin()) {
                ToastUtils.showShort("请先登录!");
                return;
            }
            if (TextUtils.isEmpty(content)) {
                ToastUtils.showShort(R.string.content_empty);
                return;
            }
            if (mType == TYPE_FRIEND) {
                mViewModel.sendMessageToFriend(content, friendId);
            } else if (mType == TYPE_GROUP) {
                mViewModel.sendMessageToGroup(content, groupId, mBinding.edit.getUserIdString());
            }
            mBinding.edit.setText("");
        });
        if (mType == TYPE_FRIEND) {
            mBinding.ivGroupDetail.setVisibility(View.GONE);
        } else if (mType == TYPE_GROUP) {
            mBinding.ivGroupDetail.setVisibility(View.VISIBLE);
            mBinding.ivGroupDetail.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt(TAG_GROUP, groupId);
                bundle.putString(ChatRoomActivity.TAG_TITLE, chatName);
                if (mAdapter.getList() != null && mAdapter.getList().size() > 0) {
                    int newestMessageId = mAdapter.getList().get(mAdapter.getList().size() - 1).getId().intValue();
                    bundle.putInt("messageId", newestMessageId);
                }
                bundle.putBoolean(TAG_IS_NO_TALK, isNoTalk);
                bundle.putString(ChatRoomActivity.TAG_HEAD_IMAGE, otherHeadImage);
                startActivityForResult(GroupDetailActivity.class, bundle, REQUEST_CODE_GROUP_DETAIL);
            });
        }
        mBinding.edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (before == 1 && count == 0) {
                    MyTextSpan[] spans = mBinding.edit.getText().getSpans(0, mBinding.edit.getText().length(), MyTextSpan.class);
                    Log.e(TAG, "onTextChanged: " + spans.length);
                    for (MyTextSpan myImageSpan : spans) {
                        if (mBinding.edit.getText().getSpanEnd(myImageSpan) == start && !s.toString().endsWith(myImageSpan.getShowText())) {
                            mBinding.edit.getText().delete(mBinding.edit.getText().getSpanStart(myImageSpan), mBinding.edit.getText().getSpanEnd(myImageSpan));
                            break;
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mViewModel.getLikeEmoji();
        UserCenterModel.getInstance().login(MMKVUtil.getUserInfo().getId());
    }


    @Override
    public int initContentView(Bundle savedInstanceState) {
        return R.layout.activity_chat_room;
    }

    @Override
    public ChatViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getApplication());
        return ViewModelProviders.of(this, factory).get(ChatViewModel.class);
    }


    private boolean isOnPause = false;
    private boolean isNoTalk = false;

    @Override
    protected void onResume() {
        super.onResume();
        isNoTalk = false;
        unreadMessage = 0;
        if (!isOnPause) {
            chatScrollToBottom();
        }
        isOnPause = false;
        isFront = true;
        if (mType == TYPE_FRIEND) {
            WebSocketManager.getInstance().addServerReceive("FRIEND" + friendId, this);
        } else if (mType == TYPE_GROUP) {
            WebSocketManager.getInstance().addServerReceive("GROUP" + groupId, this);
        }
        mBinding.llChatInput.setVisibility(View.VISIBLE);
        mBinding.llNoTalk.setVisibility(View.GONE);

        if (MMKVUtil.getSelectedPicChannel() != 0) {
            if (mAdapter != null) {
                mAdapter.setSelectedPicChannel(MMKVUtil.getSelectedPicChannel());
                mAdapter.setPicChannel(MMKVUtil.getPicChannel());
            }
        }
    }


    private static final String TAG = "ChatActivity";

    /**
     * 隐藏键盘
     */
    private boolean hideSoftInput() {
        if (((KeyBoardHeightChangeListener) this).isSoftInputShowed() && imm != null && mBinding.edit != null) {
            imm.hideSoftInputFromWindow(mBinding.edit.getWindowToken(), 0);
            return true;
        }
        return false;
    }

    @Override
    public void onKeyBoardHeightChanged(int visibleHeight, int keyboardHeight) {
        if ((keyboardHeight == 0 && isPopWindowShowed())) {
            return;
        }
        Log.e(TAG, "onKeyBoardHeightChanged: ");
        onKeyBoardChanged(0);
    }

    public boolean isPopWindowShowed() {
        return mChatFaceDialog != null || mMoreDialog != null;
    }

    @Override
    public boolean isSoftInputShowed() {
        if (mKeyBoardHeightUtil != null) {
            return mKeyBoardHeightUtil.isSoftInputShowed();
        }
        return false;
    }


    /**
     * 隐藏表情弹窗
     */
    private boolean hideFace() {
        if (mChatFaceDialog != null) {
            mChatFaceDialog.dismiss();
            mChatFaceDialog = null;
            return true;
        }
        return false;
    }

    /**
     * 隐藏更多弹窗
     */
    private boolean hideMore() {
        if (mMoreDialog != null) {
            mMoreDialog.dismiss();
            mMoreDialog = null;
            return true;
        }
        return false;
    }

    /**
     * 聊天栏滚到最底部
     */
    public void chatScrollToBottom() {
        if (mAdapter != null) {
            mAdapter.scrollToBottom();
        }
    }

    public void GoneScrollToBottomView() {
        mBinding.ivScrollToBottom.setVisibility(View.GONE);
        unreadMessage = 0;
        mBinding.ivScrollToBottom.setUnreadCount(unreadMessage);
    }

    @Override
    public void onClick(View v) {
        if (!ClickUtil.canClick()) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_send:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                String content = mBinding.edit.getText().toString().trim();
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                if (TextUtils.isEmpty(content)) {
                    ToastUtils.showShort(R.string.content_empty);
                    return;
                }
                if (mType == TYPE_FRIEND) {
                    mViewModel.sendMessageToFriend(content, friendId);
                } else if (mType == TYPE_GROUP) {
                    mViewModel.sendMessageToGroup(content, groupId, mBinding.edit.getUserIdString());
                }
                mBinding.edit.setText("");
                break;
            case R.id.btn_exclusive_red_pack:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                if (MMKVUtil.getUserInfo().getSpay() == 0) {
                    MessageDialog.build(this)
                            .setStyle(DialogSettings.STYLE.STYLE_IOS)
                            .setTitle("提示")
                            .setMessage("您还未设置过支付密码，暂不能进行余额兑换，是否现在去设置？")
                            .setOkButton("立即设置", new OnDialogButtonClickListener() {
                                @Override
                                public boolean onClick(BaseDialog baseDialog, View v) {
                                    startActivity(SetPayPassActivity.class);
                                    return false;
                                }
                            })
                            .setCancelButton("我再看看")
                            .show();
                    return;
                }
                if (mType == TYPE_FRIEND) {
                    SendRedPackActivity.navTo(this, mType, friendId, true);
                } else {
                    SendRedPackActivity.navTo(this, mType, groupId, true);
                }
                break;
            case R.id.btn_red_pack:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                if (MMKVUtil.getUserInfo().getSpay() == 0) {
                    MessageDialog.build(this)
                            .setStyle(DialogSettings.STYLE.STYLE_IOS)
                            .setTitle("提示")
                            .setMessage("您还未设置过支付密码，暂不能进行余额兑换，是否现在去设置？")
                            .setOkButton("立即设置", new OnDialogButtonClickListener() {
                                @Override
                                public boolean onClick(BaseDialog baseDialog, View v) {
                                    startActivity(SetPayPassActivity.class);
                                    return false;
                                }
                            })
                            .setCancelButton("我再看看")
                            .show();
                    return;
                }
                if (mType == TYPE_FRIEND) {
                    SendRedPackActivity.navTo(this, mType, friendId);
                } else {
                    SendRedPackActivity.navTo(this, mType, groupId);
                }
                break;
            case R.id.btn_face:
                faceClick();
                break;
            case R.id.edit:
                clickInput();
                break;
            case R.id.btn_add:
                clickMore();
                break;
            case R.id.btn_ziliao:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                isZiliao = true;
                choosePicture();
                break;
            case R.id.btn_img:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                isZiliao = false;
                choosePicture();
                break;
            case R.id.btn_camera:
                if (ActionUtil.isLogin()) {
                    ToastUtils.showShort("请先登录!");
                    return;
                }
                takePicture();
                break;
        }
    }

    private void choosePicture() {
        EasyPhotos.createAlbum(this, false, GlideEngine.getInstance())
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

    /**
     * 调用摄像头拍照
     */
    private void takePicture() {
        hideMore();
        hideFace();
        EasyPhotos.createCamera(this)
                .isCrop(false)
                .enableSingleCheckedBack(true)
//                .enableSystemCamera(true)
                .setCapture(Capture.IMAGE)
                .start(new SelectCallback() {
                    @Override
                    public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
                        new Thread(() -> zipPic(paths)).start();
                    }
                });
    }

    private int picWidth;
    private int picHeight;

    private void zipPic(List<String> list) {
        showDialog("发送中...");
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
                        picWidth = options.outWidth;
                        picHeight = options.outHeight;
                        Log.e(TAG, "onSuccess: picWidth = " + picHeight + ",picWidth = " + picWidth);
                        mViewModel.publishChatPicture(file, isZiliao);
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissDialog();
                    }
                }).launch();
    }

    /**
     * 点击表情按钮
     */
    private void faceClick() {
        if (mBinding.btnFace.isChecked()) {
            hideSoftInput();
            showFace();
        } else {
            mNeedToBottom = false;
            hideFace();
            showSoftInput();
        }
    }

    /**
     * 显示软键盘
     */
    private void showSoftInput() {
        if (!isSoftInputShowed() && imm != null && mBinding.edit != null) {
            imm.showSoftInput(mBinding.edit, InputMethodManager.SHOW_FORCED);
            mBinding.edit.requestFocus();
        }
    }

    /**
     * 显示表情弹窗
     */
    private void showFace() {
        if (mChatFaceDialog != null && mChatFaceDialog.isShowing()) {
            return;
        }
        if (mMoreDialog != null) {
            mNeedToBottom = false;
            hideMore();
        }
        if (mFaceView == null) {
            mFaceView = initFaceView();
        }
        onPopupWindowChanged(mFaceViewHeight);
        mChatFaceDialog = new ChatFaceDialog(mBinding.container.getRootView(), mFaceView, true, this);
        mChatFaceDialog.show();
        mNeedToBottom = true;
    }

    private View defaultEmojiView;
    private View mineEmojiView;

    /**
     * 初始化表情控件
     */
    @SuppressLint("ResourceType")
    private View initFaceView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.view_chat_face, null);
        v.measure(0, 0);
        mFaceViewHeight = v.getMeasuredHeight();
        v.findViewById(R.id.btn_send).setOnClickListener(this);
        ((RadioButton) v.findViewById(R.id.rbtn_emoji_default)).setChecked(true);
        final RadioGroup indicatorRadioGroup = v.findViewById(R.id.radio_group);
        if (defaultEmojiView == null)
            defaultEmojiView = initDefaultEmojiView(indicatorRadioGroup);
        FrameLayout frameLayout = v.findViewById(R.id.fl_container);
        frameLayout.removeAllViews();
        frameLayout.addView(defaultEmojiView);
        RadioGroup frameLayoutRadioGroup = v.findViewById(R.id.rg_fragment);
        frameLayoutRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rbtn_emoji_default:
                        if (defaultEmojiView == null)
                            defaultEmojiView = initDefaultEmojiView(indicatorRadioGroup);
                        frameLayout.removeAllViews();
                        frameLayout.addView(defaultEmojiView);
                        indicatorRadioGroup.setVisibility(View.VISIBLE);
                        break;
                    case R.id.rbtn_emoji_mine:
                        if (mineEmojiView == null) {
                            mineEmojiView = initMineEmojiView();
                        }
                        frameLayout.removeAllViews();
                        frameLayout.addView(mineEmojiView);
                        indicatorRadioGroup.setVisibility(View.GONE);
                        break;
                }
            }
        });
        indicatorRadioGroup.check(10000);
        return v;
    }

    private LikeEmojiAdapter likeEmojiAdapter;
    private RecyclerView likeFaceRecyclerView;
    private List<LikeEmojiEntity> likeEmojiEntities = new ArrayList<>();

    private View initMineEmojiView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.fragment_like_emoji, null);
        likeFaceRecyclerView = view.findViewById(R.id.list);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        likeFaceRecyclerView.setLayoutManager(layoutManager);
        likeEmojiAdapter = new LikeEmojiAdapter(R.layout.item_like_emoji_horizontal_layout);
        if (likeEmojiEntities != null)
            likeEmojiAdapter.setNewData(likeEmojiEntities);
        likeEmojiAdapter.setEmptyView(View.inflate(this, R.layout.empty_layout, null));
        likeEmojiAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Log.e(TAG, "onItemClick: " + position);
                if (position == 0) {
                    EditEmojiActivity.navToForResult(ChatRoomActivity.this, likeEmojiEntities);
                    return;
                }
                showDialog("正在发送...");
                if (mType == TYPE_FRIEND) {
                    mViewModel.sendPicToFriend(((LikeEmojiEntity) adapter.getData().get(position)).getSrc(), "", friendId, 140, 140, false);
                } else if (mType == TYPE_GROUP) {
                    mViewModel.sendPicToGroup(((LikeEmojiEntity) adapter.getData().get(position)).getSrc(), "", groupId, 140, 140, false);
                }
            }
        });
        likeFaceRecyclerView.setAdapter(likeEmojiAdapter);
        return view;
    }

    private View initDefaultEmojiView(RadioGroup radioGroup) {
        LayoutInflater inflater = LayoutInflater.from(this);
        radioGroup.removeAllViews();
        View view = inflater.inflate(R.layout.fragment_default_emoji, null);
        ViewPager viewPager = view.findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(10);
        ImChatFacePagerAdapter adapter = new ImChatFacePagerAdapter(this, this);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((RadioButton) radioGroup.getChildAt(position)).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        for (int i = 0, pageCount = adapter.getCount(); i < pageCount; i++) {
            RadioButton radioButton = (RadioButton) inflater.inflate(R.layout.view_chat_indicator, radioGroup, false);
            radioButton.setId(i + 10000);
            if (i == 0) {
                radioButton.setChecked(true);
            }
            radioGroup.addView(radioButton);
        }
        return view;
    }


    /**
     * 初始化更多控件
     */
    private View initMoreView() {
        View v = LayoutInflater.from(this).inflate(R.layout.view_chat_more, null);
        mMoreViewHeight = DpUtil.dp2px(200);
        v.findViewById(R.id.btn_img).setOnClickListener(this);
        v.findViewById(R.id.btn_camera).setOnClickListener(this);
        v.findViewById(R.id.btn_red_pack).setOnClickListener(this);
        v.findViewById(R.id.btn_ziliao).setOnClickListener(this);
        v.findViewById(R.id.btn_exclusive_red_pack).setOnClickListener(this);
        if (mType == TYPE_FRIEND) {
            v.findViewById(R.id.btn_exclusive_red_pack).setVisibility(View.INVISIBLE);
        } else {
            v.findViewById(R.id.btn_exclusive_red_pack).setVisibility(View.VISIBLE);
        }
        return v;
    }

    /**
     * 点击输入框
     */
    private void clickInput() {
        mNeedToBottom = false;
        hideFace();
        hideMore();
    }

    /**
     * 点击更多按钮
     */
    private void clickMore() {
        hideSoftInput();
        showMore();
    }

    /**
     * 显示更多弹窗
     */
    private void showMore() {
        if (mMoreDialog != null && mMoreDialog.isShowing()) {
            return;
        }
        if (mChatFaceDialog != null) {
            mNeedToBottom = false;
            hideFace();
        }
        if (mMoreView == null) {
            mMoreView = initMoreView();
        }
        onPopupWindowChanged(mMoreViewHeight);
        mMoreDialog = new ChatMoreDialog(mBinding.container.getRootView(), mMoreView, true, this);
        mMoreDialog.show();
        mNeedToBottom = true;
    }

    private boolean mNeedToBottom;//是否需要去底部

    /**
     * 点击表情图标按钮
     */
    @Override
    public void onFaceClick(String str, int faceImageRes) {
        if (mBinding.edit != null) {
            Editable editable = mBinding.edit.getText();
            if (str.equals("<")) {
                editable.insert(mBinding.edit.getSelectionStart(), TextRender.getFaceImageSpan(str, faceImageRes));
            } else {
                editable.append(str);
            }
        }
    }

    /**
     * 点击表情删除按钮
     */
    @Override
    public void onFaceDeleteClick() {
        if (mBinding.edit != null) {
            int selection = mBinding.edit.getSelectionStart();
            String text = mBinding.edit.getText().toString();
            if (selection > 0) {
                List<String> faces = FaceUtil.getFaceList();
                for (int i = 0; i < faces.size(); i++) {
                    if (text.endsWith(faces.get(i))) {
                        mBinding.edit.getText().delete(selection - faces.get(i).length(), selection);
                        return;
                    }
                }
                String text2 = text.substring(selection - 1, selection);
                if ("]".equals(text2)) {
                    int start = text.lastIndexOf("[", selection);
                    if (start >= 0) {
                        mBinding.edit.getText().delete(start, selection);
                    } else {
                        mBinding.edit.getText().delete(selection - 1, selection);
                    }
                } else {
                    mBinding.edit.getText().delete(selection - 1, selection);
                }

            }
        }
    }

    @Override
    public void onMoreDialogDismiss() {
        onPopupWindowChanged(0);
        mMoreDialog = null;
    }

    private void onKeyBoardChanged(int keyboardHeight) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBinding.container.getLayoutParams();
        params.setMargins(0, 0, 0, keyboardHeight);
        mBinding.container.setLayoutParams(params);
        int lastItemPosition = ((LinearLayoutManager) mBinding.recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
        Log.e(TAG, "onKeyBoardChanged: lastItemPosition = " + lastItemPosition + ",mAdapter.getList().size() = " + mAdapter.getList().size());
        if (lastItemPosition >= mAdapter.getList().size() - 5) {
            chatScrollToBottom();
        }
    }

    @Override
    public void onImageClick(MyImageView imageView, int x, int y, SendMessageResponse bean) {
        Log.e(TAG, "onImageClick: ");
        hideSoftInput();
        List<SendMessageResponse> messageResponses = mAdapter.getList();
        List<SendMessageResponse> temp = new ArrayList<>();
        List<String> gTzimglist = new ArrayList<>();
        int position = 0;
        if (messageResponses != null && messageResponses.size() > 0) {
            for (int i = 0; i < messageResponses.size(); i++) {
                if (messageResponses.get(i).getType() == MessageResponse.TYPE_IMAGE) {
                    gTzimglist.add(messageResponses.get(i).getImg().getImgurl());
                    temp.add(messageResponses.get(i));
                }
            }
            if (temp.size() > 0) {
                for (int i = 0; i < temp.size(); i++) {
                    if (temp.get(i).getId() == bean.getId()) {
                        position = i;
                    }
                }
            }
        }
        Log.e(TAG, "onImageClick: position = " + position);
        Intent intent = new Intent(ChatRoomActivity.this, PostImageDetailActivity.class);
        intent.putExtra(Extras.IMAGES, (Serializable) gTzimglist);
        intent.putExtra(Extras.POSITION, position);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAvatarLongClick(SendMessageResponse bean) {
        Log.e(TAG, "onAvatarLongClick: " + bean.toString());
        if (mType == TYPE_FRIEND)
            return;
        if (bean.isFromSelf())
            return;
        mBinding.edit.addAtSpan("@", bean.getNikeName(), bean.getSendId());
        showSoftInput();
        chatScrollToBottom();
    }

    @Override
    public void onMessageDelete(SendMessageResponse bean) {
        mViewModel.deleteMessage(bean.getId().intValue());
    }

    @Override
    public void onMessageCopy(SendMessageResponse bean) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", bean.getContents());
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    @Override
    public void onMessageCollect(SendMessageResponse bean) {
        CollectRequest collectRequest = new CollectRequest();
        ArrayList<String> imgs = new ArrayList<>();
        imgs.add(bean.getImg().getImgurl());
        collectRequest.setImgs(imgs);
        collectRequest.setText(bean.getContents());
        collectRequest.setToken(MMKVUtil.getToken());
        collectRequest.setToid(bean.getSendId());
        mViewModel.collect(collectRequest);
    }

    @Override
    public void onRedPackClick(SendMessageResponse bean, View view) {
        if (ActionUtil.isLogin()) {
            ToastUtils.showShort("请先登录!");
            return;
        }
        if (bean.getRed_status()) {
            if (mType == TYPE_FRIEND) {
                if (bean.getSendId() != MMKVUtil.getUserInfo().getId()) {
                    showRedPackRobDialog(bean);
                } else {
                    forwardRobResultDetail(bean);
                }
            } else {
                showRedPackRobDialog(bean);
            }
        } else {
            forwardRobResultDetail(bean);
        }
    }

    private void forwardRobResultDetail(SendMessageResponse bean) {
        Intent intent = new Intent(this, RedPackResultActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("pgid", bean.getPbgid());
        bundle.putString("user", bean.getNikeName());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void showRedPackRobDialog(SendMessageResponse bean) {
        LiveRedPackRobDialogFragment fragment = new LiveRedPackRobDialogFragment();
        fragment.setActionListener(new LiveRedPackRobDialogFragment.ActionListener() {
            @Override
            public void show(boolean needDelay) {
                Log.e(TAG, "show: ");
            }

            @Override
            public void hide() {

            }

            @Override
            public void onDismiss() {
                Log.e(TAG, "onDismiss: ");
                fragment.dismiss();
            }

            @Override
            public void onRobSuccess() {
                bean.setRed_status(false);
                mAdapter.notifyItem(bean);
            }
        });
        fragment.setMessageResponse(bean);
        fragment.setPgid(bean.getPbgid());
        fragment.show(getSupportFragmentManager(), "LiveRedPackRobDialogFragment");
    }

    @Override
    public void onAvatarClick(SendMessageResponse bean) {
//        UserHomeActivity.navTo(ChatRoomActivity.this, bean.getUid());
    }


    public void onPopupWindowChanged(int height) {
        onKeyBoardChanged(height);
        Log.e(TAG, "onPopupWindowChanged: ");
    }

    /**
     * 表情弹窗消失的回调
     */
    @Override
    public void onFaceDialogDismiss() {
        if (mNeedToBottom) {
            onPopupWindowChanged(0);
        }
        mChatFaceDialog = null;
        mBinding.btnFace.setChecked(false);
    }

    private boolean isFirstGetMessage = true;

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        Log.e(TAG, "onStringAvailable: I got some bytes!");
        bb.recycle();
    }


    @Override
    public void onPause() {
        super.onPause();
        isOnPause = true;
    }

    @Override
    public void initViewObservable() {
        mViewModel.uc.getHistoryFriendMessage.observe(this, this::handleGetFriendHistory);
        mViewModel.uc.getMoreFriendMessage.observe(this, this::handleGetMoreFriendMessage);
        mViewModel.uc.deleteMessage.observe(this, this::handleDeleteMessage);
        mViewModel.uc.publishImg.observe(this, this::handlePublishImg);
        mViewModel.uc.getGroupAd.observe(this, this::handleGetGroupAd);
        mViewModel.uc.isNoTalkGroup.observe(this, this::handleIsNoTalkGroup);
        mViewModel.uc.getLikeEmoji.observe(this, this::handleGetLikeEmoji);
    }


    private void handleGetLikeEmoji(List<LikeEmojiEntity> o) {
        likeEmojiEntities.clear();
        LikeEmojiEntity likeEmojiEntity = new LikeEmojiEntity();
        likeEmojiEntity.setLocalSrc(R.mipmap.add);
        likeEmojiEntities.add(likeEmojiEntity);
        if (o != null && o.size() > 0) {
            likeEmojiEntities.addAll(o);
        }
        if (likeEmojiAdapter != null && likeFaceRecyclerView != null) {
            likeEmojiAdapter.setNewData(likeEmojiEntities);
            likeFaceRecyclerView.setAdapter(likeEmojiAdapter);
        }
    }

    private void handleIsNoTalkGroup(Boolean o) {
        isNoTalk = true;
        if (o) {
            mBinding.llChatInput.setVisibility(View.GONE);
            mBinding.llNoTalk.setVisibility(View.VISIBLE);
        }
    }

    private void handleGetGroupAd(GroupAdEntity o) {
        if (o == null)
            return;
        mBinding.llNotify.setVisibility(View.VISIBLE);
        mBinding.tvNotify.setSelected(true);
        if (!TextUtils.isEmpty(o.getText()))
            mBinding.tvNotify.setText(Html.fromHtml(o.getText()));
        mBinding.tvNotify.setOnClickListener(v -> AdvertisingUtil.getInstance().SkipAdWebView(ChatRoomActivity.this, o.getUrl()));
    }

    private boolean isZiliao = false;

    private void handlePublishImg(List<String> strings) {
        if (mType == TYPE_FRIEND) {
            mViewModel.sendPicToFriend(strings.get(0), strings.get(0), friendId, picWidth, picHeight, isZiliao);
        } else if (mType == TYPE_GROUP) {
            mViewModel.sendPicToGroup(strings.get(0), strings.get(0), groupId, picWidth, picHeight, isZiliao);
        }
    }

    private void handleDeleteMessage(Object o) {
        if (o == null) {
            return;
        }
        List<DeleteMessageResponse> sendMessageResponses = JSONObject.parseArray(o.toString(), DeleteMessageResponse.class);
        DeleteMessageResponse deleteMessageResponse = sendMessageResponses.get(0);
        runOnUiThread(() -> mAdapter.deleteByMessageId(Integer.valueOf(deleteMessageResponse.getId())));
    }

    private void handleGetMoreFriendMessage(List<SendMessageResponse> o) {
        if (o == null) {
            mBinding.srlRefresh.setNoMoreData(true);
            return;
        }
        mBinding.srlRefresh.finishRefresh();
        mAdapter.addList(o);
    }

    //处理发送消息后的回调
    private void handleSendMessage(Object o) {
        if (o == null) {
            return;
        }
        List<SendMessageResponse> userResponseList = JSONObject.parseArray(o.toString(), SendMessageResponse.class);
        SendMessageResponse sendMessageResponse = userResponseList.get(0);
        if (!isRelatedCurrentAccount(sendMessageResponse))
            return;
        mAdapter.insertItem(sendMessageResponse);
        insertConversation(sendMessageResponse);
        if (!sendMessageResponse.isFromSelf()) {
            mViewModel.readMessage(sendMessageResponse.getMessageId());
        }
        if (mType == TYPE_GROUP) {
            if (mBinding.ivScrollToBottom.getVisibility() == View.VISIBLE) {
                Log.e(TAG, "handleSendMessage: unreadMessage = " + unreadMessage);
                unreadMessage++;
                mBinding.ivScrollToBottom.setUnreadCount(unreadMessage);
            } else {
                Log.e(TAG, "handleSendMessage: UNVISIBLE = ");
            }
            if (TextUtils.isEmpty(sendMessageResponse.getAssignTo()))
                return;
            String[] assignTos = sendMessageResponse.getAssignTo().split(",");
            if (assignTos.length > 0) {
                for (int i = 0; i < assignTos.length; i++) {
                    if (assignTos[i].equals("" + MMKVUtil.getUserInfo().getId())) {
                        if (mBinding.callMeLayout.getVisibility() != View.VISIBLE) {
                            mBinding.callMeLayout.setVisibility(View.VISIBLE);
                        }
                        mBinding.callMeContent.setText(sendMessageResponse.getNikeName() + "@了你");
                        mBinding.ivCloseCallMe.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBinding.callMeLayout.setVisibility(View.GONE);
                            }
                        });
                        mBinding.callMeLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mBinding.callMeLayout.setVisibility(View.GONE);
                                CustomDialog.build(ChatRoomActivity.this, R.layout.call_me_layout, new CustomDialog.OnBindView() {
                                    @Override
                                    public void onBind(final CustomDialog dialog, View v) {
                                        TextView btnOk = v.findViewById(R.id.tv_content);
                                        btnOk.setText(sendMessageResponse.getContents());
                                        btnOk.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                dialog.doDismiss();
                                            }
                                        });
                                    }
                                }).setAlign(CustomDialog.ALIGN.DEFAULT).setCancelable(true).show();
                            }
                        });
                    }
                }
            }
        }
    }

    private int unreadMessage = 0;

    private void handleSendPic(Object o) {
        if (o == null) {
            mBinding.srlRefresh.setNoMoreData(true);
            return;
        }
        List<SendMessageResponse> userResponseList = JSONObject.parseArray(o.toString(), SendMessageResponse.class);
        SendMessageResponse sendMessageResponse = userResponseList.get(0);
        if (!isRelatedCurrentAccount(sendMessageResponse))
            return;
        mAdapter.insertItem(sendMessageResponse);
        insertConversation(sendMessageResponse);
        if (mType == TYPE_GROUP) {
            if (mBinding.ivScrollToBottom.getVisibility() == View.VISIBLE) {
                unreadMessage++;
                mBinding.ivScrollToBottom.setUnreadCount(unreadMessage);
            }
        }
        if (!sendMessageResponse.isFromSelf()) {
            mViewModel.readMessage(sendMessageResponse.getMessageId());
        }
//        runOnUiThread(() -> new Handler().post(() -> ));
    }

    //处理获取历史消息回调
    private void handleGetFriendHistory(List<SendMessageResponse> o) {
        if (o == null) {
            mBinding.srlRefresh.setNoMoreData(true);
            return;
        }
        if (isFirstGetMessage) {
            isFirstGetMessage = false;
            mAdapter.setList(o);
        } else {
            mAdapter.addList(o);
        }
        mBinding.srlRefresh.finishRefresh();
    }

    public void insertConversation(SendMessageResponse sendMessageResponse) {
        Gson gson = new Gson();
        SendMessageResponse sendMessageResponse1 = gson.fromJson(gson.toJson(sendMessageResponse), SendMessageResponse.class);
        sendMessageResponse1.setIsread(1);
        if (mType == TYPE_FRIEND) {
            sendMessageResponse1.setHeadImg(otherHeadImage);
            sendMessageResponse1.setNikeName(mBinding.title.getText().toString());
        }
        ConversationDaoManager.getInstance().insertOrReplace(sendMessageResponse1, false);
    }

    @Override
    public void onReceiveMessageFromServer(BaseWebSocketResponse baseWebSocketResponse) {
        runOnUiThread(() -> {
            if (!baseWebSocketResponse.getAct().equals("login")) {
                dismissDialog();
            } else {
                isFirstGetMessage = true;
                if (mType == TYPE_FRIEND) {
                    mViewModel.getFriendHistoryMessage(friendId, 30);
                } else if (mType == TYPE_GROUP) {
                    mViewModel.getGroupHistoryMessage(groupId, 30);
                }
            }
            if (baseWebSocketResponse.getErrorcode() != BaseWebSocketResponse.ERRORCODE_SUCCESS) {
                showShortToast(baseWebSocketResponse.getMsg());
                return;
            }
            if (baseWebSocketResponse.getAct().equals(Extras.REQUEST_ACTION_SEND_MESSAGE) || baseWebSocketResponse.getAct().equals(Extras.REQUEST_ACTION_SEND_RED_PACK)) {
                handleSendMessage(baseWebSocketResponse.getData());
            } else if (baseWebSocketResponse.getAct().equals(Extras.REQUEST_ACTION_SEND_PIC)) {
                handleSendPic(baseWebSocketResponse.getData());
            }
            if (baseWebSocketResponse.getAct().equals(Extras.REQUEST_ACTION_TOP_AD)) {
                if (mType == TYPE_GROUP)
                    handleSocketGetGroupAd(baseWebSocketResponse.getData());
            }
            if (baseWebSocketResponse.getAct().equals(Extras.REQUEST_ACTION_DELETE_MESSAGE)) {
                List<DeleteMessageResponse> sendMessageResponses = JSONObject.parseArray(baseWebSocketResponse.getData().toString(), DeleteMessageResponse.class);
                DeleteMessageResponse deleteMessageResponse = sendMessageResponses.get(0);
                runOnUiThread(() -> mAdapter.deleteByMessageId(Integer.valueOf(deleteMessageResponse.getId())));
            }
        });
    }

    private void handleSocketGetGroupAd(Object data) {
        if (data == null) {
            return;
        }
        List<GroupAdEntity> groupAdEntities = JSONObject.parseArray(data.toString(), GroupAdEntity.class);
        GroupAdEntity groupAdEntity = groupAdEntities.get(0);
        if (groupAdEntity.getGroup_id() != groupId)
            return;
        mBinding.llNotify.setVisibility(View.VISIBLE);
        mBinding.tvNotify.setSelected(true);
        mBinding.tvNotify.setText(Html.fromHtml(groupAdEntity.getText()));
        mBinding.tvNotify.setOnClickListener(v -> AdvertisingUtil.getInstance().SkipAdWebView(ChatRoomActivity.this, groupAdEntity.getUrl()));
    }

    private boolean isRelatedCurrentAccount(SendMessageResponse sendMessageResponse) {
        if (mType == TYPE_GROUP) {
            return sendMessageResponse.getTotype() == SendMessageResponse.TOTYPE_GROUP && Integer.valueOf(sendMessageResponse.getGroupId()) == groupId;
        } else if (mType == TYPE_FRIEND) {
            if (sendMessageResponse.getTotype() == SendMessageResponse.TOTYPE_FRIEND) {
                if (sendMessageResponse.getGetId() == MMKVUtil.getUserInfo().getId() && sendMessageResponse.getSendId() == friendId)
                    return true;
                return sendMessageResponse.getSendId() == MMKVUtil.getUserInfo().getId() && sendMessageResponse.getGetId() == friendId;
            }
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SEND_RED_PACK) {
                assert data != null;
                Bundle bundle = data.getExtras();
                assert bundle != null;
                SendMessageResponse messageResponse = (SendMessageResponse) bundle.getSerializable("redbg");
                mAdapter.insertItem(messageResponse);
            } else if (requestCode == REQUEST_CODE_GROUP_DETAIL) {
                Bundle bundle = data.getExtras();
                if (bundle == null)
                    return;
                boolean isLeaveGroup = bundle.getBoolean("isLeaveGroup");
                if (isLeaveGroup) {
                    finish();
                }
                if (!TextUtils.isEmpty(bundle.getString(TAG_TITLE))) {
                    chatName = bundle.getString(TAG_TITLE);
                    mBinding.title.setText(chatName);
                }
                if (!TextUtils.isEmpty(bundle.getString(TAG_HEAD_IMAGE))) {
                    otherHeadImage = bundle.getString(TAG_HEAD_IMAGE);
                }
            } else if (requestCode == EditEmojiActivity.REQUEST_CODE) {
                likeEmojiEntities = (List<LikeEmojiEntity>) data.getSerializableExtra(Extras.MINE_EMOJIS);
                likeEmojiAdapter.setNewData(likeEmojiEntities);
                mChatFaceDialog.update();
            }
        }
    }
}