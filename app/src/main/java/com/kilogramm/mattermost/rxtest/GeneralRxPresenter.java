package com.kilogramm.mattermost.rxtest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.InitObject;
import com.kilogramm.mattermost.model.entity.LicenseCfg;
import com.kilogramm.mattermost.model.entity.ListSaveData;
import com.kilogramm.mattermost.model.entity.NotifyProps;
import com.kilogramm.mattermost.model.entity.RealmString;
import com.kilogramm.mattermost.model.entity.SaveData;
import com.kilogramm.mattermost.model.entity.Team;
import com.kilogramm.mattermost.model.entity.ThemeProps;
import com.kilogramm.mattermost.model.entity.channel.Channel;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.post.Post;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.network.ApiMethod;

import icepick.State;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny on 05.10.2016.
 */
public class GeneralRxPresenter extends BaseRxPresenter<GeneralRxActivity> {
    public static final String TAG = "GeneralRxPresenter";

    private static final int REQUEST_DIRECT_PROFILE = 1;
    private static final int REQUEST_LOAD_CHANNELS = 2;
    private static final int REQUEST_USER_TEAM = 3;
    private static final int REQUEST_LOGOUT = 4;
    private static final int REQUEST_SAVE = 5;

    Realm realm;

    ApiMethod service;

