package com.pitstop.dependency;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.pitstop.application.GlobalApplication;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.retrofit.PitstopCarApi;
import com.pitstop.retrofit.PitstopResponse;
import com.pitstop.retrofit.Token;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.SecretUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Module (includes = {ContextModule.class, SharedPreferencesModule.class})
public class NetworkModule {
    private final String TAG = NetworkModule.class.getSimpleName();

    @Singleton
    @Provides
    public NetworkHelper networkHelper(Context context, SharedPreferences sharedPreferences){
        return new NetworkHelper(context, sharedPreferences);
    }

    private OkHttpClient getHttpClientNoAuth(Context context){
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("client-id", SecretUtils.getClientId(context))
                            .header("Content-Type", "application/json");

                    return chain.proceed(builder.build());
                })
                .build();
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

                    okhttp3.Response response = chain.proceed(original);
                    if (response.code() == 401){
                        Log.d(TAG,"Refreshing jwt token received 401 response");
                        Response<PitstopResponse<Token>> tokenResponse = pitstopAuthApi(context)
                                .refreshAccessToken(application.getRefreshToken()).execute();
                        if (tokenResponse.isSuccessful()){
                            String token = tokenResponse.body().component1().getValue();
                            Log.d(TAG,"received new token: "+token);
                            application.setTokens(token,application.getRefreshToken());

                            //Update headers with new jwt token
                            Request.Builder builderNew = original.newBuilder()
                                    .header("client-id", SecretUtils.getClientId(context))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer "+token);
                            return chain.proceed(builderNew.build()); //Ping same endpoint again after token has been refreshed
                        }else{
                            Log.d(TAG,"Token refresh request was not successful, raw response: "+tokenResponse.raw());
                            return tokenResponse.raw(); //Return unsuccessful token refresh response
                        }
                    }

                    return chain.proceed(builder.build()); //Return successful response & non 401 errors
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

    @Provides
    PitstopAuthApi pitstopAuthApi(Context context){
        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClientNoAuth(context))
                .build()
                .create(PitstopAuthApi.class);
    }
}
