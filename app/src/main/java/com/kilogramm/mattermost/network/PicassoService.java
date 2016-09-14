package com.kilogramm.mattermost.network;

import android.content.Context;
import android.util.Log;

import com.kilogramm.mattermost.MattermostPreference;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Evgeny on 23.08.2016.
 */
public class PicassoService {

    public static void create(Context context) throws IllegalArgumentException {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        HttpLoggingInterceptor headerInterceprion = new HttpLoggingInterceptor();
        headerInterceprion.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token;
                    if((token = MattermostPreference.getInstance().getAuthToken())!=null){
                        Request request = original.newBuilder()
                                .addHeader("Authorization","Bearer " + token)
                                .build();
                        return chain.proceed(request);
                    } else {
                        return chain.proceed(original);
                    }
                })
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        MattermostPreference.getInstance().saveCookies(cookies);
                    }
                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = MattermostPreference.getInstance().getCookies();
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();

        Picasso.setSingletonInstance(new Picasso.Builder(context)
                .listener((picasso, uri, exception) -> {
                    exception.printStackTrace();
                    Log.d("PICASSO", exception.getMessage());
                })
                .downloader(new OkHttp3Downloader(client))
                .memoryCache(new LruCache(context))
                .build());

    }

}