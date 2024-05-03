package com.magyc.esp32bluetoothclassic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.magyc.esp32bluetoothclassic.ui.theme.Esp32BluetoothClassicTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false) -> {
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                } else -> { }
            }
        }

        permissionsRequest.launch(arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        setContent {

            val pairedDevices = getBluetoothPairedDevices(bluetoothManager)
            val silhouetteCameoPlotter = getBluetoothPairedDevice(pairedDevices,
                Machines.ESP32.machineName)
            val bluetoothDevice = getBluetoothRemoteDevice(bluetoothManager, silhouetteCameoPlotter)
            val bluetoothSocket = getBluetoothSocket(bluetoothDevice)

            Esp32BluetoothClassicTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                    Column {
                        Button(onClick = { if (bluetoothSocket?.isConnected == true) {
                            Log.d("Bluetooth", "Socket Connected")
                            sendBluetoothData(bluetoothSocket, "1")
                        } }) {
                            Text(text = "ON")
                        }
                        Button(onClick = { if (bluetoothSocket?.isConnected == true) {
                            Log.d("Bluetooth", "Socket Connected")
                            sendBluetoothData(bluetoothSocket, "0")
                        } }) {
                            Text(text = "OFF")
                        }
                    }
                }

                Thread {
                    try {
                        if (bluetoothSocket?.isConnected == false){
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return@Thread
                            }
                            bluetoothSocket.connect()
                            Log.d("Bluetooth", "Trying Connect...")
                        }
                        return@Thread
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Esp32BluetoothClassicTheme {
        Greeting("Android")
    }
}

@SuppressLint("MissingPermission")
fun getBluetoothPairedDevices(bluetoothManager: BluetoothManager): List<BluetoothPairedDevice>{
    val pairedDevices = mutableListOf<BluetoothPairedDevice>()
    val bondedDevices = bluetoothManager.adapter?.bondedDevices

    bondedDevices?.forEach { bondedDevice ->
        val device = BluetoothPairedDevice(name = bondedDevice.name, macAddress = bondedDevice.address)
        pairedDevices.add(device)

        Log.d("Bluetooth", "Paired Devices : Device Name = ${device.name} Device MAC Address = ${device.macAddress}")
    }

    return pairedDevices
}

fun getBluetoothPairedDevice(pairedDevices : List<BluetoothPairedDevice>, deviceName : String): BluetoothPairedDevice {
    var pairedDeviceFounded = BluetoothPairedDevice()

    if (pairedDevices.isNotEmpty()) {
        pairedDevices.forEach { pairedDevice ->
            if (pairedDevice.name.contains(deviceName)) {
                pairedDeviceFounded = pairedDevice
            }
        }
    }

    Log.d("Bluetooth", "Paired Device $deviceName : $pairedDeviceFounded")

    return pairedDeviceFounded
}

fun getBluetoothRemoteDevice(bluetoothManager: BluetoothManager, bluetoothDevice: BluetoothPairedDevice): BluetoothDevice {
    val bluetoothRemoteDevice = bluetoothManager.adapter.getRemoteDevice(bluetoothDevice.macAddress)

    Log.d("Bluetooth", "Connect to device MAC Address : ${bluetoothDevice.macAddress}")

    return bluetoothRemoteDevice
}

@SuppressLint("MissingPermission")
fun getBluetoothSocket(bluetoothDevice: BluetoothDevice): BluetoothSocket?{
    val socket: BluetoothSocket?
    val uuid = UUID.fromString(BluetoothProfiles.SERIAL_PORT.uuid)

    socket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)

    Log.d("Bluetooth", "Socket  : ${BluetoothProfiles.SERIAL_PORT.uuid}")

    return socket
}

fun sendBluetoothData(socket: BluetoothSocket?, data: String){
    val outputStream = socket?.outputStream
    val outputData = data.toByteArray()

    Thread {
        try {
            outputStream?.write(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()

    Log.d("Bluetooth", "Send Data : $data")
}


data class BluetoothPairedDevice(
    var name: String = "",
    var macAddress: String = "00:00:00:00:00:00",
)

enum class Machines(val machineName: String) {
    ESP32("BlueTooth ESP32")
}

enum class BluetoothProfiles(val uuid: String) {
    SERIAL_PORT("00001101-0000-1000-8000-00805f9b34fb")
}