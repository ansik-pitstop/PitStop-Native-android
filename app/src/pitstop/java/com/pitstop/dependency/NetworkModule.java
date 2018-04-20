package com.pitstop.dependency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.pitstop.application.GlobalApplication;
import com.pitstop.network.HttpRequest;
import com.pitstop.retrofit.GoogleSnapToRoadApi;
import com.pitstop.retrofit.PitstopAppointmentApi;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.retrofit.PitstopCarApi;
import com.pitstop.retrofit.PitstopSensorDataApi;
import com.pitstop.retrofit.PitstopSmoochApi;
import com.pitstop.retrofit.PitstopTripApi;
import com.pitstop.retrofit.Token;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.SecretUtils;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Observable;
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
    public NetworkHelper networkHelper(Context context, PitstopAuthApi pitstopAuthApi, SharedPreferences sharedPreferences){
        return new NetworkHelper(context, pitstopAuthApi, sharedPreferences);
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
                    Log.d(TAG,"Adding interceptor, access token: "+application.getAccessToken());
                    Request original = chain.request();

                    String prevAccessToken = application.getAccessToken();
                    Request.Builder builder = original.newBuilder()
                            .header("client-id", SecretUtils.getClientId(context))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer "+prevAccessToken);

                    okhttp3.Response response = null;
                    Log.d(TAG,"creating real response");
                    response = chain.proceed(builder.build());

                    if (!response.isSuccessful() && response.code() == 401){
                        //Make sure legacy code won't refresh token, or wait until they do
                        try{
                            HttpRequest.semaphore.acquire();
                            if (!application.isLoggedIn()){
                                HttpRequest.semaphore.release();
                                return response;
                            }
                        }catch(InterruptedException e){
                            e.printStackTrace();
                            HttpRequest.semaphore.release();
                            return response;
                        }

                        //Check if different thread updated token
                        if (!prevAccessToken.equals(application.getAccessToken())){
                            HttpRequest.semaphore.release();
                            //Updated therefore use new token and proceed with request, no need for refresh
                            //Update headers with new jwt token
                            Request.Builder builderNew = original.newBuilder()
                                    .header("client-id", SecretUtils.getClientId(context))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer "+application.getAccessToken());
                            return chain.proceed(builderNew.build()); //Ping same endpoint again after token has been refreshed
                        }

                        Log.d(TAG,"401 unauthorized error received, proceeding to refresh access token");

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("refreshToken",application.getRefreshToken());
                        Response<Token> tokenResponse;
                        try{
                             tokenResponse = pitstopAuthApi(context)
                                    .refreshAccessToken(jsonObject).execute();
                            Log.d(TAG,"tokenResponse: "+tokenResponse);
                        }catch(IOException e){
                            HttpRequest.semaphore.release();
                            return response;
                        }

                        if (tokenResponse.isSuccessful()){
                            String token = tokenResponse.body().getAccessToken();
                            Log.d(TAG,"received new token: "+token);
                            application.setTokens(token,application.getRefreshToken());

                            //Update headers with new jwt token
                            Request.Builder builderNew = original.newBuilder()
                                    .header("client-id", SecretUtils.getClientId(context))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer "+token);

                            //Let other threads proceed to use the new token
                            HttpRequest.semaphore.release();
                            return chain.proceed(builderNew.build()); //Ping same endpoint again after token has been refreshed
                        }else{
                            Log.d(TAG,"Token refresh response was not successful, raw response: "+tokenResponse.body());
                            //Let other threads proceed to use the new token
                            HttpRequest.semaphore.release();
                            //Logout, session cannot be continued
                            ((GlobalApplication)context).logOutUser();
                        }
                    }else if (!response.isSuccessful()){
                        Log.d(TAG,"Request failed for reason other than 401, err: "+response.message()+", code: "+response.code());
                    }

                    return response; //Return successful response & non 401 errors
                }).build();
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
    public PitstopAppointmentApi pitstopAppointmentApi(Context context){
        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(PitstopAppointmentApi.class);
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

    @Provides
    @Singleton
    Geocoder geocoder(Context context){
        return new Geocoder(context, Locale.getDefault());
    }

    @Provides
    @Singleton
    PitstopSmoochApi pitstopSmoochApi(Context context){
        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(PitstopSmoochApi.class);
    }

    @Provides
    @Singleton
    PitstopTripApi pitstopTripApi(Context context){

        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(PitstopTripApi.class);
    }

    @Provides
    @Singleton
    PitstopSensorDataApi pitstopSensorDataApi(Context context) {

        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(PitstopSensorDataApi.class);
    }

    @Provides
    @Singleton
    GoogleSnapToRoadApi googleSnapToRoadApi(Context context){
        return new Retrofit.Builder()
                .baseUrl(SecretUtils.getSnapToRoadEndpointUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(getHttpClient(context))
                .build()
                .create(GoogleSnapToRoadApi.class);
    }


    private boolean receiverRegistered = false;
    @Provides
    @Singleton
    Observable<Boolean> connectionObservable(Context context){
        String TAG = "connectionObservable";
        Log.d(TAG,"connectionObservable being created");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        return Observable.create(emitter -> {
            Log.d(TAG,"subscribe() emitter");
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context1, Intent intent) {
                    try {
                        Log.d(TAG,"Received broadcast!");
                        ConnectivityManager conn = (ConnectivityManager)
                                context1.getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (conn != null && conn.getActiveNetworkInfo() != null) {
                            emitter.onNext(conn.getActiveNetworkInfo().isConnected());
                        }
                    }catch(Exception e){
                        emitter.onError(e);
                    }
                }
            };
            if (!receiverRegistered){
                Log.d(TAG,"registering receiver");
                context.registerReceiver(broadcastReceiver,intentFilter);
                receiverRegistered = true;
            }
        });

    }
}
