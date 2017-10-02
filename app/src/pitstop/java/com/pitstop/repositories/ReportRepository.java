package com.pitstop.repositories;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.report.EmissionsReport;
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
        pid.pids.put("2141","0F0C14FF");
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
            e.printStackTrace();
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
            return etContentToJson(new JSONObject(stringResponse).getJSONObject("response"));
        }catch(JSONException e){
            e.printStackTrace();
            return null;
        }
    }

    private EmissionsReport etContentToJson(JSONObject jsonResponse){
        try{
            int id = jsonResponse.getInt("id");
            JSONObject meta = jsonResponse.getJSONObject("meta");
            JSONObject content = jsonResponse.getJSONObject("content");
            JSONObject data = content.getJSONObject("data");
            int vhrId = -1;
            if (meta.has("vhrId"))
                vhrId = meta.getInt("vhrId");
            String misfire = data.getString("Misfire");
            String ignition = data.getString("Ignition");
            String components = data.getString("Components");
            String fuelSystem = data.getString("Fuel System");
            String NMHCCatalyst = data.getString("NMHC Catalyst");
            String boostPressure = data.getString("Boost Pressure");
            String EGRVVTSystem = data.getString("EGR/VVT System");
            String exhaustSensor = data.getString("Exhaust Sensor");
            String NOxSCRMonitor = data.getString("NOx/SCR Monitor");
            String PMFilterMonitoring= data.getString("PM Filter Monitoring");
            boolean pass = content.getBoolean("pass");
            Date createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.CANADA)
                    .parse(jsonResponse.getString("createdAt"));
            return new EmissionsReport(id, vhrId, misfire, ignition, components
                    , fuelSystem, NMHCCatalyst, boostPressure, EGRVVTSystem
                    , exhaustSensor, NOxSCRMonitor, PMFilterMonitoring, createdAt, pass);
        }catch(JSONException | ParseException e){
            e.printStackTrace();
            return null;
        }

    }

    private List<EmissionsReport> jsonToEmissionsReportList(String stringResponse){
        List<EmissionsReport> emissionsReportList = new ArrayList<>();
        try{
            JSONArray response = new JSONObject(stringResponse).getJSONArray("response");
            for (int i=0;i<response.length();i++){
                EmissionsReport et = etContentToJson(response.getJSONObject(i));
                if (et != null){
                    emissionsReportList.add(et);
                }
            }
            return emissionsReportList;
        }catch(JSONException e){
            e.printStackTrace();
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
            e.printStackTrace();
        }

        Log.d(TAG,"createVehicleHealthReport() body: "+body);

        networkHelper.post(String.format("v1/car/%d/report/vehicle-health", carId)
                , (response, requestError) -> {

                    if (requestError == null){
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
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }catch(ParseException e){
            e.printStackTrace();
            return null;
        }
    }

    private VehicleHealthReport jsonToVehicleHealthReport(String response){
        try{
            return vhrContentToJson(new JSONObject(response).getJSONObject("response"));
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

}