    /*@State
    String teamId;*/
    @State
    ListSaveData mSaveData = new ListSaveData();

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        realm = Realm.getDefaultInstance();
        MattermostApp application = MattermostApp.getSingleton();
        service = application.getMattermostRetrofitService();
        initRequest();
        requestDirectProfile();
    }

    @Override
    public void takeView(GeneralRxActivity generalActivity) {
        super.takeView(generalActivity);
        setSelectedLast(MattermostPreference.getInstance().getLastChannelId());
    }

    //TODO review evgenysuetin
    public void setSelectedLast(String id) {
        Log.d(TAG, "setSelectedLast");
        Channel channel;
        if (id != null) {
            try {
                channel = new ChannelRepository.ChannelByIdSpecification(id).toRealmResults(realm).first();
                if (channel != null)
                    switch (channel.getType()) {
                        case "O":
                            setSelectedMenu(channel.getId(), channel.getName(), channel.getType());
                            break;
                        case "D":
                            setSelectedMenu(channel.getId(), channel.getUsername(), channel.getType());
                            break;
                        case "P":
                            setSelectedMenu(channel.getId(), channel.getName(), channel.getType());
                            break;
                    }
                sendSetSelectChannel(id, channel.getType());
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else {
            RealmResults<Channel> channels = new ChannelRepository.ChannelByTypeSpecification("D").toRealmResults(realm);
            if (channels.size() != 0) {
                setSelectedMenu(channels.first().getId(), channels.first().getUsername(), channels.first().getType());
            } else {
                channels.addChangeListener(element -> {
                    if (element.size() != 0)
                        setSelectedMenu(element.first().getId(), element.first().getUsername(), channels.first().getType());
                });
            }
        }
    }

    private void initRequest() {

        restartableFirst(REQUEST_DIRECT_PROFILE, () -> {
            return service.getDirectProfile()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io());
        }, (generalRxActivity, stringUserMap) -> {
            RealmList<User> users = new RealmList<>();
            users.addAll(stringUserMap.values());
            users.add(new User("materMostAll", "all", "Notifies everyone in the channel, use in Town Square to notify the whole team"));
            users.add(new User("materMostChannel", "channel", "Notifies everyone in the channel"));
            UserRepository.add(users);
            requestLoadChannels();
        }, (generalRxActivity1, throwable) -> {
            sendShowError(throwable.toString());
        });

        restartableFirst(REQUEST_LOAD_CHANNELS, () -> {
            return service.getChannelsTeam(MattermostPreference.getInstance().getTeamId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io());
        }, (generalRxActivity, channelsWithMembers) -> {
            ChannelRepository.prepareChannelAndAdd(channelsWithMembers.getChannels(),
                    MattermostPreference.getInstance().getMyUserId());
            requestUserTeam();
        }, (generalRxActivity1, throwable) -> {
            sendShowError(throwable.toString());
        });

        restartableFirst(REQUEST_USER_TEAM, () -> {
            return service.getTeamUsers(MattermostPreference.getInstance().getTeamId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io());
        }, (generalRxActivity, stringUserMap) -> {
            UserRepository.add(stringUserMap.values());
            if (MattermostPreference.getInstance().getLastChannelId() == null) {
                Log.d(TAG, "lastChannel == null");
                Channel channel = ChannelRepository.query(new ChannelRepository.ChannelByTypeSpecification("0")).first();
                sendSetSelectChannel(channel.getId(), channel.getType());
                Log.d(TAG, "sendSetSelectChannel");
                if (channel != null) {
                    setSelectedMenu(channel.getId(), channel.getName(), channel.getType());
                    Log.d(TAG, "setSelectedMenu");
                }
            }
        }, (generalRxActivity1, throwable) -> {
            throwable.printStackTrace();
        });

        restartableFirst(REQUEST_LOGOUT, () -> {
            return service.logout(new Object())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io());
        }, (generalRxActivity, logoutData) -> {
            Log.d(TAG, "Complete logout");
            clearDataBaseAfterLogout();
            clearPreference();
            sendShowMainRxActivity();
        }, (generalRxActivity1, throwable) -> {
            throwable.printStackTrace();
            Log.d(TAG, "Error logout");
        });

        initSaveRequest();
    }

    private void initSaveRequest() {
        restartableFirst(REQUEST_SAVE, () -> {
            return Observable.zip(service.save(mSaveData.getmSaveData())
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread()),
                    service.getChannelsTeam(MattermostPreference.getInstance().getTeamId())
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread()),
                    (aBoolean, channelsWithMembers) -> {
                        ChannelRepository.prepareChannelAndAdd(channelsWithMembers.getChannels(),
                                MattermostPreference.getInstance().getMyUserId());
                        String myId = MattermostPreference.getInstance().getMyUserId();
                        RealmResults<Channel> channels = realm.where(Channel.class)
                                .equalTo("name", myId + "__" + mSaveData.getmSaveData().get(0).getName())
                                .or()
                                .equalTo("name", mSaveData.getmSaveData().get(0).getName() + "__" + myId)
                                .findAll();
                        if (channels.size() != 0) {
                            return channels.get(0);
                        } else {
                            return null;
                        }
                    });
        }, (generalRxActivity, channel) -> {
            setSelectedMenu(channel.getId(), channel.getUsername(), channel.getType());
            Log.d(TAG, "Must open direct dialog");
            mSaveData.getmSaveData().clear();
        }, (generalRxActivity1, throwable) -> {
            throwable.printStackTrace();
        });
    }

    public void requestSaveData(SaveData data) {
        mSaveData.getmSaveData().clear();
        mSaveData.getmSaveData().add(data);
        start(REQUEST_SAVE);
    }


    public void requestLoadChannels() {
        start(REQUEST_LOAD_CHANNELS);
    }

    public void requestDirectProfile() {
        start(REQUEST_DIRECT_PROFILE);
    }

    public void requestUserTeam() {
        start(REQUEST_USER_TEAM);
    }

    public void requestLogout() {
        start(REQUEST_LOGOUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(realm!=null && !realm.isClosed()){
            realm.close();
        }
    }

    @Override
    protected void onTakeView(GeneralRxActivity generalRxActivity) {
        super.onTakeView(generalRxActivity);
        //loadChannels(realm.where(Team.class).findFirst().getId());
    }

    public void setSelectedMenu(String channelId, String name, String type) {
        sendSetFragmentChat(channelId, name, type);
        MattermostPreference.getInstance().setLastChannelId(channelId);
    }


    private void clearPreference() {
        MattermostPreference.getInstance().setAuthToken(null);
        MattermostPreference.getInstance().setLastChannelId(null);
    }

    private void clearDataBaseAfterLogout() {
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(realm1 -> {
            realm.delete(Post.class);
            realm.delete(Channel.class);
            realm.delete(InitObject.class);
            realm.delete(LicenseCfg.class);
            realm.delete(NotifyProps.class);
            realm.delete(RealmString.class);
            realm.delete(Team.class);
            realm.delete(InitObject.class);
            realm.delete(ThemeProps.class);
            realm.delete(User.class);
        });
        realm.close();
    }


    //to view methods

    private void sendShowError(String error) {
        createTemplateObservable(error)
                .subscribe(split(GeneralRxActivity::showErrorText));
    }

    private void sendShowMainRxActivity() {
        createTemplateObservable(new Object())
                .subscribe(split((mainRxAcivity, o) -> mainRxAcivity.showMainRxActivity()));

    }

    private void sendSetFragmentChat(String channelId, String name, String type) {
        createTemplateObservable(new OpenChatObject(channelId,name, type))
                .subscribe(split((generalRxActivity1, openChatObject)
                        -> generalRxActivity1.setFragmentChat(openChatObject.getChannelId(), name, type)));
    }

    private void sendSetSelectChannel(String channelId, String type) {
        createTemplateObservable(new OpenChatObject(channelId, type))
                .subscribe(split((generalRxActivity1, openChatObject)
                        -> generalRxActivity1.setSelectItemMenu(openChatObject.getChannelId(), type)));

    }


    public static class OpenChatObject {
        private String channelId;
        private String name;
        private String type;

        public OpenChatObject(String channelId, String name, String type) {
            this.channelId = channelId;
            this.name = name;
            this.type = type;
        }

        public OpenChatObject(String channelId, String type) {
            this.channelId = channelId;
            this.type = type;
        }

        public String getChannelId() {
            return channelId;
        }

        public String getName() {
            return name;
        }

        public String getChannel() {
            return type;
        }
    }
}
