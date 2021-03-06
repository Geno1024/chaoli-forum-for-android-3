package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.ConversationLayoutSelector;
import com.daquexian.chaoli.forum.binding.LayoutSelector;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.ConversationListResult;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.daquexian.chaoli.forum.utils.AccountUtils;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;

/**
 * ViewModel of MainActivity
 * Created by jianhao on 16-9-19.
 */
public class MainActivityVM extends BaseViewModel {
    private final String TAG = "MainActivityVM";

    public ObservableArrayList<Conversation> conversationList = new ObservableArrayList<>();

    public ObservableBoolean canRefresh = new ObservableBoolean(true);
    public ObservableBoolean isRefreshing = new ObservableBoolean();

    public ObservableField<Integer> myUserId = new ObservableField<>(-1);
    public ObservableField<String> myUsername = new ObservableField<>();
    public ObservableField<String> myAvatarSuffix = new ObservableField<>();
    public ObservableField<String> mySignature = new ObservableField<>();

    public ObservableBoolean hasLoggedIn = new ObservableBoolean(false);

    public ObservableInt listPosition = new ObservableInt();

    public ObservableInt goToHomepage = new ObservableInt();
    public ObservableInt goToLogin = new ObservableInt();
    public ObservableInt goToConversation = new ObservableInt();
    public Conversation clickedConversation;
    public ObservableInt notificationsNum = new ObservableInt(0);
    public ObservableBoolean showLoginProcessDialog = new ObservableBoolean(false);
    public ObservableInt selectedItem = new ObservableInt(-1);
    public ObservableInt goToPost = new ObservableInt();
    public ObservableBoolean failed = new ObservableBoolean();
    public ObservableBoolean showToast = new ObservableBoolean();
    public String toastContent;

    public LayoutSelector<Conversation> layoutSelector = new ConversationLayoutSelector();

    private String channel;
    private int page;

    private Timer timer;
    private TimerTask task;

    private Boolean canAutoLoad = false;

    public MainActivityVM() {
        //conversationList.add(new Conversation());
    }

    private void getList(final int page) {
        getList(page, false);
    }
    private void getList(final int page, final Boolean refresh)
    {
        showCircle();
        Log.d(TAG, "getList() called with: page = [" + page + "]");
        MyRetrofit.getService()
                .listConversations(channel, "#第 " + page + " 页")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ConversationListResult>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        removeCircle();
                        if (refresh) {
                            failed.set(true);
                            failed.notifyChange();
                        }
                    }

