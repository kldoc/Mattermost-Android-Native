package com.kilogramm.mattermost.rxtest;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.network.ApiMethod;
import com.kilogramm.mattermost.network.ServerMethod;
import com.kilogramm.mattermost.tools.FileUtil;
import com.kilogramm.mattermost.view.BaseActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny on 31.10.2016.
 */

public class EditProfileRxPresenter extends BaseRxPresenter<EditProfileRxActivity> {


    private static final String TAG = "EditProfileRxPresenter";

    private static final int REQUEST_SAVE = 1;
    private static final int REQUEST_NEW_IMAGE = 2;

    private ApiMethod service;

    private User editedUser;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        service = MattermostApp.getSingleton().getMattermostRetrofitService();
        initRequest();
    }

    public void requestNewImage(Uri uri){
        this.imageUri = uri;
        start(REQUEST_NEW_IMAGE);
    }

    public void requestSave(User editedUser) {
        this.editedUser = editedUser;
        start(REQUEST_SAVE);
    }

    private void initRequest() {
        initSave();
        initNewImage();
    }

    private void initSave() {
        restartableFirst(REQUEST_SAVE,
                () -> ServerMethod.getInstance()
                        .updateUser(editedUser)
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                ,(editProfileRxActivity, user) -> {
                    if (user != null){
                        Log.d(TAG, "user.firstName = " + user.getFirstName() + " " +
                                "user.lastname = " + user.getLastName() + " " +
                                "user.userName = " + user.getUsername() + " " +
                                "user.nickName = " + user.getNickname() + " ");
                        UserRepository.updateUserAfterSaveSettings(user);
                        sendGood(editProfileRxActivity.getString(R.string.user_updated));
                    }
                },(editProfileRxActivity, throwable) -> sendError(parceError(throwable, null)));
    }

    private void initNewImage(){
        restartableFirst(REQUEST_NEW_IMAGE,
                () -> {
                    String filePath = FileUtil.getInstance().getPath(imageUri);
                    String mimeType = FileUtil.getInstance().getMimeType(filePath);

                    File file = null;
                    if(filePath == null){
                        file = new File(FileUtil.getInstance().getFileByUri(imageUri));
                    } else{
                        file = new File(filePath);
                    }

                    if(mimeType == null) {
                        mimeType = "*/*";
                    }
                    RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData("image", file.getName(), fileBody);
                    return service.newimage(filePart)
                            .observeOn(Schedulers.io())
                            .subscribeOn(Schedulers.io());
                },(editProfileRxActivity, result) -> {
                    sendGood(editProfileRxActivity.getString(R.string.avatar_updated));
                    ImageLoader.getInstance().clearDiskCache();
                    ImageLoader.getInstance().clearMemoryCache();
                    updateAvatarTime();
                    imageUri = null;
                    sendUriNull();
                },(editProfileRxActivity, throwable) -> sendError(parceError(throwable)));
    }

    private void updateAvatarTime(){
        User user = UserRepository.query(new UserRepository.
                UserByIdSpecification(MattermostPreference.getInstance().getMyUserId())).first();
        Calendar ca = Calendar.getInstance();
        UserRepository.updateUserAvataTime(user.getId(), ca.getTimeInMillis());
    }

    private void sendUriNull() {
        createTemplateObservable(new Object())
                .subscribe(split((editProfileRxActivity, o) -> editProfileRxActivity.selectedImageUri=null));
    }

    private void sendError(String error){
        createTemplateObservable(error)
                .subscribe(split(BaseActivity::showErrorText));
        sendOnUpdateComplete();
    }

    private void sendGood(String good){
        createTemplateObservable(good)
                .subscribe(split(BaseActivity::showGoodText));
        sendOnUpdateComplete();
    }

    private void sendOnUpdateComplete(){
        createTemplateObservable(null)
                .subscribe(split((editProfileRxActivity, s) -> editProfileRxActivity.onUpdateComplete()));
    }
}
