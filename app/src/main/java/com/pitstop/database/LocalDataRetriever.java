package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.database.models.Cars;
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Responses;
import com.pitstop.database.models.Services;
import com.pitstop.database.models.Uploads;

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
        dbase = new Database(c);
    }

    /**
     *
     * @param type "Cars","Recalls", "Services", "Responses" or "Uploads"
     * @param values values for input
     */
    public void saveData(String type, HashMap<String,String> values){
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
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses" or "Uploads"
     */
    public ArrayList<DBModel> getDataSet(String type, String column, String value) {
        SQLiteDatabase db = dbase.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + type + " WHERE " + column + "='" + value + "'";
        ArrayList<DBModel> array = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do{
            DBModel curr = null;
                switch (type) {
                    case "Cars":
                        curr = new Cars();
                        break;
                    case "DTCs":
                        curr = new DTCs();
                        break;
                    case "Recalls":
                        curr = new Recalls();
                        break;
                    case "Services":
                        curr = new Services();
                        break;
                    case "Responses":
                        curr = new Responses();
                        break;
                    case "Uploads":
                        curr = new Uploads();
                        break;
                }
                for (int i = 0 ; i < cursor.getColumnCount(); i++) {
                    assert curr != null;
                    curr.setValue(cursor.getColumnName(i), cursor.getString(i));
                }
                array.add(curr);
            }while(cursor.moveToNext());
        }
        db.close();
        return array;
    }

    /**
     *
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses" or "Uploads"
     */
    public ArrayList<String> getDistinctDataSet(String type, String column){
        SQLiteDatabase db = dbase.getReadableDatabase();
        String selectQuery = "SELECT DISTINCT " + column + " FROM " + type;
        ArrayList<String> array = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do{
                array.add(cursor.getString(0));
            }while(cursor.moveToNext());
        }
        db.close();
        return array;
    }

    /**
     *
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses" or "Uploads"
     */
    public DBModel getData(String type, String column, String value){
        dbase.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + type + " WHERE " + column + "='" + value + "'";
        SQLiteDatabase db = dbase.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery,null);
        //Cursor cursor = db.query(type,c.getColumns(),this.id + "=?",new String[]{id},null,null,null,null);
        DBModel curr = null;
        if (cursor.moveToFirst()) {
            switch (type) {
                case "Cars":
                    curr = new Cars();
                    break;
                case "DTCs":
                    curr = new DTCs();
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
        }
        db.close();
        return curr;
    }



    /**
     *
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses" or "Uploads"
     */
    public boolean deleteData(String type, String column, String value) {
        dbase.getReadableDatabase();
        SQLiteDatabase db = dbase.getWritableDatabase();
        db.delete(type, column + "=?", new String[]{value});
        //Cursor cursor = db.rawQuery(selectQuery,null);
        return true;
    }
}
