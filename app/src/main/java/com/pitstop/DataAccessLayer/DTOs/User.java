package com.pitstop.DataAccessLayer.DTOs;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Paul Soladoye on 3/21/2016.
 */
public class User implements Serializable {

    //@Expose(serialize = false, deserialize = false)
    //private int id;

    @SerializedName("id")
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String password;
    //authData
    private boolean activated;
    private String phone;
    private String role;
    private boolean verifiedEmail;

    public User() {}

    //public int getId() {
    //    return id;
    //}
//
    //public void setId(int id) {
    //    this.id = id;
    //}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerifiedEmail() {
        return verifiedEmail;
    }

    public void setVerifiedEmail(boolean verifiedEmail) {
        this.verifiedEmail = verifiedEmail;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public static User jsonToUserObject(String json) {
        User user = null;
        try {
            user = JsonUtil.json2object(json, User.class);

            if(user.getUserId() == 0) {
                user = new User();
                JSONObject userJson = new JSONObject(json).getJSONObject("user");
                user.setUserId(userJson.getInt("id"));
                user.setFirstName(userJson.getString("firstName"));
                user.setLastName(userJson.getString("lastName"));
                user.setEmail(userJson.getString("email"));
                user.setPhone(userJson.getString("phone"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return user;
    }
}
