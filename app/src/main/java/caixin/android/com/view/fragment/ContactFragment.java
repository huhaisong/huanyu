package caixin.android.com.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.caixin.toutiao.R;
import com.caixin.toutiao.databinding.FragmentContactBinding;

import caixin.android.com.base.AppViewModelFactory;
import caixin.android.com.view.activity.BlackFriendListActivity;
import caixin.android.com.view.activity.NewFriendApplyActivity;
import caixin.android.com.view.activity.PhoneContactListActivity;
import caixin.android.com.view.activity.ScanActivity;
import caixin.android.com.viewmodel.ContactViewModel;
import caixin.android.com.base.BaseFragment;
import caixin.android.com.widget.AddPopWindow;


import java.util.ArrayList;
import java.util.List;


public class ContactFragment extends BaseFragment<FragmentContactBinding, ContactViewModel> {
    private static final String CONTACT_PERSON_FRAGMENT = "ContactFragment_contactPersonFragment";
    private static final String CONTACT_GROUP_FRAGMENT = "ContactFragment_contactGroupFragment";
    private static final String GROUP_FRAGMENT = "ContactFragment_groupFragment";
    private List<BaseFragment> mFragments = new ArrayList<>();
    private FragmentManager mFragmentManager;
    private ContactPersonFragment contactPersonFragment;
    private ContactGroupFragment contactGroupFragment;
    private GroupFragment groupFragment;

    @Override
    public int initContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mFragmentManager = getChildFragmentManager();
        if (savedInstanceState != null) {
            /*获取保存的fragment  没有的话返回null*/
            contactPersonFragment = (ContactPersonFragment) getChildFragmentManager().getFragment(savedInstanceState, CONTACT_PERSON_FRAGMENT);
            contactGroupFragment = (ContactGroupFragment) getChildFragmentManager().getFragment(savedInstanceState, CONTACT_GROUP_FRAGMENT);
            groupFragment = (GroupFragment) getChildFragmentManager().getFragment(savedInstanceState, GROUP_FRAGMENT);
            addToList(contactPersonFragment);
            addToList(contactGroupFragment);
            addToList(groupFragment);
        } else {
            initFragment();
        }
        return R.layout.fragment_contact;
    }

    @Override
    public ContactViewModel initViewModel() {
        AppViewModelFactory factory = AppViewModelFactory.getInstance(getActivity().getApplication());
        return ViewModelProviders.of(this, factory).get(ContactViewModel.class);
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        mBinding.title.setText("通讯录");
        mBinding.rlNewFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(NewFriendApplyActivity.class);
            }
        });

        mBinding.rlGroupChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    @Override
    public void initViewObservable() {

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void refresh() {
        for (int i = 0; i < mFragments.size(); i++) {
            mFragments.get(i).refresh();
        }
    }

    public void setOvalVisibility(boolean ovalVisibility) {

    }

    private void addToList(BaseFragment fragment) {
        if (fragment != null) {
            mFragments.add(fragment);
        }
    }

    private void initFragment() {
        showFragment(0);
    }

    private void showFragment(Fragment fragment) {
        for (Fragment frag : mFragments) {
            if (frag != fragment) {
                /*先隐藏其他fragment*/
                mFragmentManager.beginTransaction().hide(frag).commit();
            }
        }
        mFragmentManager.beginTransaction().show(fragment).commit();
    }

    private void addFragment(BaseFragment fragment) {
        if (!fragment.isAdded()) {
            mFragmentManager.beginTransaction().add(R.id.container, fragment).commit();
            mFragments.add(fragment);
        }
    }

    private void showFragment(int index) {
        switch (index) {
            case 0:
                if (contactPersonFragment == null) {
                    contactPersonFragment = new ContactPersonFragment();
                }
                addFragment(contactPersonFragment);
                showFragment(contactPersonFragment);
                break;
            case 1:
                if (contactGroupFragment == null) {
                    contactGroupFragment = new ContactGroupFragment();
                }
                addFragment(contactGroupFragment);
                showFragment(contactGroupFragment);
                break;
            case 2:
                if (groupFragment == null) {
                    groupFragment = new GroupFragment();
                }
                addFragment(groupFragment);
                showFragment(groupFragment);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (contactPersonFragment != null) {
            mFragmentManager.putFragment(outState, CONTACT_PERSON_FRAGMENT, contactPersonFragment);
        }
        if (contactGroupFragment != null) {
            mFragmentManager.putFragment(outState, CONTACT_GROUP_FRAGMENT, contactGroupFragment);
        }
        if (groupFragment != null) {
            mFragmentManager.putFragment(outState, GROUP_FRAGMENT, groupFragment);
        }
        super.onSaveInstanceState(outState);
    }
}
