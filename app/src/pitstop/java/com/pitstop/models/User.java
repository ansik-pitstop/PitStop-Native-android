package com.pitstop.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.castel.obd.util.JsonUtil;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by Paul Soladoye on 3/21/2016.
 */
public class User implements Parcelable {

    private static final String TAG = User.class.getSimpleName();

    private int id;
    private List<String> installationId;
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String password;
    private Car currentCar;
    //authData
    private boolean activated;
    private String phone;
    private String role;
    private boolean verifiedEmail;
    private Settings settings;

    public User() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Car getCurrentCar() {
        return currentCar;
    }

    public void setCurrentCar(Car currentCar) {
        this.currentCar = currentCar;
    }

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

    public List<String> getInstallationID() {
        return installationId;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setInstallationID(List<String> installationID) {
        this.installationId = installationID;
    }

    public static User jsonToUserObject(String json) {

        User user = null;
        try {
            user = JsonUtil.json2object(json, User.class);
            //Log.d("installationID", user.getInstallationID().toString());
            Log.d("userJson", json);
            if(user.getId() == 0) {
                user = new User();

                JSONObject userJson = new JSONObject(json).getJSONObject("user");
                /*List<String> userInstallationIds;
                userInstallationIds = new Gson().fromJson(userJson.getJSONArray("installationId").toString(), new TypeToken<List<String>>() {
                }.getType());

                user.setInstallationID(userInstallationIds);*/
                user.setId(userJson.getInt("id"));
                user.setFirstName(userJson.getString("firstName"));
                user.setLastName(userJson.getString("lastName"));
                user.setEmail(userJson.getString("email"));
                user.setPhone(userJson.getString("phone"));

                JSONObject settings = userJson.getJSONObject("settings");
                int carId = 0;
                boolean firstCarAdded = true; //if not present, default is true

                if (settings.has("isFirstCarAdded")){
                    firstCarAdded = settings.getBoolean("isFirstCarAdded");
                }
                if (settings.has("mainCar")){
                    carId = settings.getInt("mainCar");
                }
                user.setSettings(new Settings(user.getId(),carId,firstCarAdded));

                /*Log.d("installationIDs", user.getInstallationID().toString());*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG,"jsonToUserObject() user: "+user + ", json: "+json);

        return user;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.firstName);
        dest.writeString(this.lastName);
        dest.writeString(this.email);
        dest.writeString(this.userName);
        dest.writeString(this.password);
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
        dest.writeString(this.phone);
        dest.writeString(this.role);
        dest.writeByte(this.verifiedEmail ? (byte) 1 : (byte) 0);
    }

    protected User(Parcel in) {
        this.id = in.readInt();
        this.firstName = in.readString();
        this.lastName = in.readString();
        this.email = in.readString();
        this.userName = in.readString();
        this.password = in.readString();
        this.activated = in.readByte() != 0;
        this.phone = in.readString();
        this.role = in.readString();
        this.verifiedEmail = in.readByte() != 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public String toString(){
        try{
            return String.format("id: %d, firstName: %s, lastName: %s, email: %s, settings: %s"
                    ,id,firstName,lastName,email,settings);
        }catch(NullPointerException e){
            return "null";
        }
    }
}
