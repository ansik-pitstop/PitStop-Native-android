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
import com.pitstop.database.models.Shops;
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
    public long saveData(String type, HashMap<String,String> values){
        SQLiteDatabase db = dbase.getWritableDatabase();
        ContentValues inputValues = new ContentValues();
        for(String i : values.keySet()){
            inputValues.put(i,values.get(i));
        }
        long id = db.insert(
                type,
                "null",
                inputValues);
        db.close();
        return id;

    }

    /**
     *
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses", "Uploads", "Shops"
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
                    case "Shops":
                        curr = new Shops();
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
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses", "Uploads", "Shops"
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
     * @param type "Cars", "Dtcs", "Recalls", "Services", "Responses", "Uploads", "Shops"
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
                case "Shops":
                    curr = new Shops();
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

    public ArrayList<DBModel> getResponse(String deviceId, String start, String end) {
        SQLiteDatabase db = dbase.getReadableDatabase();
        String selectQuery = "SELECT * FROM Responses WHERE ResponseID > " + start + " AND ResponseID < " + end + " AND deviceID = " + deviceId;
        ArrayList<DBModel> array = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do{
                DBModel curr = new Responses();
                for (int i = 0 ; i < cursor.getColumnCount(); i++) {
                    curr.setValue(cursor.getColumnName(i), cursor.getString(i));
                }
                array.add(curr);
            }while(cursor.moveToNext());
        }
        db.close();
        return array;
    }

    public void updateData(String table, String id, String index, HashMap<String, String> values) {
        SQLiteDatabase db = dbase.getWritableDatabase();
        ContentValues inputValues = new ContentValues();
        for(String i : values.keySet()){
            inputValues.put(i,values.get(i));
        }
        db.update(
                table,
                inputValues,
                id + "= ?",
                new String []{index});
        db.close();

    }

    public DBModel getLastRow(String tableName, String column) {
        SQLiteDatabase db = dbase.getReadableDatabase();
        String selectQuery = "SELECT * FROM "+tableName+" ORDER BY "+column+" DESC LIMIT 1";
        Cursor cursor = db.rawQuery(selectQuery, null);

        DBModel curr = null;
        if (cursor.moveToFirst()) {
            do{
                switch (tableName){
                    case "Uploads":
                        curr = new Uploads();
                        break;
                    case "Responses":
                        curr = new Responses();
                        break;
                }
                for (int i = 0 ; i < cursor.getColumnCount(); i++) {
                    curr.setValue(cursor.getColumnName(i), cursor.getString(i));
                }
            }while(cursor.moveToNext());
        }
        db.close();
        return curr;
    }

    public ArrayList<DBModel> getAllDataSet(String type) {
        SQLiteDatabase db = dbase.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + type;
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
                    case "Shops":
                        curr = new Shops();
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
}
