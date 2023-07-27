package com.ava.cb1wificonnect

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


const val TAG = "CB1_WIFI_ADVERTISER"
const val APP_ID = "cb1_wifi_advertiser"
private val intentFilter = IntentFilter()
lateinit var channel: WifiP2pManager.Channel
lateinit var manager: WifiP2pManager
const val IS_ADVERTISER: Boolean = true
public var IS_WIFI_CONNECTED = false;
public var IS_ETHERNET_CONNECTED = false;

class MainActivity : AppCompatActivity() {

    private lateinit var receiver: WiFiDirectBroadcastReceiver
    private val peers = mutableListOf<WifiP2pDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // UI Setup
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Prepared View")
        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        // Indicates this device's discovery state has changed
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)

        Log.d(TAG, "Initializing Channel")
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)


        Log.d(TAG, "Starting Permission check")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Permissions ACCESS_FINE_LOCATION not available")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions..............................
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Permissions NEARBY_WIFI_DEVICES not available")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return
        }
        Log.d(TAG, "Starting Peer Discovery.")
        checkConnectivity()
        if (!IS_WIFI_CONNECTED && !IS_ETHERNET_CONNECTED) {
            discoverPeers()
        } else (
            checkForLostConnectivity()
        )
    }

    public override fun onResume() {

        Log.d(TAG, "Starting OnResume")
        super.onResume()
        checkConnectivity()
        if (!IS_WIFI_CONNECTED && !IS_ETHERNET_CONNECTED) {
            startReceiver()
        }
        Log.d(TAG, "Starting OnResume Successful")
    }

    public override fun onPause() {
        Log.d(TAG, "Starting OnPause")
        super.onPause()
        unregisterReceiver(receiver)
        Log.d(TAG, "Starting OnPause Successful")
    }

    @SuppressLint("MissingPermission")
    fun checkConnectivity() {
        val connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION") val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        @Suppress("DEPRECATION") val mEthernet = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)


        @Suppress("DEPRECATION")
        if (mWifi!!.isConnected) {
            // Do whatever
            Log.d(TAG, "WiFi is Connected... no action required")
            IS_WIFI_CONNECTED = true
        } else if (mEthernet!!.isConnected) {
            Log.d(TAG, "Ethernet is Connected... no action required")
            IS_ETHERNET_CONNECTED = true
        } else {
            Log.d(TAG, "WiFi & Ethernet is NOT Connected... ")
            IS_WIFI_CONNECTED = false
            IS_ETHERNET_CONNECTED = false
        }
    }

    fun checkForLostConnectivity(){
        checkConnectivity()
        if (!IS_WIFI_CONNECTED && !IS_ETHERNET_CONNECTED) {
            Log.d(TAG, "Start Receiver again")
            startReceiver()
        } else {
            Thread.sleep(60000)
            checkForLostConnectivity()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        manager.discoverPeers(
            channel,
            object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    // Code for when the discovery initiation is successful goes here.
                    // No services have actually been discovered yet, so this method
                    // can often be left blank. Code for peer discovery goes in the
                    // onReceive method, detailed below.
                    Log.d(TAG, "Started Peer Discovery Successful")
                }

                override fun onFailure(reasonCode: Int) {
                    // Code for when the discovery initiation fails goes here.
                    // Alert the user that something went wrong.
                    Log.d(TAG, "Failed Peer Discovery Initiation")
                }
            })
    }

    fun startReceiver() {
        receiver = WiFiDirectBroadcastReceiver(manager, channel, IS_ADVERTISER, this)
        registerReceiver(receiver, intentFilter)
        Log.d(TAG, "Discover Peers...")
    }
}