package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.bluetooth.dataPackages.CastelPidPackage;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
        networkHelper.get(String.format("v1/report/?carId=%d&type=vhr",carId)
                , (response, requestError) -> {
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

    public void getEmissionReports(int carId, Callback<List<EmissionsReport>> callback){
        Log.d(TAG,"getEmissionReports() carId: "+carId);
        networkHelper.get(String.format("v1/report/?carId=%d&type=emission",carId)
                , (response, requestError) -> {
                        if (requestError == null){
                                Log.d(TAG,"networkHelper.get() SUCCESS response: "+response);
                            List<EmissionsReport> emissionsReports
                                    = jsonToEmissionsReportList(response);
                            if (emissionsReports == null){
                                callback.onError(RequestError.getUnknownError());
                            }else{
                                callback.onSuccess(emissionsReports);
                            }
                        }else{
                            Log.d(TAG,"networkHelper.get() ERROR error: "+requestError.getMessage());
                            callback.onError(requestError);
                        }
        });
    }

    public void createEmissionsReport(int carId, int vhrId, boolean isInternal
            , DtcPackage dtc, PidPackage pid, Callback<EmissionsReport> callback){
        Log.d(TAG,"createEmissionsReport() carId: "+carId+", isInternal: "
                +isInternal+", dtc: "+dtc+", pid: "+pid);
        JSONObject body = new JSONObject();
        JSONObject meta = new JSONObject();

        try {
            meta.put("vhrId",vhrId);
            body.put("meta", meta);
            body.put("engineCodes", dtcPackageToJSON(dtc));
            body.put("pid", pidPackageToJSON(pid));
            body.put("isInternal", isInternal);
        } catch (JSONException e) {
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
        }

        Log.d(TAG,"createEmissionsReport() body: "+body);

        networkHelper.post(String.format("v1/car/%d/report/emissions", carId)
                , (response, requestError) -> {

                    if (requestError == null){
                        Log.d(TAG,"networkHelper.post() SUCCESS response: "+response);
                        EmissionsReport emissionsReport = jsonToEmissionsReport(response);

                        if (emissionsReport == null){
                            Log.d(TAG,"Error parsing response.");
                            callback.onError(RequestError.getUnknownError());
                        }
                        else{
                            callback.onSuccess(emissionsReport);
                        }

                    }else{
                        Log.d(TAG,"networkHelper.post() ERROR response: "+requestError.getMessage()
                                +", error: "+requestError.getError()+", response code: "+requestError.getStatusCode());
                        callback.onError(requestError);
                    }

                }, body);
    }

    private EmissionsReport jsonToEmissionsReport(String stringResponse){
        try{
            JSONObject response = new JSONObject(stringResponse).getJSONObject("response");
            int id = response.getInt("id");
            JSONObject content = response.getJSONObject("content");
            JSONObject data = content.getJSONObject("data");
            LinkedHashMap<String,String> sensorMap = new LinkedHashMap<>();
            Iterator keyIterator = data.keys();
            while (keyIterator.hasNext()){
                String key = (String)keyIterator.next();
                sensorMap.put(key,data.getString(key));
            }
            boolean pass = content.getBoolean("pass");
            String reason = "";
            if (content.has("reason"))
                reason = content.getString("reason");
            Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH)
                    .parse(response.getString("createdAt"));
            return new EmissionsReport(id , createdAt, pass, reason, sensorMap);


        }catch(JSONException | ParseException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }
    }

    private List<EmissionsReport> jsonToEmissionsReportList(String stringResponse){
        List<EmissionsReport> emissionsReportList = new ArrayList<>();
        try{
            JSONArray response = new JSONObject(stringResponse).getJSONArray("response");
            for (int i=0;i<response.length();i++){
                EmissionsReport et;
                try{
                    JSONObject currentJson = response.getJSONObject(i);
                    int id = currentJson.getInt("id");
                    JSONObject content = currentJson.getJSONObject("content");
                    JSONObject data = content.getJSONObject("data");
                    LinkedHashMap<String,String> sensorMap = new LinkedHashMap<>();
                    Iterator keyIterator = data.keys();
                    while (keyIterator.hasNext()){
                        String key = (String)keyIterator.next();
                        sensorMap.put(key,data.getString(key));
                    }
                    boolean pass = false;
                    if (content.has("pass")){
                        pass = content.getBoolean("pass");
                    }
                    String reason = "";
                    if (content.has("reason"))
                        reason = content.getString("reason");
                    Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.CANADA)
                            .parse(currentJson.getString("createdAt"));
                    et = new EmissionsReport(id , createdAt, pass, reason, sensorMap);

                }catch(JSONException | ParseException e){
                    Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
                    break;
                }

                JSONObject meta = null;
                if (!response.getJSONObject(i).isNull("meta"))
                    meta = response.getJSONObject(i).getJSONObject("meta");
                    if (meta != null && meta.has("vhrId"))
                        et.setVhrId(meta.getInt("vhrId"));
                emissionsReportList.add(et);
            }
            return emissionsReportList;
        }catch(JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }
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
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
        }

        Log.d(TAG,"createVehicleHealthReport() body: "+body);

        networkHelper.post(String.format("v1/car/%d/report/vehicle-health", carId)
                , (response, requestError) -> {
                    Log.d(TAG,"vhr response: "+response);
                    if (requestError == null){
                        Log.d(TAG, "VHR RESPONSE: " + response);
                        VehicleHealthReport vehicleHealthReport = jsonToVehicleHealthReport(response);
                        if (vehicleHealthReport == null){
                            Log.d(TAG,"Error parsing response.");
                            callback.onError(RequestError.getUnknownError());
                        }
                        else{
                            callback.onSuccess(vehicleHealthReport);
                        }

                    }else{
                        Log.d(TAG,"networkHelper.post() ERROR response: "+requestError);
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
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            }

        }
        return dtcArr;
    }

    private JSONArray pidPackageToJSON(PidPackage pidPackage){
        JSONArray pidArr = new JSONArray();
        for (Map.Entry<String,String> entry: pidPackage.getPids().entrySet()){
            JSONObject dtcJson = new JSONObject();
            try{
                dtcJson.put("id",entry.getKey());
                dtcJson.put("data",entry.getValue());
                String rtcTime = "0";
                if (pidPackage instanceof CastelPidPackage){
                    CastelPidPackage castelPidPackage = (CastelPidPackage)pidPackage;
                    rtcTime = castelPidPackage.getRtcTime();
                }
                dtcJson.put("rtcTime",Long.valueOf(rtcTime));
                pidArr.put(dtcJson);
            }catch(JSONException e){
                Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            }

        }
        return pidArr;
    }

    private List<VehicleHealthReport> jsonToVehicleHealthReportList(String response){
        List<VehicleHealthReport> vehicleHealthReports = new ArrayList<>();
        JSONArray reportList;
        try {
            reportList = new JSONObject(response).getJSONArray("response");
            for (int i=0;i<reportList.length();i++){
                VehicleHealthReport vhr = vhrContentToJson(reportList.getJSONObject(i));
                if (vhr != null){
                    vehicleHealthReports.add(vhr);
                }
            }
        }catch (JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }
        return vehicleHealthReports;
    }

    private VehicleHealthReport vhrContentToJson(JSONObject vhrResponse){
        try{
            int id = vhrResponse.getInt("id");
            Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .parse(vhrResponse.getString("createdAt"));
            JSONObject healthReportContentJson = vhrResponse.getJSONObject("content");

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
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }catch(ParseException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }
    }

    private VehicleHealthReport jsonToVehicleHealthReport(String response){
        try{
            return vhrContentToJson(new JSONObject(response).getJSONObject("response"));
        }catch (JSONException e){
            Logger.getInstance().logException(TAG,e, DebugMessage.TYPE_REPO);
            return null;
        }
    }

}