                    @Override
                    public void onNext(ConversationListResult conversationListResult) {
                        //conversationList.remove(conversationList.size() - 1);
                        int oldLen = conversationList.size();
                        Log.d(TAG, "onNext: " + oldLen);
                        List<Conversation> newConversationList = conversationListResult.getResults();
                        if (page == 1) {
                            conversationList.clear();
                            conversationList.addAll(newConversationList);
                        } else {
                            MyUtils.expandUnique(conversationList, newConversationList);
                        }
                        //conversationList.add(new Conversation());
                        canAutoLoad = true;
                        removeCircle();
                        if (refresh) {
                            listPosition.set(0);
                            listPosition.notifyChange();
                        }
                        MainActivityVM.this.page = page;
                    }
                });
    }

    public void refresh(){
        page = 1;
        getList(page, true);
    }
    public void loadMore() {
        if (isRefreshing.get()) return;
        getList(page + 1);
    }

    /**
     * 去掉刷新时的圆圈
     */
    private void removeCircle() {
        isRefreshing.set(false);
        isRefreshing.notifyChange();
    }

    /**
     * 显示刷新时的圆圈
     */
    private void showCircle() {
        isRefreshing.set(true);
        isRefreshing.notifyChange();
    }

    public void onClickAvatar(View view) {
        if (hasLoggedIn.get()) {
            goToHomepage.notifyChange();
        } else {
            goToLogin.notifyChange();
        }
    }

    public void onClickPostFab(View view) {
        goToPost.notifyChange();
    }

    public void onClickConversation(Conversation conversation) {
        Log.d(TAG, "onClickConversation: ");
        clickedConversation = conversation;
        goToConversation.notifyChange();
    }

    public void tryToLoadFromBottom() {
        if (canAutoLoad) {
            canAutoLoad = false;
            loadMore();
        }
    }

    private Handler notificationHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(!Me.isEmpty()) {
                AccountUtils.checkNotification(new AccountUtils.MessageObserver() {
                    @Override
                    public void onGetUpdateSuccess(Boolean hasUpdate) {

                    }

                    @Override
                    public void onGetUpdateFailure(int statusCode) {

                    }

                    @Override
                    public void onCheckNotificationSuccess(NotificationList notificationList) {
                        notificationsNum.set(notificationList.count);
                    }

                    @Override
                    public void onCheckNotificationFailure(int statusCode) {

                    }
                });
            }
        }
    };
    public void login() {
        showLoginProcessDialog.set(true);
        LoginUtils.begin_login(new LoginUtils.LoginObserver() {
            @Override
            public void onLoginSuccess(int userId, String token) {
                failed.set(false);
                showLoginProcessDialog.set(false);
                hasLoggedIn.set(true);
                /*navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.menu_navigation);*/
                String username = ChaoliApplication.getAppContext().getSharedPreferences("username_and_password", MODE_PRIVATE).getString("username", "");
                //((TextView) ((Activity) mContext).findViewById(R.id.loginHWndUsername)).setText(username);
                Me.setInstanceFromSharedPreference(ChaoliApplication.getAppContext(), username);
                if (!Me.isEmpty()) {
                    Log.d(TAG, "onLoginSuccess: " + Me.getMyUserId() + ", " + Me.getMyUsername() + Me.getMyAvatarSuffix() + Me.getMySignature());
                    myUserId.set(Me.getMyUserId());
                    myUsername.set(Me.getMyUsername());
                    myAvatarSuffix.set(Me.getMyAvatarSuffix());
                    mySignature.set(Me.getMySignature());
                } else {
                    myUsername.set(getString(R.string.loading));
                    mySignature.set(getString(R.string.loading));
                }
                AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
                    @Override
                    public void onGetProfileSuccess() {
                        myUserId.set(Me.getMyUserId());
                        myUsername.set(Me.getMyUsername());
                        myAvatarSuffix.set(Me.getMyAvatarSuffix());
                        mySignature.set(Me.getMySignature());
                    }

                    @Override
                    public void onGetProfileFailure() {

                    }
                });

                timer = new Timer();
                task = new TimerTask() {
                    @Override
                    public void run() {
                        notificationHandler.sendEmptyMessage(0);
                    }
                };
                // TODO: 17-1-2 imporve code here
                //noinspection EmptyCatchBlock
                try {
                    timer.schedule(task, Constants.getNotificationInterval * 1000, Constants.getNotificationInterval * 1000);
                } catch (Exception e) {

                }
                //view.selectItem(0);
                selectedItem.set(0);
            }

            @Override
            public void onLoginFailure(int statusCode) {
                showLoginProcessDialog.set(false);
                if (statusCode == LoginUtils.EMPTY_UN_OR_PW) {
                    selectedItem.set(0);
                    selectedItem.notifyChange();
                }
                else {
                    failed.set(true);

                }
                Log.d(TAG, "onLoginFailure: " + statusCode);
            }
        });
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelByPosition(int position) {
        String[] channel =
                new String[]
                        {
                                "",
                                Channel.caff.name(),
                                Channel.maths.name(),
                                Channel.physics.name(),
                                Channel.chem.name(),
                                Channel.biology.name(),
                                Channel.tech.name(),
                                Channel.court.name(),
                                Channel.announ.name(),
                                Channel.others.name(),
                                Channel.socsci.name(),
                                Channel.lang.name(),
                        };
        return channel[position];
    }

    public void resume() {
        if(!Me.isEmpty()) {
            //timer.cancel();
            mySignature.set(Me.getMySignature());
            myAvatarSuffix.set(Me.getAvatarSuffix());
            if (task != null) task.cancel();
            task = new TimerTask() {
                @Override
                public void run() {
                    notificationHandler.sendEmptyMessage(0);
                }
            };
            timer = new Timer();
            timer.schedule(task, 0, Constants.getNotificationInterval * 1000);
        }
    }

    public void destory() {
        if (timer != null) timer.cancel();
        MyOkHttp.getClient().dispatcher().cancelAll();
    }
}
