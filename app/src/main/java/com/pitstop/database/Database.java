package com.pitstop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by David Liu on 11/20/2015.
 */
public class Database extends SQLiteOpenHelper {
    private static final String DB_NAME = "PitstopDB";
    DBModel model;
    public Database(Context context, DBModel model) {
        super(context, DB_NAME, null, 1);
        this.model = model;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String makeTableSQL = "CREATE TABLE " + model.getTableName() + " (";
        HashMap<String,String> map = model.getColumnStructure();
        for(String a : map.keySet()){
            makeTableSQL+= " " + a + " " + map.get(a).toUpperCase() + " , ";
        }
        makeTableSQL = makeTableSQL.substring(0,makeTableSQL.length()-2);
        makeTableSQL +=" );";

        sqLiteDatabase.execSQL(makeTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.w("Database Upgrade",
                "Upgrading database from version " + i + " to "
                        + i1 + ", which will destroy all old data");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + model.getTableName());
        onCreate(sqLiteDatabase);
    }
}
