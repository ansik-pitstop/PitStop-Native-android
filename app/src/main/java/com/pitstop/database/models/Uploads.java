package com.pitstop.database.models;

import com.pitstop.database.DBModel;

/**
 * Created by David Liu on 12/11/2015.
 */
public class Uploads extends DBModel {
    public Uploads() {
        super("Uploads", "UploadID", "ScannerID");
    }

    @Override
    protected void setUpTable() {
        columns.put("UploadID","INTEGER PRIMARY KEY AUTOINCREMENT");
        columns.put("UploadedAt","Text");
        columns.put("EntriesUploaded","Text");
        columns.put("ScannerID","Text");
    }

}
