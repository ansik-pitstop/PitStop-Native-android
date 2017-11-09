package com.pitstop.ui.main_activity

import android.app.Activity
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*

import com.parse.ParseACL
import com.parse.ParseInstallation
import com.pitstop.BuildConfig
import com.pitstop.R
import com.pitstop.adapters.CarsAdapter
import com.pitstop.application.GlobalApplication
import com.pitstop.observer.AutoConnectServiceBindingObserver
import com.pitstop.bluetooth.BluetoothAutoConnectService
import com.pitstop.observer.BluetoothAutoConnectServiceObservable
import com.pitstop.database.LocalCarStorage
import com.pitstop.database.LocalScannerStorage
import com.pitstop.database.LocalShopStorage
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerTempNetworkComponent
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase
import com.pitstop.interactors.get.GetCarsByUserIdUseCase
import com.pitstop.interactors.get.GetCurrentCarDealershipUseCase
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

import io.smooch.core.Smooch
import io.smooch.core.User
import io.smooch.ui.ConversationActivity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Created by David on 6/8/2016.
 */
class MainActivity : IBluetoothServiceActivity(), MainActivityCallback, Device215BreakingObserver, BluetoothConnectionObserver, TabSwitcher, MainView, BluetoothAutoConnectServiceObservable {


