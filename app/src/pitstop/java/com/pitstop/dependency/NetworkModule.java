package com.pitstop.dependency;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonObject;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.pitstop.application.GlobalApplication;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.retrofit.PitstopCarApi;
import com.pitstop.retrofit.Token;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.SecretUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
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
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("client-id", SecretUtils.getClientId(context))
                            .header("Content-Type", "application/json");

                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .build();
    }

    private OkHttpClient getHttpClient(Context context){
        GlobalApplication application = (GlobalApplication)context.getApplicationContext();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Log.d(TAG,"Adding interceptor.");
                    Request original = chain.request();

                    Request.Builder builder = original.newBuilder()
                            .header("client-id", SecretUtils.getClientId(context))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer "+application.getAccessToken());

                    okhttp3.Response response = null;
                    Log.d(TAG,"creating real response");
                    response = chain.proceed(original);

                    if (response.code() == 401){
                        Log.d(TAG,"Refreshing jwt token received 401 response");
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("refreshToken",application.getRefreshToken());
                        Response<Token> tokenResponse = pitstopAuthApi(context)
                                .refreshAccessToken(jsonObject).execute();
                        Log.d(TAG,"tokenResponse: "+tokenResponse);
                        if (tokenResponse.isSuccessful()){
                            String token = tokenResponse.body().getAccessToken();
                            Log.d(TAG,"received new token: "+token);
                            application.setTokens(token,application.getRefreshToken());

                            //Update headers with new jwt token
                            Request.Builder builderNew = original.newBuilder()
                                    .header("client-id", SecretUtils.getClientId(context))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer "+token);
                            return chain.proceed(builderNew.build()); //Ping same endpoint again after token has been refreshed
                        }else{
                            Log.d(TAG,"Token refresh request was not successful, raw response: "+tokenResponse.body());
                            return tokenResponse.raw(); //Return unsuccessful token refresh response //Todo: should log out here
                        }
                    }

                    return chain.proceed(builder.build()); //Return successful response & non 401 errors
                }).addInterceptor(logging)
                .build();
    }

    @Provides
    @Singleton
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
    @Singleton
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
