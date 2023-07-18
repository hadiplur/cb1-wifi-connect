package com.example.headlessdevicedemo

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class WiFiDirectBroadcastReceiver(
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
    advertiserMode: Boolean
) : BroadcastReceiver() {

    private val peers = mutableListOf<WifiP2pDevice>()

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when(intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Determine if Wi-Fi Direct mode is enabled or not, alert
                    // the Activity.
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    //activity.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                    // The peer list has changed! We should probably do something about
                    // that.
                    manager.requestPeers(channel, peerListListener)
                    Log.d(TAG, "P2P peers changed")

                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    if (manager != null) {
                        @Suppress("DEPRECATION") val mNetworkInfo: NetworkInfo? =
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (mNetworkInfo != null) {
                            @Suppress("DEPRECATION")
                            if (mNetworkInfo.isConnected) {
                                manager.requestConnectionInfo(channel, connectionListener)
                            } else {
                                Log.d(TAG, "onReceive: connection ended. device disconnected ")
                            }
                        }
                    }
                    // Connection state changed! We should probably do something about
                    // that.

                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    /*
                    (activity.supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
                        .apply {
                            updateThisDevice(
                                intent.getParcelableExtra(
                                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice
                            )
                        }*/
                }
            }
        }
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            Log.d(TAG, "List possible Peers")
            for (peer in peers) {
                Log.d(TAG, "peer name ${peer.deviceName}")
                Log.d(TAG, "peer address ${peer.deviceAddress}")

                if (peer.deviceName == "AVA Nano Brain") {

                    Log.d(TAG, "AVA Nano Brain discovered")
                    if (!advertiserMode) {
                        connect(peer)
                    }
                }
            }
            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
            return@PeerListListener
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(peer : WifiP2pDevice) {
        Log.d(TAG, "Trying to setup Connection")

        val config = WifiP2pConfig().apply {
            deviceAddress = peer.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Log.d(TAG, "Connection Succeeded")

            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "Connection Failed")
            }
        })
    }

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->

        // String from WifiP2pInfo struct
        val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
            var serverSocket = ServerSocket(1313);
            Log.d(TAG, "Running Socket on port: 1313");
            var socket = serverSocket.accept()

            // TODO: When Socket is connected
        } else if (info.groupFormed) {
            // The other device acts as the peer (client). In this case,
            // you'll want to create a peer thread that connects
            // to the group owner.
            var socket = Socket()
            socket.bind(null)
            socket.connect(InetSocketAddress(info.groupOwnerAddress, 1313), 500)

        }
    }


}