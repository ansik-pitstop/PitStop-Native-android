package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
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
        networkHelper.get("v1/report/?carId=" + carId, (response, requestError) -> {
            if (requestError == null){
                callback.onSuccess(new ArrayList<>());
            }else{
                callback.onError(requestError);
            }
        });
    }

    public void createVehicleHealthReport(int carId, boolean isInternal
            , DtcPackage dtc, PidPackage pid, Callback<VehicleHealthReport> callback){
        Log.d(TAG,"createVehicleHealthReport() carId: "+carId+", isInternal: "
                +isInternal+", dtc: "+dtc+", pid: "+pid);
        JSONObject body = new JSONObject();

        try {
            body.put("engineCodes", dtcPackageToJSON(dtc));
            body.put("isInternal", pidPackageToJSON(pid));
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
                dtcJson.put("rtcTime",pidPackage.rtcTime);
                pidArr.put(dtcJson);
            }catch(JSONException e){
                e.printStackTrace();
            }

        }
        return pidArr;
    }

    private VehicleHealthReport jsonToVehicleHealthReport(String response){
        try{
            JSONObject healthReportJson = new JSONObject(response).getJSONObject("response");
            int id = healthReportJson.getInt("id");
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
            return new VehicleHealthReport(id, "2001/01/01", engineIssues,recalls,services); //Todo: retrieve created date
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

}
