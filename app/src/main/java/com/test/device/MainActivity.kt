package com.test.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val permissionArray: Array<String> = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
    private val permissionResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: Map<String, Boolean> ->
            map.forEach {
                Log.d("my_test", "permissionResult[${it.key}:${it.value}]")
            }

            if (map.any { !it.value }) {
                showDialog()
            } else {
                getPairedDevices()
            }
        }
    private val bluetoothResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("my_test", "bluetoothResult: resultCode=${it.resultCode}, data=${it.data}")
            checkPermission()
        }

    private val bluetoothListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            Log.d("my_test", "onServiceConnected")
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.d("my_test", "onServiceConnected")
        }
    }

    // SDK 怎麼知道 device 的相關資訊？並且 sync 到它
    private val myDeviceMacAddress = "C0:26:DF:00:B3:D4" // this is foraGD40b

    private var bluetoothDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        checkBluetooth()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        bluetoothAdapter.cancelDiscovery()
        unregisterReceiver(receiver)
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address
                        Log.d(
                            "my_test",
                            "BroadcastReceiver[deviceName=$deviceName, Address=$deviceHardwareAddress]"
                        )
                        if (deviceHardwareAddress == myDeviceMacAddress) {
                            printDeviceInfo(device)
                            start(device)
                            bluetoothAdapter.cancelDiscovery()
                        }

                    } else {
                        Log.d("my_test", "BroadcastReceiver device is null")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun start(device: BluetoothDevice) {
        //TODO https://developer.android.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices#connect-server
//        bluetoothAdapter.listenUsingRfcommWithServiceRecord()
    }

    private fun registerBleReceiver() {
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    private fun showDialog() {
        AlertDialog.Builder(this)
            .setMessage("permission!")
            .setPositiveButton("ok") { _, _ ->
                checkBluetooth()
            }
            .setNegativeButton("Bye!") { _, _ ->
                finish()
            }
            .show()
    }

    private fun checkBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            checkPermission()
        } else {
            Log.d("my_test", "bluetoothAdapter is NOT enabled")
            val enableBleIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothResult.launch(enableBleIntent)
        }
    }

    private fun checkPermission() {
        if (hasAllPermission()) {
            getPairedDevices()
        } else {
            permissionResult.launch(permissionArray)
        }
    }

    private fun hasAllPermission(): Boolean {
        permissionArray.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.e("my_test", "[$deviceName] $deviceHardwareAddress")
        }

        val isCancel = bluetoothAdapter.cancelDiscovery()
        Log.e("my_test", "isCancelDiscovery ? $isCancel")

        registerBleReceiver()
        val isStart = bluetoothAdapter.startDiscovery()
        Log.e("my_test", "isStartDiscovery ? $isStart")
    }

    @SuppressLint("MissingPermission")
    private fun printDeviceInfo(device: BluetoothDevice) {
        Log.e("my_test", "=== ${device.name} ===")
        Log.e("my_test", "address=${device.address}")
        Log.e("my_test", "alias=${device.alias}")
        Log.e("my_test", "type=${device.type}")
        Log.e("my_test", "bluetoothClass=${device.bluetoothClass}")
        Log.e("my_test", "bondState=${device.bondState}")
        Log.e("my_test", "uuids=${device.uuids}")
    }
}