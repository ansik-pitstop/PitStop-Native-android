package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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

import java.lang.reflect.Type;
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

    private JSONArray dtcPackageToJSON(DtcPackage dtcPackage){
        JSONArray dtcArr = new JSONArray();
        for (Map.Entry<String,Boolean> entry: dtcPackage.dtcs.entrySet()){
            JSONObject dtcJson = new JSONObject();
            try{
                dtcJson.put("code",entry.getKey());
                dtcJson.put("isPending",entry.getValue());
                dtcJson.put("rtcTime",dtcPackage.rtcTime);
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
                        try{
                            JSONObject healthReportJson = new JSONObject(response);
                            Type listType = new TypeToken<List<String>>() {}.getType();
                            List<EngineIssue> engineIssues
                                    = gson.fromJson((JsonElement)(healthReportJson.get("dtc")),listType);
                            List<Recall> recalls
                                    = gson.fromJson((JsonElement)healthReportJson.get("recall"),listType);
                            List<Service> services
                                    = gson.fromJson((JsonElement)healthReportJson.get("services"),listType);
                            VehicleHealthReport vehicleHealthReport
                                    = new VehicleHealthReport(engineIssues,recalls,services);
                            Log.d(TAG,"Vehicle health report created: "+vehicleHealthReport);
                            callback.onSuccess(vehicleHealthReport);
                        }catch(JSONException e){
                            e.printStackTrace();
                            callback.onError(RequestError.getUnknownError());
                        }

                    }else{
                        Log.d(TAG,"networkHelper.post() ERROR response: "+requestError.getMessage());
                        callback.onError(requestError);
                    }

        }, body);

    }

}
