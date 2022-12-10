package caixin.android.com.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.caixin.toutiao.R;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import caixin.android.com.entity.SendMessageResponse;
import caixin.android.com.utils.ImgLoader;

import java.util.ArrayList;
import java.util.List;


public class ConversationAdapter extends BaseQuickAdapter<SendMessageResponse, BaseViewHolder> implements Filterable {

    private Context context;

    public ConversationAdapter(Context context) {
        super(R.layout.item_conversation);
        this.context = context;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, SendMessageResponse item) {
        helper.itemView.scrollTo(0, 0);
        if (item.getTotype() == SendMessageResponse.TOTYPE_GROUP) {
            initGroup(helper, item);
        } else {
            initFriend(helper, item);
        }
        helper.addOnClickListener(R.id.delete);
        if (item.getSort() > 0) {
            ((RelativeLayout) helper.getView(R.id.list_itease_layout)).setBackgroundColor(mContext.getResources().getColor(R.color.lightgray));
            helper.getView(R.id.iv_is_top).setVisibility(View.VISIBLE);
        } else {
            ((RelativeLayout) helper.getView(R.id.list_itease_layout)).setBackgroundColor(mContext.getResources().getColor(R.color.white));
            helper.getView(R.id.iv_is_top).setVisibility(View.GONE);
        }
    }

    private void initGroup(BaseViewHolder helper, SendMessageResponse item) {
        if (item.getUnread() == 0) {
            helper.getView(R.id.unread_msg_number).setVisibility(View.INVISIBLE);
        } else {
            helper.getView(R.id.unread_msg_number).setVisibility(View.VISIBLE);
            if (item.getUnread() > 99) {
                helper.setText(R.id.unread_msg_number, "99+");
            } else {
                helper.setText(R.id.unread_msg_number, String.valueOf(item.getUnread()));
            }
        }
        switch (item.getPid()) {
            case SendMessageResponse.PID_TYPE_MESSAGE:
                helper.setText(R.id.message, item.getNikeName() + "：" + item.getContents());
                break;
            case SendMessageResponse.PID_TYPE_PICTURE:
                helper.setText(R.id.message, item.getNikeName() + "：" + "[图片]");
                break;
            case SendMessageResponse.PID_TYPE_REDPACK:
                helper.setText(R.id.message, item.getNikeName() + "：" + "[红包]");
                break;
        }
        helper.setText(R.id.time, item.getUpdatetime());
        helper.setText(R.id.name, item.getGroupName());
        ImgLoader.GlideLoadRoundedCorners(item.getGroupImage(), helper.getView(R.id.avatar), R.mipmap.default_avatar);
        helper.getView(R.id.mentioned).setVisibility(View.GONE);
    }

    private void initFriend(BaseViewHolder helper, SendMessageResponse item) {
        setUserNick(item, helper.getView(R.id.name));
        setUserAvatar(item, helper.getView(R.id.avatar));
        helper.getView(R.id.mentioned).setVisibility(View.GONE);
        if (item.getUnread() == 0) {
            helper.getView(R.id.unread_msg_number).setVisibility(View.INVISIBLE);
        } else {
            helper.getView(R.id.unread_msg_number).setVisibility(View.VISIBLE);
            if (item.getUnread() > 99) {
                helper.setText(R.id.unread_msg_number, "99+");
            } else {
                helper.setText(R.id.unread_msg_number, String.valueOf(item.getUnread()));
            }
        }
        switch (item.getPid()) {
            case SendMessageResponse.PID_TYPE_MESSAGE:
                helper.setText(R.id.message, item.getContents());
                break;
            case SendMessageResponse.PID_TYPE_PICTURE:
                helper.setText(R.id.message, "[图片]");
                break;
            case SendMessageResponse.PID_TYPE_REDPACK:
                helper.setText(R.id.message, "[恭喜发财，大吉大利]");
                break;
        }
        helper.setText(R.id.time, item.getUpdatetime());
    }

    private void setUserNick(SendMessageResponse friendEntity, TextView textView) {
        if (textView != null) {
            if (friendEntity == null)
                return;
            if (!TextUtils.isEmpty(friendEntity.getNikeName())) {
                textView.setText(friendEntity.getNikeName());
            }
            if (!TextUtils.isEmpty(friendEntity.getTag())) {
                textView.setText(friendEntity.getTag());
            }
        }
    }

    private void setUserAvatar(SendMessageResponse friendEntity, ImageView imageView) {
        if (friendEntity != null) {
            try {
                ImgLoader.GlideLoadRoundedCorners(friendEntity.getHeadImg(), imageView, R.mipmap.default_avatar);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Filter getFilter() {
        if (myFilter == null) {
            myFilter = new Filter() {
                //执行过滤操作
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();
                    FilterResults filterResults = new FilterResults();
                    Log.e(TAG, "performFiltering: " + charString);
                    if (TextUtils.isEmpty(charString)) {
                        //没有过滤的内容，则使用源数据
                        filterResults.values = originDatas;
                        return filterResults;
                    } else {
                        List<SendMessageResponse> filteredList = new ArrayList<>();
                        for (SendMessageResponse item : originDatas) {
                            //这里根据需求，添加匹配规则
                            if (TextUtils.isEmpty(item.getNikeName()))
                                continue;

                            if (item.getNikeName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(item);
                                continue;
                            } else {
                                if (TextUtils.isEmpty(item.getTag()))
                                    continue;
                                else if (item.getTag().toLowerCase().contains(charString.toLowerCase())) {
                                    filteredList.add(item);
                                    continue;
                                }
                            }
                            if (!TextUtils.isEmpty(item.getGroupName())) {
                                if (item.getGroupName().toLowerCase().contains(charString.toLowerCase())) {
                                    filteredList.add(item);
                                }
                            }
                        }
                        filterResults.values = filteredList;
                    }
                    return filterResults;
                }

                //把过滤后的值返回出来
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
//                mFilterList = (ArrayList<String>) filterResults.values;
                    isFilter = true;
                    setNewData((List<SendMessageResponse>) filterResults.values);
                    isFilter = false;
//                    notifyDataSetChanged();
                }
            };
        }
        return myFilter;
    }


    private Filter myFilter;
    private boolean isFilter = false;

    private List<SendMessageResponse> originDatas;

    @Override
    public void setNewData(@Nullable List<SendMessageResponse> data) {
        super.setNewData(data);
        if (!isFilter) {
            originDatas = data;
        }
    }
}