    private var presenter: MainActivityPresenter? = null
    private var application: GlobalApplication? = null
    private var serviceIsBound: Boolean = false
    private var isFirstAppointment: Boolean = false
    private var serviceIntent: Intent? = null
    private var appointmentsButton: View? = null
    private var requestAppointmentButton: View? = null
    private var drawerToggle : ActionBarDrawerToggle? = null
    private var carRecyclerView:RecyclerView? = null
    private var carsAdapter:CarsAdapter?  = null
    private var addCarBtn:View? = null
    private var messageBtn: View?  = null
    private var callBtn: View?  = null
    private var findDirectionsBtn:View? = null
    private var contactView : View? = null
    private var appointmentsView: View?  = null
    private var progressView : View? = null
    private var textAboveCars: LinearLayout? = null
    private var drawerRefreshLayout:SwipeRefreshLayout? = null
    private var drawerLinearLayout: LinearLayout? = null
    private var errorLoadingCars: TextView?  = null
    private var carsTapDescription : TextView? = null
    private var serviceObservers: ArrayList<AutoConnectServiceBindingObserver> = ArrayList();
    protected var serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "connecting: onServiceConnection")
            // cast the IBinder and get MyService instance

            autoConnectService = (service as BluetoothAutoConnectService.BluetoothBinder).service
            autoConnectService.subscribe(this@MainActivity)
            autoConnectService.requestDeviceSearch(false, false)
            displayDeviceState(autoConnectService.deviceState)
            notifyServiceBinded(autoConnectService)
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

        appointmentsView = findViewById(R.id.appointments_view_drawer)
        contactView = findViewById(R.id.contact_view_drawer)
        progressView = findViewById(R.id.progress_drawer)
        textAboveCars = findViewById(R.id.drawer_text_above_cars)


        if (this.presenter == null){
            this.presenter = MainActivityPresenter(useCaseComponent!!, mixpanelHelper!!)
        }
        presenter?.subscribe(this)
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

            drawerToggle = ActionBarDrawerToggle(this, drawerLayout,R.string.app_name, R.string.app_name );
            drawerLayout!!.setDrawerListener(drawerToggle)
            setContentView(drawerLayout)
        }
        else{
            drawerToggle = ActionBarDrawerToggle(this, mDrawerLayout,R.string.app_name, R.string.app_name )
            drawerToggle?.isDrawerIndicatorEnabled = true
            mDrawerLayout.setDrawerListener(drawerToggle)


        }
        setUpDrawer()
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
       supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }

    private fun setUpDrawer() {
        Log.d(TAG, "setUpDrawer")
        this.carRecyclerView = findViewById(R.id.car_recycler_view)
        carRecyclerView?.layoutManager = LinearLayoutManager(this)
        carsAdapter = CarsAdapter(this, presenter?.dealershipList,presenter?.carList )
        carRecyclerView?.adapter = carsAdapter
        drawerRefreshLayout = findViewById(R.id.drawer_layout_garage)
        drawerRefreshLayout?.setOnRefreshListener { presenter?.onRefresh() }
        drawerLinearLayout = findViewById(R.id.main_drawer_linear_layout)
        carsTapDescription = findViewById(R.id.my_vehicles_description_garage);
        errorLoadingCars = findViewById(R.id.error_loading_cars)
        this.addCarBtn = findViewById(R.id.add_car_garage)
        addCarBtn?.setOnClickListener { presenter?.onAddCarClicked() }
        this.appointmentsButton = findViewById(R.id.my_appointments_garage)
        appointmentsButton?.setOnClickListener {
            showLoading("Loading...")
            this.presenter?.onMyAppointmentsClicked() }
        this.requestAppointmentButton = findViewById(R.id.request_service_garage)
        requestAppointmentButton?.setOnClickListener {
            showLoading("Loading...")
            this.presenter?.onRequestServiceClicked()
        }
        this.messageBtn = findViewById(R.id.message_my_garage)
        messageBtn?.setOnClickListener {
            showLoading("Loading...")
            presenter?.onMessageClicked()
        }
        this.callBtn = findViewById(R.id.call_garage)
        callBtn?.setOnClickListener {
            presenter?.onCallClicked()
        }
        drawerToggle?.syncState()

        this.findDirectionsBtn = findViewById(R.id.find_direction_garage)
        findDirectionsBtn?.setOnClickListener { presenter?.onFindDirectionsClicked() }
        presenter?.onUpdateNeeded()
    }

    override fun subscribe(autoConnectBinderObserver: AutoConnectServiceBindingObserver) {
        serviceObservers.add(autoConnectBinderObserver);
    }

    fun notifyServiceBinded(bluetoothAutoConnectService: BluetoothAutoConnectService){
        for (listener in serviceObservers){
            listener.onServiceBinded(bluetoothAutoConnectService)
        }
    }

    override fun callDealership(dealership: Dealership?) {
        Log.d(TAG, "callDealership()")
        val intent = Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + dealership?.getPhone()))
        this.startActivity(intent)
    }

    override fun openDealershipDirections(dealership: Dealership?) {
        Log.d(TAG, "openDealershipDirections()")
        val uri = String.format(Locale.ENGLISH,
                "http://maps.google.com/maps?daddr=%s",
                dealership?.address)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        this.startActivityForResult(intent, 0)
    }

    override fun openAddCarActivity() {
        Log.d(TAG, "onAddCarClicked()")
        val intent = Intent(this, AddCarActivity::class.java)
        startActivityForResult(intent, MainActivity.RC_ADD_CAR)
    }

    override fun noCarsView() {

    }

    override fun showCars(carList: MutableList<Car>) {
        Log.d(TAG, "showCars()")
        carsAdapter?.notifyDataSetChanged()
        if (carList.size == 0) {
            noCarsView()
        }
    }

    override fun onCarClicked(car: Car) {
        Log.d(TAG, "onCarClicked()")
        makeCarCurrent(car)
    }

    fun makeCarCurrent(car: Car){
        Log.d(TAG, "make car Current " + car.year + " " + car.make  + " " + car.model)
        presenter?.makeCarCurrent(car)
    }

    override fun notifyCarDataChanged() {
        carsAdapter?.notifyDataSetChanged()
    }


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
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, if (darkTheme) R.color.black else R.color.primary_dark)
        }
    }

    private fun loadDealerDesign(dealership: Dealership?) {
        //Update tab design to the current dealerships custom design if applicable
        if (dealership != null) {
            if (BuildConfig.DEBUG && (dealership.id == 4 || dealership.id == 18)) {

                bindMercedesDealerUI()
            } else if (!BuildConfig.DEBUG && dealership.id == 14) {
                bindMercedesDealerUI()
            } else {
                bindDefaultDealerUI()
            }
            hideLoading()
        }
    }

    fun getBluetoothConnectService(): BluetoothAutoConnectService {
        return autoConnectService
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
        super.onResume();

        Log.d(TAG, "onResume, serviceBound? "+serviceIsBound);

        checkPermissions();
        if (!serviceIsBound){
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            serviceIsBound = true;
        }
        if (autoConnectService != null){
            displayDeviceState(autoConnectService.getDeviceState());
            autoConnectService.subscribe(this);
            autoConnectService.requestDeviceSearch(false, false);
        }

        useCaseComponent?.getGetCurrentDealershipUseCase()!!.execute(object: GetCurrentCarDealershipUseCase.Callback{
            override fun onGotDealership(dealership:Dealership) {
                loadDealerDesign(dealership);
            }

            override fun onNoCarExists() {
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent : Intent?) {
        Log.i(TAG, "onActivityResult")

        if ((intent!= null) && requestCode == RC_ADD_CAR) {

            if (resultCode == AddCarActivity.ADD_CAR_SUCCESS_HAS_DEALER || resultCode == AddCarActivity.ADD_CAR_SUCCESS_NO_DEALER) {

                useCaseComponent?.getUserCarUseCase()!!.execute(object: GetUserCarUseCase.Callback{
                    override fun onCarRetrieved(car: Car?, dealership: Dealership?) {
                        updateSmoochUser(application?.currentUser, car, dealership)
                    }

                    override fun onNoCarSet() {
                    }

                    override fun onError(error: RequestError?) {
                    }

                })
                useCaseComponent?.checkFirstCarAddedUseCase()!!
                        .execute(object: CheckFirstCarAddedUseCase.Callback{

                            override fun onFirstCarAddedChecked(added: Boolean) {
                                if (!added){
                                    sendSignedUpSmoochMessage(application!!.getCurrentUser())
                                    prepareAndStartTutorialSequence()

                                    useCaseComponent?.setFirstCarAddedUseCase()!!
                                            .execute(true, object : SetFirstCarAddedUseCase.Callback {
                                                override fun onFirstCarAddedSet() {
                                                    //Variable has been set
                                                }
                                                override fun onError(error: RequestError) {
                                                    //Networking error logic here
                                                }
                                            })
                                } }
                            override fun onError(error: RequestError?) {
                                //error logic here
                            }})

            } else {
                mixpanelHelper?.trackButtonTapped("Cancel in Add Car", "Add Car");
            }
        }
        else{
            super.onActivityResult(requestCode,resultCode,intent);
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

    private fun updateSmoochUser(user: com.pitstop.models.User?, car: Car?, dealership: Dealership?) {
        if (car == null || user == null) return

        val customProperties: HashMap<String, Any?> = HashMap()
        customProperties.put("VIN", car.vin)
        Log.d(TAG, car.vin)
        customProperties.put("Car Make", car.make)
        Log.d(TAG, car.make)
        customProperties.put("Car Model", car.model)
        Log.d(TAG, car.model)
        customProperties.put("Car Year", car.year)
        Log.d(TAG, car.year.toString())

        //Add custom user properties
        if (dealership != null) {
            customProperties.put("Email", dealership.email)
            Log.d(TAG, dealership.email)
        }

        if (user != null) {
            customProperties.put("Phone", user.phone)
            User.getCurrentUser().firstName = user.firstName
            User.getCurrentUser().email = user.email
        }

        User.getCurrentUser().addProperties(customProperties)
    }


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
        if (drawerToggle?.onOptionsItemSelected(item)!!)
            return true;
        when (item.itemId) {
            R.id.action_settings -> {
                settingsClicked(null)
                return true;}
            R.id.action_request_service-> {
                requestMultiService(null);
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

    override fun hideLoading() {
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
    fun requestMultiService(view: View?) {

        val thisInstance = this
        val intent = Intent(thisInstance, RequestServiceActivity::class.java)
        intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, isFirstAppointment)
        isFirstAppointment = false
        startActivity(intent)

    }

    fun myAppointments() {
        mixpanelHelper!!.trackButtonTapped("My Appointments", "Dashboard")
        val thisInstance = this
        showLoading("Loading...")
        useCaseComponent!!.userCarUseCase.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car?, dealership: Dealership?) {
                if (dealership == null){
                    Toast.makeText(thisInstance,"Select a dealership first", Toast.LENGTH_LONG).show();
                    return;
                }
                //if (!checkDealership(car)) return;
                val intent:Intent = Intent(thisInstance, MyAppointmentActivity::class.java)
                intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false)
                intent.putExtra(MainActivity.CAR_EXTRA, car);
                startActivity(intent);
                hideLoading();
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

   fun myTrips(){
        val thisInstance:MainActivity = this

        showLoading("Loading...");
        useCaseComponent?.getUserCarUseCase()!!.execute(object:  GetUserCarUseCase.Callback{
            override fun onCarRetrieved(car: Car?, dealership: Dealership?) {
                val intent: Intent = Intent(thisInstance, MyTripsActivity::class.java)
                intent.putExtra(MainActivity.CAR_EXTRA, car);
                startActivity(intent);
                hideLoading();
            }

            override fun onNoCarSet() {
                hideLoading();
                Toast.makeText(thisInstance,"Please add a car",Toast.LENGTH_LONG).show();
            }

            override fun onError(error: RequestError?) {
                hideLoading();
                Toast.makeText(thisInstance,"Error loading car",Toast.LENGTH_LONG).show();
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
        useCaseComponent?.getUserCarUseCase()!!.execute(object : GetUserCarUseCase.Callback {
            override fun onCarRetrieved(car: Car, dealership: Dealership) {
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
        presenter?.unsubscribe()
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
        return true
    }

    override fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
       /* val pickedPosition = intArrayOf(-1)

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
        dialog.show()*/
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

    override fun isUserNull(): Boolean {
        return((this.applicationContext as GlobalApplication).currentUser == null)
    }

    override fun getUserPhone(): String {
        return (this.getApplicationContext() as GlobalApplication).currentUser.phone
    }

    override fun getUserFirstName(): String {
        return (this.getApplicationContext() as GlobalApplication).currentUser.firstName
    }

    override fun getUserEmail(): String? {
        return (this.getApplicationContext() as GlobalApplication).currentUser.email
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

    override fun openSmooch() {
        ConversationActivity.show(this)
    }

    override fun onDeviceSyncing() {
    }

    override fun openRequestService(car: Car?) {
        val intent = Intent(this, RequestServiceActivity::class.java)
        /*intent.putExtra(RequestServiceActivity.EXTRA_CAR, car)*/
        intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, isFirstAppointment)
        isFirstAppointment = false
        startActivity(intent)
        hideLoading()
    }

    override fun openCurrentServices() {
        tabFragmentManager!!.openServices()
    }

    override fun showCarsLoading() {
        Log.d(TAG, "showCarsLoading()")
        if (!(drawerRefreshLayout?.isRefreshing)!!) {

            val params = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            params.gravity = Gravity.CENTER_VERTICAL
            drawerLinearLayout?.setLayoutParams(params)
            contactView?.visibility = View.GONE
            appointmentsView?.visibility = View.GONE
            carRecyclerView?.visibility = View.GONE
            addCarBtn?.visibility = View.GONE
            textAboveCars?.visibility = View.GONE
            progressView?.visibility = View.VISIBLE
            progressView?.bringToFront()
        }

    }

    override fun hideCarsLoading() {
        Log.d(TAG, "hideCarsLoading()")
        if (!(drawerRefreshLayout?.isRefreshing)!!) {
            val params = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            params.gravity = Gravity.NO_GRAVITY
            drawerLinearLayout?.layoutParams = params
            progressView?.visibility = View.GONE
            errorLoadingCars?.visibility = View.GONE
            contactView?.visibility = View.VISIBLE
            appointmentsView?.visibility = View.VISIBLE
            textAboveCars?.visibility = View.VISIBLE
            carRecyclerView?.visibility = View.VISIBLE
            addCarBtn?.visibility = View.VISIBLE
        }
        else {
            drawerRefreshLayout?.isRefreshing = false;
        }
    }

    override fun openAppointments() {
       myAppointments()
    }

    override fun openAppointments(car :Car) {
        mixpanelHelper!!.trackButtonTapped("My Appointments", "Dashboard")
        showLoading("Loading...")
        if (!checkDealership(car)) return
        val intent = Intent(this, MyAppointmentActivity::class.java)
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA, false)
        intent.putExtra(MainActivity.CAR_EXTRA, car)
        startActivity(intent)
        hideLoading()
    }

    override fun openScanTab() {
        tabFragmentManager!!.openScanTab()
    }

    companion object {

         val TAG = MainActivity::class.java.simpleName
         val CURRENT_ISSUE_SOURCE = "currentService"
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

    override fun errorLoadingCars() {
        carRecyclerView?.visibility = View.GONE
        errorLoadingCars?.visibility = View.VISIBLE
        carsTapDescription?.visibility = View.GONE

    }
}
