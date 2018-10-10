package com.Difetis.IntervalTimer;

/**
 * Created by kerfall on 23/04/2018.
 */

import android.app.Service;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Iterator;

public class GPSTracker extends Service implements LocationListener/*, GpsStatus.Listener*/ {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS location
    boolean canGetLocation = false;

    public boolean isGPSUpdated() {
        return _isGPSUpdated;
    }

    public void setGPSUpdated(boolean isGPSUpdated) {
        _isGPSUpdated = isGPSUpdated;
    }

    // flag for GPS update
    boolean _isGPSUpdated = false;

    Location oldLocation; // old known location
    double latitude; // latitude
    double longitude; // longitude
    double altitude; // altitude
    double accuracy; // accuracy
    double speed; // speed
    double _runningLength; // running length


    protected GpsStatus gpsStatus;
    protected int status;
    private int nbSat;


    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 seconds

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;

        // start gps request immediately
        startLocationRequest();
    }

    public void startLocationRequest() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network or gps provider is enabled

            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network enabled", "Network enabled");

                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {

                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");

                }
            }

        } catch (SecurityException ex) {

            ex.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {

        // return longitude
        return longitude;
    }

    /**
     * Function to get Altitude
     */
    public double getAltitude() {

        // return altitude
        return altitude;
    }

    /**
     * Function to get Accuracy
     */
    public double getAccuracy() {

        // return accuracy
        return accuracy;
    }

    /**
     * Function to get speed
     */
    public double getSpeed() {

        // return speed
        return speed;
    }

    /**
     * Function to get number of satellites
     */
    public int getSatellites() {

        // return satellites
        return nbSat;
    }

    /**
     * Function to get running Length
     */
    public double getLength() {

        return _runningLength;
    }

    /**
     * Function to set running Length
     */
    public void setLength(double runningLength) {

        _runningLength = runningLength;
        oldLocation = null;
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }


    /**
     * Function to check if Gps is ready
     *
     * @return boolean
     */
    public boolean isGPSReady() {

        if (canGetLocation() && (isGPSEnabled || isNetworkEnabled) && (getLatitude() != 0 || getLongitude() != 0) ) {

            return true;
        } else {
            return false;
        }
    }


    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location == null) return;

        // only take gps location data
        if(!location.getProvider().equals(LocationManager.GPS_PROVIDER)){

            return;
        }

        double dLongueur = 0;
        double calculatedSpeed = 0;
        if(oldLocation != null){
            dLongueur = location.distanceTo(oldLocation);
        }

        // only positive length
        if(dLongueur > 0){
            _runningLength +=  Math.abs(dLongueur) ;
        }

        // calculate instant speed in case location doesn't provide it
        if (oldLocation != null) {
            double elapsedTime = location.getTime() - oldLocation.getTime();

            if(elapsedTime > 0)
            {
                calculatedSpeed = (double) (3600 * oldLocation.distanceTo(location)) / elapsedTime;
            }
        }

        speed = /*location.hasSpeed() ? location.getSpeed() :*/ calculatedSpeed;

        _isGPSUpdated = true;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();
        altitude = location.getAltitude();

        // save location for next update
        oldLocation = location;


    }

/*
    public void onGpsStatusChanged(int event) {

        try{

            gpsStatus = locationManager.getGpsStatus(gpsStatus);
            String csMessage = null;

            status = event;

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                // Do Something with mStatus info
                csMessage = "GPS démarré";
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                // Do Something with mStatus info
                csMessage = "GPS arreté";
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do Something with mStatus info
                int iTime = gpsStatus.getTimeToFirstFix();

                csMessage = String.format("FirstFix en %d ms", iTime);
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                // Do Something with mStatus info

                // on remet a jour le nombre de sattelites
                nbSat = 0;

                final Iterator<GpsSatellite> it = gpsStatus.getSatellites().iterator();
                while( it.hasNext() ) {
                    it.next();
                    nbSat += 1;
                };


                break;

        }

        } catch (SecurityException ex) {

            ex.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void onProviderDisabled(String provider) {
        if (provider != null) {

            String msg = String.format(
                    getResources().getString(R.string.provider_disabled), provider);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider != null) {
            String msg = String.format(
                    getResources().getString(R.string.provider_enabled), provider);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (provider != null) {
            String newStatus = "";
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    newStatus = "OUT_OF_SERVICE";
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    newStatus = "TEMPORARILY_UNAVAILABLE";
                    break;
                case LocationProvider.AVAILABLE:
                    newStatus = "AVAILABLE";
                    break;
            }
            String msg = String.format(
                    getResources().getString(R.string.provider_new_status), provider,
                    newStatus);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}