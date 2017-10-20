package com.pitstop.ui.main_activity

import android.app.Activity
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast

import com.parse.ParseACL
import com.parse.ParseInstallation
import com.pitstop.BuildConfig
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.bluetooth.BluetoothAutoConnectService
import com.pitstop.database.LocalCarStorage
import com.pitstop.database.LocalScannerStorage
import com.pitstop.database.LocalShopStorage
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerTempNetworkComponent
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.TempNetworkComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase
import com.pitstop.interactors.get.GetCarsByUserIdUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.interactors.set.SetFirstCarAddedUseCase
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.models.ObdScanner
import com.pitstop.models.ReadyDevice
import com.pitstop.models.issue.CarIssue
import com.pitstop.network.RequestError
import com.pitstop.observer.BluetoothConnectionObservable
import com.pitstop.observer.BluetoothConnectionObserver
import com.pitstop.observer.Device215BreakingObserver
import com.pitstop.ui.IBluetoothServiceActivity
import com.pitstop.ui.LoginActivity
import com.pitstop.ui.add_car.AddCarActivity
import com.pitstop.ui.add_car.PromptAddCarActivity
import com.pitstop.ui.issue_detail.IssueDetailsActivity
import com.pitstop.ui.my_appointments.MyAppointmentActivity
import com.pitstop.ui.my_trips.MyTripsActivity
import com.pitstop.ui.service_request.RequestServiceActivity
import com.pitstop.ui.services.custom_service.CustomServiceActivity
import com.pitstop.utils.AnimatedDialogBuilder
import com.pitstop.utils.LogUtils
import com.pitstop.utils.MigrationService
import com.pitstop.utils.MixpanelHelper
import com.pitstop.utils.NetworkHelper

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.HashMap

import io.smooch.core.Smooch
import io.smooch.core.User

/**
 * Created by David on 6/8/2016.
 */
class MainActivity : IBluetoothServiceActivity(), MainActivityCallback, Device215BreakingObserver, BluetoothConnectionObserver, TabSwitcher {

    private var application: GlobalApplication? = null
    private var serviceIsBound = false
    private var isFirstAppointment = false
    private var serviceIntent: Intent? = null
    private var garageDrawerLayout: DrawerLayout?  = null
    private var drawerToggle : ActionBarDrawerToggle? = null
    protected var serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "connecting: onServiceConnection")
            // cast the IBinder and get MyService instance

            autoConnectService = (service as BluetoothAutoConnectService.BluetoothBinder).service
            autoConnectService.subscribe(this@MainActivity)
            autoConnectService.requestDeviceSearch(false, false)
            displayDeviceState(autoConnectService.deviceState)

            checkPermissions()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i(TAG, "Disconnecting: onServiceConnection")
            autoConnectService = null
        }
    }

    // Database accesses
    private var carLocalStore: LocalCarStorage? = null
    private var shopLocalStore: LocalShopStorage? = null
    private var scannerLocalStore: LocalScannerStorage? = null

    // Views
    private var rootView: View? = null
    private var toolbar: Toolbar? = null

    private var progressDialog: ProgressDialog? = null
    private var isLoading = false

    // Utils / Helper
    private var mixpanelHelper: MixpanelHelper? = null
    private var networkHelper: NetworkHelper? = null

    private var userSignedUp: Boolean = false
    private var tabFragmentManager: TabFragmentManager? = null

    private var useCaseComponent: UseCaseComponent? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userSignedUp = intent.getBooleanExtra(LoginActivity.USER_SIGNED_UP, false)

        application = applicationContext as GlobalApplication
        mixpanelHelper = MixpanelHelper(applicationContext as GlobalApplication)

        val tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(ContextModule(this))
                .build()
        networkHelper = tempNetworkComponent.networkHelper()

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(applicationContext))
                .build()

        //If user just signed up then store the user has not sent its initial smooch message
        if (userSignedUp) {
            setGreetingsNotSent()
        }

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(MigrationService.notificationId)

        rootView = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(rootView)
        val acl = ParseACL()
        acl.publicReadAccess = true
        acl.publicWriteAccess = true

        val installation = ParseInstallation.getCurrentInstallation()
        installation.acl = acl
        installation.put("userId", application!!.currentUserId.toString())
        installation.saveInBackground { e ->
            if (e == null) {
                Log.d(TAG, "Installation saved")
            } else {
                Log.w(TAG, "Error saving installation: " + e.message)
            }
        }

        serviceIntent = Intent(this@MainActivity, BluetoothAutoConnectService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        serviceIsBound = true



        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        displayDeviceState(BluetoothConnectionObservable.State.DISCONNECTED)


        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE, true )){
            val drawerLayout: DrawerLayout = layoutInflater.inflate(R.layout.activity_debug_drawer, null) as DrawerLayout
            (drawerLayout as DrawerLayout).addView(rootView)
            setContentView(drawerLayout)
            drawerToggle = ActionBarDrawerToggle(this, drawerLayout,R.string.app_name, R.string.app_name );
            drawerLayout!!.setDrawerListener(drawerToggle)
            setContentView(drawerLayout)
        }
        else{
        }

        progressDialog = ProgressDialog(this)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setCanceledOnTouchOutside(false)

        // Local db adapters
        carLocalStore = LocalCarStorage(application)
        shopLocalStore = LocalShopStorage(application)
        scannerLocalStore = LocalScannerStorage(application)

        logAuthInfo()

        updateScannerLocalStore()
        tabFragmentManager = TabFragmentManager(this, mixpanelHelper)
        tabFragmentManager!!.createTabs()


    }

