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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.experimental.and

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    companion object {
        private val GLUCOSE_MEASUREMENT_UUID =
            UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")

        private val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

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

    private val handler: Handler = Handler(Looper.getMainLooper(), Handler.Callback {
        Log.d("my_test", "handler data = ${it.data}")
        return@Callback true
    })

    // SDK 怎麼知道 device 的相關資訊？並且 sync 到它
    private val myDeviceMacAddress = "C0:26:DF:00:B3:D4" // this is foraGD40b

    private var bluetoothDevice: BluetoothDevice? = null
    private var connectThread: ConnectThread? = null

    private var glucoseMeasurementCharacteristic: BluetoothGattCharacteristic? = null

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
        connectThread?.cancel()
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            Log.d("my_test", "onReceive action=$action")
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
        // https://developer.android.com/guide/topics/connectivity/bluetooth#ConnectAsAClient
        val uuid = UUID.nameUUIDFromBytes("CCAB08LP2580T8".toByteArray())
        Log.e("my_test", "start uuid=$uuid")

        device.connectGatt(this, true, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.e("my_test", "onConnectionStateChange($gatt, $status, $newState)")

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        gatt?.close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                Log.e("my_test", "onServicesDiscovered($gatt, $status)")
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    gatt?.services?.forEach { service ->
                        val serviceUuid = service.uuid
                        Log.e("my_test", "serviceUuid=$serviceUuid")

                        service.characteristics.forEach { characteristic ->
                            if (GLUCOSE_MEASUREMENT_UUID == characteristic.uuid) {
                                Log.e("my_test", "FIND GLUCOSE_MEASUREMENT_UUID !!!")
                                glucoseMeasurementCharacteristic = characteristic
                            } else {
//                                Log.d("my_test", "FIND ${characteristic.uuid}")
                            }
                        }
                    }

                    if (glucoseMeasurementCharacteristic == null) {
                        gatt?.disconnect()
                        Log.e("my_test", "disconnect(bg characteristic not found) !!!")
                        return
                    }

//                    glucoseMeasurementCharacteristic?.let {
//                        gatt?.setCharacteristicNotification(it, true)
//                        val descriptor = it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
//                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                        gatt?.readDescriptor(descriptor)
//                    }

                    glucoseMeasurementCharacteristic?.let {
                        val sequenceNum = it.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)

                        val baseDateTime: Date = getDateTimeFromCharacteristic(it, 3)

                        val flag = it.value[0]
//                        if (flag and 0x01 != 0) {
//                            timeOffset =
//                                characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, index)
//                            index += 2
//                        }

//                        var gluco = it.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, index)
//                        if (unit == 0) {
//                            gluco *= 100000f // kg/L => mm/gL
//                        }

                        Log.e("my_test", "sequenceNum=$sequenceNum")
                        Log.e("my_test", "baseDateTime=$baseDateTime")
                    } ?: Log.e("my_test", "glucoseMeasurementCharacteristic is null")
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                Log.e("my_test", "onCharacteristicWrite")
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorWrite(gatt, descriptor, status)
                Log.e("my_test", "onDescriptorWrite")
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                Log.e("my_test", "onMtuChanged")
            }

            override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status)
                Log.e("my_test", "onPhyUpdate")
            }

            override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                super.onReliableWriteCompleted(gatt, status)
                Log.e("my_test", "onReliableWriteCompleted")
            }

            override fun onDescriptorRead(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorRead(gatt, descriptor, status)
                Log.e("my_test", "onDescriptorRead($gatt, $descriptor, $status)")
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                Log.e("my_test", "onCharacteristicRead($gatt, $characteristic, $status)")

                val sequenceNumber = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)
//                val baseTime = characteristic?.getIntValue(BluetoothGattCharacteristic.SC, 0)
                val glucoseConcentration = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 0)

                Log.e("my_test", "sequenceNumber=$sequenceNumber")
                Log.e("my_test", "glucoseConcentration=$glucoseConcentration")
            }

            override fun onServiceChanged(gatt: BluetoothGatt) {
                super.onServiceChanged(gatt)
                Log.e("my_test", "onServiceChanged($gatt)")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                Log.e("my_test", "onCharacteristicChanged($gatt, $characteristic)")

            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
                Log.e("my_test", "onReadRemoteRssi($gatt, $rssi, $status)")
            }

            override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                super.onPhyRead(gatt, txPhy, rxPhy, status)
                Log.e("my_test", "onPhyRead($gatt, txPhy=$txPhy, rxPhy=$rxPhy, status=$status)")
            }
        })
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

    private inner class ConnectThread(device: BluetoothDevice, MY_UUID: UUID) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        private val mmInStream: InputStream? = mmSocket?.inputStream
        private val mmOutStream: OutputStream? = mmSocket?.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        public override fun run() {
            Log.e("my_test", "ConnectThread run")
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
//                manageMyConnectedSocket(socket)


                var numBytes: Int // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    numBytes = try {
                        mmInStream?.read(mmBuffer) ?: 0
                    } catch (e: IOException) {
                        Log.d("my_test", "Input stream was disconnected", e)
                        break
                    }

                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer
                    )
                    readMsg.sendToTarget()
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("my_test", "Could not close the client socket", e)
            }
        }
    }

    private fun getDateTimeFromCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        startIndex: Int
    ): Date {
        var startIndex = startIndex
        val year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, startIndex)
        startIndex += 2
        val month =
            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
        val day = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
        val hours =
            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
        val minutes =
            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
        val seconds =
            characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, startIndex++)
        // DateTime(year, month, day, hours, minutes, seconds)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, seconds)
        }.time
    }
}