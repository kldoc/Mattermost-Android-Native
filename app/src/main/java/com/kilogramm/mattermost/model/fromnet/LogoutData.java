package com.kilogramm.mattermost.model.fromnet;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Evgeny on 20.09.2016.
 */
// TODO класс по факту не используется (Kepar)
public class LogoutData {

    @SerializedName("user_id")
    @Expose
    private String userId;
}