/*

      private void setGarageDrawerLayout(){
        garageDrawerLayout = this.layoutInflater().inflate(R.layout.layout_drawer_garage, null);
        garageDrawerLayout.addView(rootView,0);
        super.setContentView(garageDrawerLayout);

        mDrawerToggle = new ActionBarDrawerToggle(this,garageDrawerLayout,R.string.app_name,R.string.app_name){
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        garageDrawerLayout.setDrawerListener(mDrawerToggle);
    }
*/



    // public void removeBluetoothFragmentCallback

    private fun setGreetingsNotSent() {
        useCaseComponent!!.setFirstCarAddedUseCase().execute(false, object : SetFirstCarAddedUseCase.Callback {
            override fun onFirstCarAddedSet() {
                //Do nothing
            }

            override fun onError(error: RequestError) {
                //Error logic here
            }
        })
    }

    fun changeTheme(darkTheme: Boolean) {
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(if (darkTheme) Color.BLACK else ContextCompat.getColor(this, R.color.primary)))
        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, if (darkTheme) R.color.black else R.color.primary_dark)
        }
    }

    private fun loadDealerDesign(car: Car) {
        //Update tab design to the current dealerships custom design if applicable
        if (car.dealership != null) {
            if (BuildConfig.DEBUG && (car.dealership.id == 4 || car.dealership.id == 18)) {

                bindMercedesDealerUI()
            } else if (!BuildConfig.DEBUG && car.dealership.id == 14) {
                bindMercedesDealerUI()
            } else {
                bindDefaultDealerUI()
            }
            hideLoading()
        }
    }

    private fun displayDeviceState(state: String) {
        Log.d(TAG, "displayDeviceState(): " + state)

        if (supportActionBar == null) return

        runOnUiThread {
            if (state == BluetoothConnectionObservable.State.CONNECTED_VERIFIED) {
                supportActionBar!!.subtitle = "Device connected"
            } else if (state == BluetoothConnectionObservable.State.VERIFYING) {
                supportActionBar!!.subtitle = "Verifying device"
            } else if (state == BluetoothConnectionObservable.State.SEARCHING) {
                supportActionBar!!.subtitle = "Searching for device"
            } else if (state == BluetoothConnectionObservable.State.DISCONNECTED) {
                supportActionBar!!.subtitle = "Device not connected"
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d(TAG, "onResume, serviceBound? " + serviceIsBound)

        checkPermissions()
        if (!serviceIsBound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            serviceIsBound = true
        }
        if (autoConnectService != null) {
            displayDeviceState(autoConnectService.deviceState)
            autoConnectService.subscribe(this)
            autoConnectService.requestDeviceSearch(false, false)
        }

        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car) {
                loadDealerDesign(car)
            }

            override fun onNoCarSet() {
                //startPromptAddCarActivity();
            }

            override fun onError(error: RequestError) {

            }
        })
    }

    override fun onStop() {
        hideLoading()

        if (autoConnectService != null) {
            autoConnectService.unsubscribe(this)
        }
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }

        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "onActivityResult")

        //Returned from car being added
        if (data != null && requestCode == RC_ADD_CAR) {

            if (resultCode == AddCarActivity.ADD_CAR_SUCCESS_HAS_DEALER || resultCode == AddCarActivity.ADD_CAR_SUCCESS_NO_DEALER) {
                val addedCar = data.getParcelableExtra<Car>(CAR_EXTRA)

                updateSmoochUser(application!!.currentUser, addedCar)

                useCaseComponent!!
                        .checkFirstCarAddedUseCase()
                        .execute(object : CheckFirstCarAddedUseCase.Callback {
                            override fun onFirstCarAddedChecked(added: Boolean) {
                                if (!added) {

                                    sendSignedUpSmoochMessage(application!!.currentUser)
                                    prepareAndStartTutorialSequence()

                                    useCaseComponent!!.setFirstCarAddedUseCase()
                                            .execute(true, object : SetFirstCarAddedUseCase.Callback {
                                                override fun onFirstCarAddedSet() {
                                                    //Variable has been set
                                                }

                                                override fun onError(error: RequestError) {
                                                    //Networking error logic here
                                                }
                                            })
                                }
                            }

                            override fun onError(error: RequestError) {
                                //Error logic here
                            }
                        })

            } else {
                mixpanelHelper!!.trackButtonTapped("Cancel in Add Car", "Add Car")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun sendSignedUpSmoochMessage(user: com.pitstop.models.User) {
        Log.d("MainActivity Smooch", "Sending message")
        Smooch.getConversation().sendMessage(io.smooch.core.Message(user.firstName +
                (if (user.lastName == null || user.lastName == "null")
                    ""
                else
                    " " + user.lastName) + " has signed up for Pitstop!"))
    }

    private fun updateSmoochUser(user: com.pitstop.models.User?, car: Car?) {
        if (car == null || user == null) return

        val customProperties = HashMap<String, Any>()
        customProperties.put("VIN", car.vin)
        Log.d(TAG, car.vin)
        customProperties.put("Car Make", car.make)
        Log.d(TAG, car.make)
        customProperties.put("Car Model", car.model)
        Log.d(TAG, car.model)
        customProperties.put("Car Year", car.year)
        Log.d(TAG, car.year.toString())

        //Add custom user properties
        if (car.dealership != null) {
            customProperties.put("Email", car.dealership.email)
            Log.d(TAG, car.dealership.email)
        }

        if (user != null) {
            customProperties.put("Phone", user.phone)
            User.getCurrentUser().firstName = user.firstName
            User.getCurrentUser().email = user.email
        }

        User.getCurrentUser().addProperties(customProperties)
    }

    val bluetoothConnectService: BluetoothAutoConnectService
        get() = autoConnectService

    override fun onBackPressed() {
        Log.i(TAG, "onBackPressed")

        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                settingsClicked(null)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    private fun startPromptAddCarActivity() {
        val intent = Intent(this@MainActivity, PromptAddCarActivity::class.java)
        //Don't allow user to come back to tabs without first setting a car
        startActivityForResult(intent, RC_ADD_CAR)
    }

    private fun bindMercedesDealerUI() {
        changeTheme(true)
    }

    private fun bindDefaultDealerUI() {
        Log.d(TAG, "Binding deafult dealer UI.")
        //Change theme elements back to default
        changeTheme(false)
    }

    private fun updateScannerLocalStore() {
        useCaseComponent!!.carsByUserIdUseCase.execute(object : GetCarsByUserIdUseCase.Callback {
            override fun onCarsRetrieved(cars: List<Car>) {
                Log.d(TAG, "retrievedCars: " + cars)
                for (car in cars) { // populate scanner table with scanner ids associated with the cars
                    if (!scannerLocalStore!!.isCarExist(car.id)) {
                        carLocalStore!!.deleteAllCars()
                        carLocalStore!!.storeCars(cars)
                        scannerLocalStore!!.storeScanner(ObdScanner(car.id, car.scannerId))
                        Log.d("Storing Scanner", car.id.toString() + " " + car.scannerId)
                    }
                }
            }

            override fun onError(error: RequestError) {

            }
        })
    }

    private var ignoreMissingDeviceName = false
    private var alertInvalidDeviceNameDialog: AlertDialog? = null
    private var idInput = false

    private fun displayGetScannerIdDialog() {
        if (idInput) return
        if (alertInvalidDeviceNameDialog != null && alertInvalidDeviceNameDialog!!.isShowing)
            return

        runOnUiThread {
            val input = EditText(this@MainActivity)
            val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
            alertDialogBuilder.setTitle("Device Id Invalid")
            alertDialogBuilder
                    .setView(input)
                    .setMessage("Your OBD device has lost its ID or is invalid, please input " + "the ID found on the front of the device so our algorithm can fix it.")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->
                        autoConnectService.setDeviceNameAndId(input.text
                                .toString().trim { it <= ' ' }.toUpperCase())

                        allowDeviceOverwrite = false
                        idInput = true
                    }
                    .setNegativeButton("Ignore") { dialog, id ->
                        dialog.cancel()
                        ignoreMissingDeviceName = true
                    }
            alertInvalidDeviceNameDialog = alertDialogBuilder.create()
            alertInvalidDeviceNameDialog!!.show()
        }
    }

    fun hideLoading() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        } else {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setCanceledOnTouchOutside(false)
        }
        isLoading = false
    }

    fun showLoading(text: String) {

        isLoading = true
        if (progressDialog == null) {
            return
        }
        progressDialog!!.setMessage(text)
        if (!progressDialog!!.isShowing) {
            progressDialog!!.show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == RC_LOCATION_PERM) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (autoConnectService.deviceState == BluetoothConnectionObservable.State.DISCONNECTED) {
                    autoConnectService.requestDeviceSearch(false, false)
                }
            } else {
                Snackbar.make(findViewById(R.id.main_view), R.string.location_request_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") { ActivityCompat.requestPermissions(this@MainActivity, LOC_PERMS, RC_LOCATION_PERM) }
                        .show()
            }
        }
    }

    /**
     * Request permission with custom message dialog
     *
     * @param activity
     * @param permissions
     * @param requestCode
     * @param needDescription
     * @param message
     */
    override fun requestPermission(activity: Activity, permissions: Array<String>, requestCode: Int,
                                   needDescription: Boolean, message: String?) {
        if (isFinishing) {
            return
        }

        if (needDescription) {
            AnimatedDialogBuilder(activity)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setCancelable(false)
                    .setTitle("Request Permissions")
                    .setMessage(message ?: getString(R.string.request_permission_message_default))
                    .setNegativeButton("", null)
                    .setPositiveButton("OK") { dialog, which -> ActivityCompat.requestPermissions(activity, permissions, requestCode) }.show()
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }
    }

    /**
     * Onclick method for Settings button
     *
     * @param view
     */
    fun settingsClicked(view: View?) {

        val intent = Intent(this, com.pitstop.ui.settings.SettingsActivity::class.java)
        startActivity(intent)

        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

        final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

        IntentProxyObject proxyObject = new IntentProxyObject();

        application = ((GlobalApplication) getApplication());
        if (application.getCurrentUser() == null) {
            networkHelper.getUser(application.getCurrentUserId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        application.setCurrentUser(com.pitstop.models.User.jsonToUserObject(response));
                        startActivityForResult(intent, RC_SETTINGS);
                    } else {
                        Log.e(TAG, "Get user error: " + requestError.getMessage());
                    }
                }
            });
        } else {
            startActivityForResult(intent, RC_SETTINGS);
        }*/
    }

    /**
     * Onclick method for requesting services
     *
     * @param view if this view is null, we consider the service booking is tentative (first time)
     */
    fun requestMultiService(view: View) {

        val thisInstance = this

        showLoading("Loading...")
        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car) {
                if (!checkDealership(car)) return

                // view is null for request from tutorial
                val intent = Intent(thisInstance, RequestServiceActivity::class.java)
                intent.putExtra(RequestServiceActivity.EXTRA_CAR, car)
                intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, isFirstAppointment)
                isFirstAppointment = false
                startActivityForResult(intent, RC_REQUEST_SERVICE)
                hideLoading()
            }

            override fun onNoCarSet() {
                hideLoading()
                Toast.makeText(thisInstance, "Please add a car", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: RequestError) {
                hideLoading()
                Toast.makeText(thisInstance, "Error loading car", Toast.LENGTH_LONG).show()
            }
        })

    }

    fun myAppointments() {
        mixpanelHelper!!.trackButtonTapped("My Appointments", "Dashboard")


        val thisInstance = this

        showLoading("Loading...")

        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car) {
                if (!checkDealership(car)) return
                val intent = Intent(thisInstance, MyAppointmentActivity::class.java)
                intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA, false)
                intent.putExtra(MainActivity.CAR_EXTRA, car)
                startActivity(intent)
                hideLoading()
            }

            override fun onNoCarSet() {
                hideLoading()
                Toast.makeText(thisInstance, "Please add a car", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: RequestError) {
                hideLoading()
                Toast.makeText(thisInstance, "Error loading car", Toast.LENGTH_LONG).show()
            }
        })

    }

    fun myTrips() {
        val thisInstance = this

        showLoading("Loading...")
        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car) {
                val intent = Intent(thisInstance, MyTripsActivity::class.java)
                intent.putExtra(MainActivity.CAR_EXTRA, car)
                startActivity(intent)
                hideLoading()
            }

            override fun onNoCarSet() {
                hideLoading()
                Toast.makeText(thisInstance, "Please add a car", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: RequestError) {
                hideLoading()
                Toast.makeText(thisInstance, "Error loading car", Toast.LENGTH_LONG).show()
            }
        })

    }

    override fun prepareAndStartTutorialSequence() {

    }

    override fun startDisplayIssueActivity(issues: List<CarIssue>, position: Int) {
        val intent = Intent(this, IssueDetailsActivity::class.java)
        val carIssueArrayList = ArrayList(issues)
        intent.putParcelableArrayListExtra(CAR_ISSUE_KEY, carIssueArrayList)
        intent.putExtra(CAR_ISSUE_POSITION, position)
        intent.putExtra(IssueDetailsActivity.SOURCE, CURRENT_ISSUE_SOURCE)

        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car) {
                intent.putExtra(CAR_KEY, car)
                startActivity(intent)
            }

            override fun onNoCarSet() {
                // this should never happen because this function only gets called when service clicked and if user doesnt have a car he cant have services for it
            }

            override fun onError(error: RequestError) {
                Toast.makeText(applicationContext, "An error occured. Please try again", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        if (serviceIsBound) {
            unbindService(serviceConnection)
            serviceIsBound = false
        }
        super.onDestroy()
    }

    private fun checkDealership(car: Car?): Boolean {
        if (car == null) {
            return false
        }

        if (car.dealership == null) {
            Snackbar.make(rootView!!, "Please select your dealership first!", Snackbar.LENGTH_LONG)
                    .setAction("Select") { view -> selectDealershipForDashboardCar(car) }
                    .show()
            return false
        }
        return true
    }

    private fun selectDealershipForDashboardCar(car: Car?) {
        val dealerships = shopLocalStore!!.allDealerships
        val shops = ArrayList<String>()
        val shopIds = ArrayList<String>()

        showLoading("Getting shop information..")
        networkHelper!!.getShops { response, requestError ->
            hideLoading()
            if (requestError == null) {
                try {
                    val dealers = Dealership.createDealershipList(response)
                    shopLocalStore!!.deleteAllDealerships()
                    shopLocalStore!!.storeDealerships(dealers)
                    for (dealership in dealers) {
                        shops.add(dealership.name)
                        shopIds.add(dealership.id.toString())
                    }
                    showSelectDealershipDialog(car, shops.toTypedArray(),
                            shopIds.toTypedArray())
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "An error occurred, please try again", Toast.LENGTH_SHORT)
                            .show()
                }

            } else {
                Log.e(TAG, "Get shops: " + requestError.message)
                Toast.makeText(this@MainActivity, "An error occurred, please try again", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun showSelectDealershipDialog(car: Car?, shops: Array<String>, shopIds: Array<String>) {
        val pickedPosition = intArrayOf(-1)

        val dialog = AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setSingleChoiceItems(shops, -1) { dialogInterface, which -> pickedPosition[0] = which }
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CONFIRM", null)
                .create()

        dialog.setOnShowListener { dialogInterface ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { view ->
                if (pickedPosition[0] == -1) {
                    Toast.makeText(this@MainActivity, "Please select a dealership", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val shopId = Integer.parseInt(shopIds[pickedPosition[0]])

                try {
                    mixpanelHelper!!.trackCustom("Button Tapped",
                            JSONObject(String.format("{'Button':'Select Dealership', 'View':'%s', 'Make':'%s', 'Model':'%s'}",
                                    MixpanelHelper.SETTINGS_VIEW, car!!.make, car.model)))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                networkHelper!!.updateCarShop(car!!.id, shopId) { response, requestError ->
                    dialog.dismiss()
                    if (requestError == null) {
                        Log.i(TAG, "Dealership updated - carId: " + car.id + ", dealerId: " + shopId)
                        // Update car in local database
                        car.shopId = shopId
                        car.dealership = shopLocalStore!!.getDealership(shopId)
                        carLocalStore!!.updateCar(car)

                        val properties = User.getCurrentUser().properties
                        properties.put("Email", shopLocalStore!!.getDealership(shopId)!!.email)
                        User.getCurrentUser().addProperties(properties)

                        Toast.makeText(this@MainActivity, "Car dealership updated", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.e(TAG, "Dealership updateCarIssue error: " + requestError.error)
                        Toast.makeText(this@MainActivity, "There was an error, please try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun logAuthInfo() {
        LogUtils.LOGD(TAG, "RefreshToken: " + application!!.refreshToken)
        LogUtils.LOGD(TAG, "AccessToken: " + application!!.accessToken)
    }

    override fun onDeviceNeedsOverwrite() {

        LogUtils.LOGD(TAG, "onDeviceNeedsOverwrite(), BuildConfig.DEBUG?" + BuildConfig.DEBUG
                + " ignoreMissingDeviceName?" + ignoreMissingDeviceName)

        /*Check for device name being broken and create pop-up to set the id on DEBUG only(for now)
        **For 215 device only*/

        if ((BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_BETA)
                && !ignoreMissingDeviceName && allowDeviceOverwrite) {

            displayGetScannerIdDialog()
        }
    }

    override fun onSearchingForDevice() {
        Log.d(TAG, "onSearchingForDevice()")
        displayDeviceState(BluetoothConnectionObservable.State.SEARCHING)
    }

    override fun onDeviceReady(device: ReadyDevice) {
        displayDeviceState(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)
    }

    override fun onDeviceDisconnected() {
        Log.d(TAG, "onDeviceDisconnected()")
        displayDeviceState(BluetoothConnectionObservable.State.DISCONNECTED)
    }

    override fun onDeviceVerifying() {
        Log.d(TAG, "onDeviceVerifying()")
        displayDeviceState(BluetoothConnectionObservable.State.VERIFYING)
    }

    override fun onDeviceSyncing() {

    }

    override fun openCurrentServices() {
        tabFragmentManager!!.openServices()
    }

    override fun openAppointments() {
        myAppointments()
    }

    override fun openScanTab() {
        tabFragmentManager!!.openScanTab()
    }

    companion object {

        public val TAG = MainActivity::class.java.simpleName
       final  val CURRENT_ISSUE_SOURCE = "currentService"
        const val CAR_ISSUE_KEY = "carIssues"
        const val CAR_ISSUE_POSITION = "issuePosition"
        const val CAR_KEY = "car"

        const val RC_ADD_CAR = 50
        const val RC_SCAN_CAR = 51
        const val RC_SETTINGS = 52
        const val RC_ADD_CUSTOM_ISSUE = 54
        const val RC_REQUEST_SERVICE = 55
        val FROM_NOTIF = "from_notfftfttfttf"

        val RC_ENABLE_BT = 102
        val RESULT_OK = 60

        const val CAR_EXTRA = "car"
        const val CAR_ISSUE_EXTRA = "car_issue"
        const val CAR_LIST_EXTRA = "car_list"
        const val REFRESH_FROM_SERVER = "_server"
        const val FROM_ACTIVITY = "from_activity"
        const val REMOVE_TUTORIAL_EXTRA = "remove_tutorial"

        const val RC_LOCATION_PERM = 101
        val LOC_PERMS = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        //Primarily for development reasons, set inside BluetoothAutoConnectService
        var allowDeviceOverwrite = false
    }
}
