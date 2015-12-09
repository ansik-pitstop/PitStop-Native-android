package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Responses;
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
        dbase = new Database(c);
    }

    /**
     *
     * @param type "Cars","Recalls" or "Services" or "Responses"
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
     * @param type "Cars","Recalls" or "Services" or "Responses"
     */
    public ArrayList<DBModel> getDataSet(String type, String id){
        SQLiteDatabase db = dbase.getReadableDatabase();
        if (type.equals("Cars")) this.id = (new Cars()).foreignKey;
        if (type.equals("Services")) this.id = (new Services()).primaryKey;
        if (type.equals("Recalls")) this.id = (new Recalls()).primaryKey;
        if (type.equals("Responses")) this.id = (new Responses()).foreignKey;
        String selectQuery = "SELECT  * FROM " + type + " WHERE " + this.id + "='" + id+"'";
        ArrayList<DBModel> array = new ArrayList<>();
        //Cursor cursor = db.query(type,c.getColumns(),this.id + "=?",new String[]{id},null,null,null,null);
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do{
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
            }while(cursor.moveToNext());
        }
        db.close();
        return array;
    }

    /**
     *
     * @param type "Cars","Recalls" or "Services" or "Responses"
     */
    public DBModel getData(String type, String id){
        dbase.getReadableDatabase();
        if (type.equals("Cars")) this.id = (new Cars()).foreignKey;
        if (type.equals("Services")) this.id = (new Services()).primaryKey;
        if (type.equals("Recalls")) this.id = (new Recalls()).primaryKey;
        if (type.equals("Responses")) this.id = (new Responses()).primaryKey;
        String selectQuery = "SELECT  * FROM " + type + " WHERE " + this.id + "='" + id+"'";
        SQLiteDatabase db = dbase.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery,null);
        //Cursor cursor = db.query(type,c.getColumns(),this.id + "=?",new String[]{id},null,null,null,null);
        DBModel curr = null;
        if (cursor.moveToFirst()) {
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
        }
        db.close();
        return curr;
    }



    /**
     *
     * @param type "Cars","Recalls", "Services" or "Responses"
     */
    public boolean deleteData(String type, String id){
        dbase.getReadableDatabase();
        if (type.equals("Cars")) this.id = (new Cars()).foreignKey;
        if (type.equals("Services")) this.id = (new Services()).primaryKey;
        if (type.equals("Recalls")) this.id = (new Recalls()).primaryKey;
        if (type.equals("Responses")) this.id = (new Responses()).foreignKey;
        String selectQuery = "DELETE FROM " + type + " WHERE " + this.id + "='" + id+"'";
        SQLiteDatabase db = dbase.getWritableDatabase();
        db.delete("Cars", this.id +" = ?",new String[]{id});
        //Cursor cursor = db.rawQuery(selectQuery,null);
        return true;
    }
}
