package com.example.usbtest

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mSensorManager: SensorManager? = null
    private var mUsbManager: UsbManager? = null
    private var inEndPoint: UsbEndpoint? = null
    private var outEndPoint: UsbEndpoint? = null
    private var btnAdapter: ButtonAdapter? = null
    private var argService: ArgService? = null
    private var mServiceConnection: ServiceConnection? = null
    private val usbStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val deviceName =
                (intent.getParcelableExtra<Parcelable>("device") as UsbDevice).deviceName
            if ("android.hardware.usb.action.USB_DEVICE_ATTACHED" == intent.action) {
                toast("$deviceName is Attached")
            } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED" == intent.action) {
                toast("$deviceName is Detached")
                argService?.let {
                    unbindService(mServiceConnection)
                }
                mServiceConnection?.let {
                    unbindService(it)
                }
                btnAdapter?.setNewData(defaultBtnList)
            }
        }
    }
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

        override fun onSensorChanged(event: SensorEvent?) {
            toast("get sensor")
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        registerReceiver(usbStateReceiver, intentFilter)
        if (verifyPermissions()) {
            initBtn()
        }
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var sensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        mSensorManager?.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBtn()
            } else {
                toast("No External Storge Permission!!")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbStateReceiver)
        mServiceConnection?.let {
            unbindService(it)
        }
        mSensorManager?.unregisterListener(sensorListener)
    }

    private fun verifyPermissions(): Boolean {
        val grantResult =
            ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")
        return if (grantResult != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSION_EXTERNAL_STORAGE,
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    private fun initBtn() {
        btnAdapter = ButtonAdapter()
        btnAdapter?.let {
            it.setOnItemClickListener { _, _, position ->
                when (position) {
                    0 -> initUsbInfo()
                    1 -> toast(argService?.argMcuVersion)
                    2 -> toast(argService?.argLtVersion)
                    3 -> argService?.threeD = true
                    4 -> toast("3D mode = " + argService?.threeD)
                    5 -> argService?.calibration = intArrayOf(1, 2, 3, 4)
                    6 -> toast("calibration = " + argService?.calibration)
                    7 -> argService?.brightness = 4
                    8 -> toast("brightness = " + argService?.brightness)
                    else -> toast("unknown!")
                }
            }
            rv_btn.adapter = it
            it.setNewData(defaultBtnList)
        }
    }

    private fun initUsbInfo() {
        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mUsbManager?.let {
            if (it.deviceList.isNullOrEmpty()) {
                toast("未找到设备")
            } else {
                val arrayList = ArrayList<String>()
                for (usbDevice in it.deviceList.values) {
                    arrayList.add("设备类别" + usbDevice.deviceClass)
                    arrayList.add("设备id" + usbDevice.deviceId)
                    arrayList.add("设备名称" + usbDevice.deviceName)
                    arrayList.add("协议类别" + usbDevice.deviceProtocol)
                    arrayList.add("设备子类别" + usbDevice.deviceSubclass)
                    arrayList.add("生产商ID" + usbDevice.vendorId)
                    arrayList.add("产品ID" + usbDevice.productId)
                    arrayList.add("接口数量" + usbDevice.interfaceCount)
                    if (it.hasPermission(usbDevice)) {
                        val intf = usbDevice.getInterface(0)
                        inEndPoint = intf.getEndpoint(0)
                        outEndPoint = intf.getEndpoint(1)
                        arrayList.add("in节点地址" + inEndPoint?.address)
                        arrayList.add("in节点属性" + inEndPoint?.attributes)
                        arrayList.add("in节点传输方向" + inEndPoint?.direction)
                        arrayList.add("in节点数据长度" + inEndPoint?.maxPacketSize)
                        arrayList.add("out节点地址" + outEndPoint?.address)
                        arrayList.add("out节点属性" + outEndPoint?.attributes)
                        arrayList.add("out节点传输方向" + outEndPoint?.direction)
                        arrayList.add("out节点数据长度" + outEndPoint?.maxPacketSize)
                        val usbAdapter = UsbDeviceAdapter()
                        rv_usb.adapter = usbAdapter
                        usbAdapter.setNewData(arrayList)

                        mServiceConnection = object : ServiceConnection {
                            override fun onServiceConnected(
                                componentName: ComponentName,
                                iBinder: IBinder
                            ) {
                                argService = (iBinder as ArgService.ArgBinder).service
                            }

                            override fun onServiceDisconnected(componentName: ComponentName) {
                                argService = null
                            }
                        }
                        bindService(
                            Intent(this, ArgService::class.java), mServiceConnection!!,
                            Context.BIND_AUTO_CREATE
                        )
                        btnAdapter?.addData(otherBtnList)
                    } else {
                        it.requestPermission(
                            usbDevice,
                            PendingIntent.getBroadcast(this, 0, Intent("com.USB_PERMISSION"), 0)
                        )
                    }
                }
            }
        } ?: toast("手机不支持OTG")
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10
        private val PERMISSION_EXTERNAL_STORAGE = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        private val defaultBtnList = arrayListOf("findUsb")
        private val otherBtnList = arrayListOf(
            "getArgMcuVersion",
            "getArgLtVersion",
            "set3DMode",
            "get3DMode",
            "setCalibration",
            "getCalibration",
            "setBrightness",
            "getBrightness"
        )
    }
}