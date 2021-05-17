package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.content.IntentSender
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {


    val REQUEST_FINE_LOCATION = 1
    val REQUEST_BACKGROUND_LOCATION = 2
    val REQUEST_TURN_DEVICE_LOCATION_ON = 3


    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr = MutableLiveData<String>()
    val selectedPOI = MutableLiveData<PointOfInterest>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    val goBack: LiveData<Boolean>
        get() = _goBack
    private var _goBack = MutableLiveData<Boolean>()

    val checkBackgroundLocationPermission: LiveData<Boolean>
        get() = _checkBackgroundLocationPermission
    private var _checkBackgroundLocationPermission = MutableLiveData<Boolean>()

    val checkForegroundLocationPermission: LiveData<Boolean>
        get() = _checkForegroundLocationPermission
    private var _checkForegroundLocationPermission = MutableLiveData<Boolean>()

    val locationSettingsException: LiveData<ResolvableApiException>
        get() = _locationSettingsException
    private var _locationSettingsException = MutableLiveData<ResolvableApiException>()

    val startGeofence: LiveData<Boolean>
        get() = _startGeofence
    private var _startGeofence = MutableLiveData<Boolean>()

    private var runningQOrLater: Boolean = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    var permissionsGranted: Boolean = false


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null

        _goBack.value = false
        _checkForegroundLocationPermission.value = false
        _checkBackgroundLocationPermission.value = false
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun savePOI() {
        if (selectedPOI.value == null) {
            showSnackBarInt.value = R.string.err_select_location
        } else {
            reminderSelectedLocationStr.value = selectedPOI.value!!.name
            _goBack.value = true
        }
    }

    fun checkLocationPermission() {
        _checkForegroundLocationPermission.value = true
    }

    fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val locationSettingsRequestBuilder =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(app)

        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingsRequestBuilder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                _locationSettingsException.value = exception
            } else {
                showSnackBar.value = app.getString(R.string.location_required_error)
                checkLocationSettings()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if(it.isSuccessful){
                _startGeofence.value = true
            }
        }
    }

    fun handleRequestPermissionResult(requestCode: Int, grantResults: IntArray?) {
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults!!.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showToast.value = app.getString(R.string.permission_foreground_granted)
                _checkForegroundLocationPermission.value = false
                if (runningQOrLater) {
                    _checkBackgroundLocationPermission.value = true
                } else {
                    permissionsGranted = true
                }
            } else {
                permissionsGranted = false
                showToast.value = app.getString(R.string.permission_denied_explanation)
            }
        }

        if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (grantResults!!.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                _checkBackgroundLocationPermission.value = false
                permissionsGranted = true
                showToast.value = app.getString(R.string.permission_background_granted)
            } else {
                permissionsGranted = false
                showToast.value = app.getString(R.string.permission_denied_explanation)
            }
        }

        if(requestCode == REQUEST_TURN_DEVICE_LOCATION_ON){
            checkLocationSettings()
        }
    }
}