package com.pitstop.dependency;

import android.content.Context;
import android.content.SharedPreferences;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.pitstop.application.GlobalApplication;
import com.pitstop.retrofit.PitstopCarApi;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.SecretUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module (includes = {ContextModule.class, SharedPreferencesModule.class})
public class NetworkModule {

    @Singleton
    @Provides
    public NetworkHelper networkHelper(Context context, SharedPreferences sharedPreferences){
        return new NetworkHelper(context, sharedPreferences);
    }

    private OkHttpClient getHttpClient(Context context){
        GlobalApplication application = (GlobalApplication)context.getApplicationContext();
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("client-id", SecretUtils.getClientId(context))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer "+application.getAccessToken());

                    return chain.proceed(builder.build());
                })
                .build();
    }

    @Provides
    public PitstopCarApi pitstopCarApi(Context context){
        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(PitstopCarApi.class);
    }
}
