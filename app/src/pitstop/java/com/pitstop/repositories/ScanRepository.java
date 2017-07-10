package com.pitstop.repositories;

import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.ObdScanner;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class ScanRepository implements Repository {

    private NetworkHelper networkHelper;
    private LocalScannerAdapter localScannerStorage;

    public void storeScanner(ObdScanner scanner){

    }

    public void updateScanner(ObdScanner scanner){

    }

    public void getScannerByCarId(int carId){

    }

    public void getScanner(int scannerId){

    }

    public void removeScanner(int scannerId){

    }

}
