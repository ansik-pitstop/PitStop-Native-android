package com.pitstop.database;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by David Liu on 11/20/2015.
 */
public abstract class DBModel implements Serializable{
    private String TableName;
    protected HashMap<String,String> columns;
    protected HashMap<String,String> values;
    public String primaryKey, foreignKey;
    public DBModel(String name, String key, String fkey){
        values = new HashMap<String, String>();
        TableName = name;
        primaryKey = key;
        foreignKey = fkey;
    }

    public String getTableName(){
        return TableName;
    }


    public HashMap<String, String> getColumnStructure(){
        columns = new HashMap<String, String>();
        setUpTable();
        return columns;
    }


    public String[] getColumns(){
        columns = new HashMap<String, String>();
        setUpTable();
        return columns.keySet().toArray(new String[columns.keySet().size()]);
    }

    protected abstract void setUpTable();
    public void setValue(String key, String value){
        values.put(key,value);
    }

    public String getValue(String key){
        return values.get(key);
    }
    public HashMap<String,String> getValues(){
        return values;
    }
}
