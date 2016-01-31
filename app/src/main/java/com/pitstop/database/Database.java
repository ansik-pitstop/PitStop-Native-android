package com.pitstop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
public class Database extends SQLiteOpenHelper {
    private static final String DB_NAME = "PitstopDB";
    ArrayList<DBModel> models = new ArrayList<>();
    public Database(Context context) {
        super(context, DB_NAME, null, 12);
        models.add(new Cars());
        models.add(new DTCs());
        models.add(new Recalls());
        models.add(new Responses());
        models.add(new Services());
        models.add(new Uploads());
        models.add(new Shops());
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (DBModel model : models) {
            String makeTableSQL = "CREATE TABLE " + model.getTableName() + " (";
            HashMap<String, String> map = model.getColumnStructure();
            for (String a : map.keySet()) {
                makeTableSQL += " " + a + " " + map.get(a).toUpperCase() + " , ";
            }
            makeTableSQL = makeTableSQL.substring(0, makeTableSQL.length() - 2);
            makeTableSQL += " );";
            sqLiteDatabase.execSQL(makeTableSQL);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.w("Database Upgrade",
                "Upgrading database from version " + i + " to "
                        + i1 + ", which will destroy all old data");
        for (DBModel model : models) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + model.getTableName());
        }
        onCreate(sqLiteDatabase);
    }
}
