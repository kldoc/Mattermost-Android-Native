package com.kilogramm.mattermost.rxtest.left_menu;

import android.os.Bundle;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.ListPreferences;
import com.kilogramm.mattermost.model.entity.Preference.PreferenceRepository;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.channel.Channel;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.member.MembersRepository;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.model.entity.usermember.UserMemberRepository;
import com.kilogramm.mattermost.model.fromnet.LogoutData;
import com.kilogramm.mattermost.network.ServerMethod;
import com.kilogramm.mattermost.rxtest.BaseRxPresenter;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import rx.schedulers.Schedulers;


public class LeftMenuRxPresenter extends BaseRxPresenter<LeftMenuRxFragment> {
    private static final String TAG = LeftMenuRxPresenter.class.getName();

    private static final int REQUEST_SAVE = 1;
    private static final int REQUEST_UPDATE = 2;
    private static final int REQUEST_INIT = 3;

    private ListPreferences mListPreferences = new ListPreferences();
    private LogoutData mUser;

    private String mTeamId;
    private List<String> mDirectIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mUser = new LogoutData();
        mTeamId = MattermostPreference.getInstance().getTeamId();
        initRequest();
    }


    public void requestSaveData(Preferences data, String userId) {
        mListPreferences.getmSaveData().clear();
        mListPreferences.getmSaveData().add(data);
        mUser.setUserId(userId);
        start(REQUEST_SAVE);
    }

    public void requestUpdate() {
        getIds().subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(strings -> {
                    mDirectIds.clear();
                    mDirectIds.addAll(strings);
                    start(REQUEST_UPDATE);
                });
    }

    private void initRequest() {
        initLeftMenuRequest();
        initSaveRequest();
        initChannelUpdateRequest();
    }

    private void initLeftMenuRequest() {
        restartableFirst(REQUEST_INIT, () ->
                        ServerMethod.getInstance()
                                .loadLeftMenu(mDirectIds, MattermostPreference.getInstance().getTeamId())
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                , (leftMenuRxFragment, responseLeftMenuData) -> {
                    UserRepository.add(responseLeftMenuData.getStringUserMap().values());
                    UserMemberRepository.add(responseLeftMenuData.getUserMembers());
                    ChannelRepository.add(responseLeftMenuData.getChannels());
                    MembersRepository.add(responseLeftMenuData.getMembers());
                    sendUpdateMenuView();
                    sendShowLeftMenu();
                    sendSelectLastChannel();
                }, (leftMenuRxFragment, throwable) -> {
                    sendErrorLoading(parceError(throwable, null));
                    throwable.printStackTrace();
                });
    }

    private void sendShowLeftMenu() {
        createTemplateObservable(new Object())
                .subscribe(split((leftMenuRxFragment, o) -> leftMenuRxFragment.showLeftMenu()));
    }

    private void sendErrorLoading(String message) {
        createTemplateObservable(message)
                .subscribe(split((leftMenuRxFragment, s) -> leftMenuRxFragment.showErrorLoading(message)));
    }

    private void initChannelUpdateRequest() {
        restartableFirst(REQUEST_UPDATE,
                () -> ServerMethod.getInstance()
                        .loadLeftMenu(mDirectIds, MattermostPreference.getInstance().getTeamId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                , (leftMenuRxFragment, responseLeftMenuData) -> {
                    UserRepository.add(responseLeftMenuData.getStringUserMap().values());
                    UserMemberRepository.add(responseLeftMenuData.getUserMembers());
                    ChannelRepository.prepareChannelAndAdd(responseLeftMenuData.getChannels(), MattermostPreference.getInstance().getMyUserId());
                    MembersRepository.add(responseLeftMenuData.getMembers());
                    sendSetRefreshAnimation(false);
                    sendInvalidateData();
//                    sendUpdateMenuView();
                    sendSelectLastChannel();
                }, (leftMenuRxFragment, throwable) -> {
                    throwable.printStackTrace();
                    sendSetRefreshAnimation(false);
                    Log.d(TAG, throwable.getMessage());
                });

    }

    private void initSaveRequest() {
        restartableFirst(REQUEST_SAVE,
                () -> ServerMethod.getInstance()
                        .saveOrCreateDirectChannel(mListPreferences.getmSaveData(),
                                mTeamId,
                                mUser.getUserId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io()),
                (leftMenuRxFragment, channel) -> {
                    if (channel != null) {
                        ChannelRepository.prepareDirectChannelAndAdd(channel, mUser.getUserId());
                        mListPreferences.getmSaveData().clear();
                        if (channel.getId() != null)
                            sendSetFragmentChat(channel.getId(), channel.getUsername(), channel.getType());
                    }
                }, (leftMenuRxFragment, throwable) -> throwable.printStackTrace());
    }

    private void sendSetFragmentChat(String channelId, String name, String type) {
        createTemplateObservable(new Channel(channelId, name, type))
                .subscribe(split((leftMenuRxFragment, channel) -> {
                    leftMenuRxFragment.onChannelClick(channel.getId(),
                            channel.getName(),
                            channel.getType());
                    leftMenuRxFragment.setSelectItemMenu(channel.getId(),
                            channel.getType());

                }));
    }

    private void sendInvalidateData() {
        createTemplateObservable(new Object())
                .subscribe(split((leftMenuRxFragment, o) -> leftMenuRxFragment.invalidateDirect()));
    }

    private void sendSetRefreshAnimation(Boolean bool) {
        createTemplateObservable(bool).subscribe(split(
                (leftMenuRxFragment, aBoolean) -> leftMenuRxFragment.setRefreshAnimation(bool)));
    }

    private void sendUpdateMenuView() {
        createTemplateObservable(new Object()).subscribe(split(
                (leftMenuRxFragment, o) -> leftMenuRxFragment.initView()));
    }

    private void sendSelectLastChannel() {
        createTemplateObservable(new Object()).subscribe(split(
                (leftMenuRxFragment, o) -> leftMenuRxFragment.selectLastChannel()));
    }

    public void requestInit(List<Preferences> preferences) {
        this.mDirectIds.clear();
        List<String> ids = Stream.of(preferences)
                .map(Preferences::getName)
                .collect(Collectors.toList());
        this.mDirectIds.addAll(ids);
        start(REQUEST_INIT);
    }

    public Observable<List<String>> getIds() {
        return Observable.create(subscriber -> {
            try {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();

                RealmResults<Preferences> listPref = PreferenceRepository.query(realm,new PreferenceRepository.ListDirectMenu());
                List<String> ids = Stream.of(listPref)
                        .map(Preferences::getName)
                        .collect(Collectors.toList());

                realm.commitTransaction();
                realm.close();

                subscriber.onNext(ids);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }

        });
    }
}
