package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.EmissionsReport;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class ReportRepository implements Repository {

    private final String TAG = getClass().getSimpleName();
    private final Gson gson = new Gson();
    private NetworkHelper networkHelper;

    public ReportRepository(NetworkHelper networkHelper) {
        this.networkHelper = networkHelper;
    }

    public void getVehicleHealthReports(int carId, Callback<List<VehicleHealthReport>> callback){
        Log.d(TAG,"getVehicleHealthReports() carId: "+carId);
        networkHelper.get("v1/report/?carId=" + carId, (response, requestError) -> {
            if (requestError == null){
                Log.d(TAG,"networkHelper.get() SUCCESS response: "+response);
                List<VehicleHealthReport> vehicleHealthReports
                        = jsonToVehicleHealthReportList(response);
                if (vehicleHealthReports == null){
                    callback.onError(RequestError.getUnknownError());
                    return;
                }else{
                    callback.onSuccess(vehicleHealthReports);
                }
            }else{
                Log.d(TAG,"networkHelper.get() ERROR error: "+requestError.getMessage());
                callback.onError(requestError);
            }
        });
    }

    public void createEmissionsReport(int carId, boolean isInternal
            , DtcPackage dtc, PidPackage pid, Callback<EmissionsReport> callback){
        pid.pids.put("2141","0F0C14FF");
        Log.d(TAG,"createEmissionsReport() carId: "+carId+", isInternal: "
                +isInternal+", dtc: "+dtc+", pid: "+pid);
        JSONObject body = new JSONObject();

        try {
            body.put("engineCodes", dtcPackageToJSON(dtc));
            body.put("pid", pidPackageToJSON(pid));
            body.put("isInternal", isInternal);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"createEmissionsReport() body: "+body);

        networkHelper.post(String.format("v1/car/%d/report/emissions", carId)
                , (response, requestError) -> {

                    if (requestError == null){
                        Log.d(TAG,"networkHelper.post() SUCCESS response: "+response);
//                        EmissionsReport emissionsReport = jsonToEmissionsReport(response);
//                        if (emissionsReport == null){
//                            Log.d(TAG,"Error parsing response.");
//                            callback.onError(RequestError.getUnknownError());
//                        }
//                        else{
//                            callback.onSuccess(emissionsReport);
//                        }

                    }else{
                        Log.d(TAG,"networkHelper.post() ERROR response: "+requestError.getMessage()
                                +", error: "+requestError.getError()+", response code: "+requestError.getStatusCode());
                        callback.onError(requestError);
                    }

                }, body);
    }

    public void createVehicleHealthReport(int carId, boolean isInternal
            , DtcPackage dtc, PidPackage pid, Callback<VehicleHealthReport> callback){
        Log.d(TAG,"createVehicleHealthReport() carId: "+carId+", isInternal: "
                +isInternal+", dtc: "+dtc+", pid: "+pid);
        JSONObject body = new JSONObject();

        try {
            body.put("engineCodes", dtcPackageToJSON(dtc));
            body.put("pid", pidPackageToJSON(pid));
            body.put("isInternal", isInternal);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"createVehicleHealthReport() body: "+body);

        networkHelper.post(String.format("v1/car/%d/report/vehicle-health", carId)
                , (response, requestError) -> {

                    if (requestError == null){
                        Log.d(TAG,"networkHelper.post() SUCCESS response: "+response);
                        VehicleHealthReport vehicleHealthReport = jsonToVehicleHealthReport(response);
                        if (vehicleHealthReport == null){
                            Log.d(TAG,"Error parsing response.");
                            callback.onError(RequestError.getUnknownError());
                        }
                        else{
                            callback.onSuccess(vehicleHealthReport);
                        }

                    }else{
                        Log.d(TAG,"networkHelper.post() ERROR response: "+requestError.getMessage());
                        callback.onError(requestError);
                    }

        }, body);

    }

    private JSONArray dtcPackageToJSON(DtcPackage dtcPackage){
        JSONArray dtcArr = new JSONArray();
        for (Map.Entry<String,Boolean> entry: dtcPackage.dtcs.entrySet()){
            JSONObject dtcJson = new JSONObject();
            try{
                dtcJson.put("code",entry.getKey());
                dtcJson.put("isPending",entry.getValue());
                dtcJson.put("rtcTime",Long.valueOf(dtcPackage.rtcTime));
                dtcArr.put(dtcJson);
            }catch(JSONException e){
                e.printStackTrace();
            }

        }
        return dtcArr;
    }

    private JSONArray pidPackageToJSON(PidPackage pidPackage){
        JSONArray pidArr = new JSONArray();
        for (Map.Entry<String,String> entry: pidPackage.pids.entrySet()){
            JSONObject dtcJson = new JSONObject();
            try{
                dtcJson.put("id",entry.getKey());
                dtcJson.put("data",entry.getValue());
                dtcJson.put("rtcTime",Long.valueOf(pidPackage.rtcTime));
                pidArr.put(dtcJson);
            }catch(JSONException e){
                e.printStackTrace();
            }

        }
        return pidArr;
    }

    private List<VehicleHealthReport> jsonToVehicleHealthReportList(String response){
        try{
            List<VehicleHealthReport> vehicleHealthReports = new ArrayList<>();
            JSONArray reportList = new JSONObject(response).getJSONArray("response");

            for (int i=0;i<reportList.length();i++){

                JSONObject healthReportJson = reportList.getJSONObject(i);
                int id = healthReportJson.getInt("id");
                Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.CANADA)
                        .parse(healthReportJson.getString("createdAt"));
                JSONObject healthReportContentJson = healthReportJson.getJSONObject("content");

                List<EngineIssue> engineIssues
                        = gson.fromJson(healthReportContentJson.get("dtc").toString()
                        ,new TypeToken<List<EngineIssue>>() {}.getType());
                List<Recall> recalls
                        = gson.fromJson(healthReportContentJson.get("recall").toString()
                        ,new TypeToken<List<Recall>>() {}.getType());
                List<Service> services
                        = gson.fromJson(healthReportContentJson.get("services").toString()
                        ,new TypeToken<List<Service>>() {}.getType());

                vehicleHealthReports.add(
                        new VehicleHealthReport(id, createdAt, engineIssues,recalls,services));
            }
            return vehicleHealthReports;

        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }catch(ParseException e){
            e.printStackTrace();
            return null;
        }
    }

    private VehicleHealthReport jsonToVehicleHealthReport(String response){
        try{
            JSONObject healthReportJson = new JSONObject(response).getJSONObject("response");
            int id = healthReportJson.getInt("id");
            Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .parse(healthReportJson.getString("createdAt"));
            JSONObject healthReportContentJson = healthReportJson.getJSONObject("content");

            List<EngineIssue> engineIssues
                    = gson.fromJson(healthReportContentJson.get("dtc").toString()
                    ,new TypeToken<List<EngineIssue>>() {}.getType());
            List<Recall> recalls
                    = gson.fromJson(healthReportContentJson.get("recall").toString()
                    ,new TypeToken<List<Recall>>() {}.getType());
            List<Service> services
                    = gson.fromJson(healthReportContentJson.get("services").toString()
                    ,new TypeToken<List<Service>>() {}.getType());
            return new VehicleHealthReport(id, createdAt , engineIssues,recalls,services);
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }catch(ParseException e){
            e.printStackTrace();
            return null;
        }
    }

}
