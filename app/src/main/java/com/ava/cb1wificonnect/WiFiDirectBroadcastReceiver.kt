package com.ava.cb1wificonnect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.BUSY
import android.net.wifi.p2p.WifiP2pManager.CONNECTION_REQUEST_DEFER_TO_SERVICE
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class WiFiDirectBroadcastReceiver(
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
    advertiserMode: Boolean
) : BroadcastReceiver() {

    private val peers = mutableListOf<WifiP2pDevice>()
    private var connectionTryStarted = false
    private var socketStarted = false
    private val port = 13133
    private var ssidSent = false
    private var passwordSent = false

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when(intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Determine if Wi-Fi Direct mode is enabled or not
                    Log.d(TAG, "WIFI State Changed")
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    Log.d(TAG, "WIFI State Changed to: $state")
                    var isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "isWifiP2pEnabled: $isWifiP2pEnabled")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                    // The peer list has changed!
                    // We should probably do something about that.
                    Log.d(TAG, "P2P peers changed")
                    manager.requestPeers(channel, peerListListener)
                    Log.d(TAG, "P2P peers changed: requestPeers done")

                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                    // Connection state changed! We should probably do something about
                    // that.

                    Log.d(TAG, "onReceive: connection changed")
                    if (manager != null) {
                        Log.d(TAG, "onReceive: manager available")
                        @Suppress("DEPRECATION") val mNetworkInfo: NetworkInfo? =
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (mNetworkInfo != null) {
                            Log.d(TAG, "onReceive: NetworkInfo available")
                            @Suppress("DEPRECATION")
                            if (mNetworkInfo.isConnected) {
                                Log.d(TAG, "onReceive: requesting Info")
                                manager.requestConnectionInfo(channel, connectionListener)
                            } else {
                                Log.d(TAG, "onReceive: connection ended. device disconnected ")
                            }
                        }
                    }

                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    Log.d(TAG, "P2P this device changed")
                }
                WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                    Log.d(TAG, "P2P Discovery Changed")
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 10000)
                        if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                            // Wifi P2P discovery started.
                            Log.d(TAG, "Wifi P2P discovery started.")
                        } else {
                            // Wifi P2P discovery stopped.
                            // Do what you want to do when discovery stopped
                            Log.d(TAG, "Wifi P2P discovery stopped.")
                            Log.d(TAG, "Trying to Restart WiFi P2P discovery...")
                            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

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
                            Log.d(TAG, "Restart WiFi P2P discovery command sent...")

                        }

                }
            }
        }
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->

        Log.d(TAG, "Peer Listener Active")
        val refreshedPeers = peerList.deviceList
        Log.d(TAG, "Refreshed Peer-List")
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            Log.d(TAG, "List possible Peers")
            for (peer in peers) {
                Log.d(TAG, "peer name ${peer.deviceName}")
                Log.d(TAG, "peer address ${peer.deviceAddress}")

                if (peer.deviceName == "AVA RX2" ||
                    peer.deviceName == "AVA RX2 manual" ||
                    peer.deviceName == "AVA-RM-RX2") {

                    Log.d(TAG, "AVA RX2 discovered")
                    if (advertiserMode && !connectionTryStarted) {
                        connect(peer)
                    }
                }
            }
        }

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(peer : WifiP2pDevice) {
        Log.d(TAG, "Trying to setup Connection")
        connectionTryStarted = true
        Log.d(TAG, "block other connection attempts")
        val config = WifiP2pConfig().apply {
            deviceAddress = peer.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Connection Succeeded")

            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Connection Failed $reason")
                if (reason == CONNECTION_REQUEST_DEFER_TO_SERVICE) {
                    // REASON: Defer the decision back to the Wi-Fi service
                    // (which will display a dialog to the user)

                    Log.d(TAG, "User must first allow the connection")
                }

                if (reason == BUSY) {
                    // Indicates that the operation failed because the framework is busy
                    // and unable to service the request
                    Log.d(TAG, "Service is busy, retry later")
                }
                Log.d(TAG, "Re-allow Connections")
                connectionTryStarted = false
                Log.d(TAG, "Connection try started set to false")
            }
        })
    }

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->

        Log.d(TAG, "ConnectionInfoListener started");
        // String from WifiP2pInfo struct
        val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            Log.d(TAG, "Is Group Owner");
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.

            if (!socketStarted) {
                Log.d(TAG, "Starting a Network Thread")
                socketStarted = true
                Log.d(TAG, "Block other Socket Connection Attempts")

                val thread = Thread {
                    try {
                        Log.d(TAG, "Running Socket on port: $port");
                        var serverSocket = ServerSocket(port);
                        var socket = serverSocket.accept()
                        Log.d(TAG, "Socket Start Procedure executed")

                        var output = PrintWriter(socket.getOutputStream())
                        var input = BufferedReader(InputStreamReader(socket.getInputStream()))

                        var ssid = ""
                        var password = ""

                        Log.d(TAG, "Socket isConnected = ${socket.isConnected}")

                        while (!passwordSent) {
                            Log.d(TAG, "Waiting for messages")
                            try {
                                var message = input.readLine();
                                if (message != null) {
                                    Log.d(TAG, "Received Message $message")
                                    if (!ssidSent) {
                                        ssid = message
                                        ssidSent = true
                                        Log.d(TAG, "SSID: $ssid")
                                    } else if (!passwordSent) {
                                        password = message
                                        passwordSent = true
                                        Log.d(TAG, "Password: $password")
                                    }
                                } else {
                                    Log.d(TAG, "no message received")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (passwordSent && ssidSent) {
                            val cmds = arrayOf("cmd wifi connect-network \"$ssid\" wpa2 \"$password\"")
                            val p = Runtime.getRuntime().exec("su")
                            val os = DataOutputStream(p.outputStream)
                            for (tmpCmd in cmds) {
                                os.writeBytes(tmpCmd + "\n")
                            }
                            os.writeBytes("exit\n")
                            os.flush()

                            Log.d(TAG, "WIFI Connection here? .")
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                thread.start()
            } else {
                Log.d(TAG, "Socket already started, not starting another connection for now")
            }
        }
    }


}