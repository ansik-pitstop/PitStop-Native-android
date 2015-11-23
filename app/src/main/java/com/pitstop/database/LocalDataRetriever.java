package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by David Liu on 11/20/2015.
 */
public class LocalDataRetriever {
    static Database dbase;
    private Context context;
    private String id,fid;

    public LocalDataRetriever(Context c){
        context = c;
    }

    /**
     *
     * @param type "Cars","Recalls" or "Services"
     * @param values values for input
     */
    public void saveData(String type, HashMap<String,String> values){
        setUpDBAccess(type);
        SQLiteDatabase db = dbase.getWritableDatabase();
        ContentValues inputValues = new ContentValues();
        for(String i : values.keySet()){
            inputValues.put(i,values.get(i));
        }
        db.insert(
                type,
                "null",
                inputValues);
        db.close();

    }

    /**
     *
     * @param type "Cars","Recalls" or "Services"
     */
    public ArrayList<DBModel> getData(String type, String id){
        setUpDBAccess(type);
        if (type =="Cars") this.id = fid;
        String selectQuery = "SELECT  * FROM " + type + " WHERE " + this.id + "='" + id+"'";
        SQLiteDatabase db = dbase.getReadableDatabase();
        Cars c = new Cars();
        Cursor cursor = db.rawQuery(selectQuery,null);
        //Cursor cursor = db.query(type,c.getColumns(),this.id + "=?",new String[]{id},null,null,null,null);
        ArrayList<DBModel> array = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                DBModel curr = null;
                switch (type) {
                    case "Cars":
                        curr = new Cars();
                        break;
                    case "Recalls":
                        curr = new Recalls();
                        break;
                    case "Services":
                        curr = new Services();
                        break;
                }
                for (int i = 0 ; i < cursor.getColumnCount(); i++) {
                    assert curr != null;
                    curr.setValue(cursor.getColumnName(i), cursor.getString(i));
                }
                array.add(curr);
            } while (cursor.moveToNext());
        }
        db.close();
        return array;
    }

    private void setUpDBAccess(String type){
        switch (type) {
            case "Cars":
                dbase = new Database(context, new Cars());
                id = Cars.primaryKey;
                fid = Cars.foreignKey;
                break;
            case "Recalls":
                dbase = new Database(context, new Recalls());
                id = Recalls.primaryKey;
                break;
            case "Services":
                dbase = new Database(context, new Services());
                id = Services.primaryKey;
                break;
        }
    }
}
