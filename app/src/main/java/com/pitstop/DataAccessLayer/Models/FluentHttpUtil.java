package com.pitstop.DataAccessLayer.Models;

import com.google.gson.Gson;

import org.json.JSONObject;

/**
 * Created by Paul Soladoye on 3/11/2016.
 */
public class FluentHttpUtil {
    private static final Gson GSON = new Gson();
    public static  <Type>  Type deserializeResponse(JSONObject response) {

        return null;
    }

    public static <Type> JSONObject serializeResource(Type resource) {
        return null;
    }
}
