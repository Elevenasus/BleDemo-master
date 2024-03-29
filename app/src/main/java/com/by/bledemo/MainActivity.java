package com.by.bledemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;//手机蓝牙适配器
    private BluetoothLeScanner scanner;//蓝牙扫描器
    private Handler mHandler = new Handler();//线程通信
    private boolean mScanning;//蓝牙状态

    final UUID UUID_SERVICE = UUID.fromString("5052494D-2DAB-0141-6972-6F6861424C45");//服务的UUID
    //
    //  设备特征值UUID, 需固件配合同时修改
    //
    final UUID UUID_READ = UUID.fromString("43484152-2DAB-1441-6972-6F6861424C45");   // 用于从设备读取数据
    final UUID UUID_WRITE = UUID.fromString("0000AE01-0000-1000-8000-00805F9B34FB");  // 用于发送数据到设备
    final UUID UUID_NOTIFICATION = UUID.fromString("0000AE02-0000-1000-8000-00805F9B34FB"); // 用于接收设备推送的数据

    private BluetoothGatt mBluetoothGatt;//GATT协议接口
    private TextView deviceName;
    private TextView textView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Android 6以上需要动态申请 获取粗略位置和录音权限两个权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {//ACCESS_COARSE_LOCATION 获取粗略位置权限，通过WiFi或移动基站的方式获取用户错略的经纬度信息，定位精度大概误差在30~1500米
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {//RECORD_AUDIO 录音权限，录制声音通过手机或耳机的麦克
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
        //显示设备名称，消息状态
        deviceName = (TextView) findViewById(R.id.device_name);
        textView1 = (TextView) findViewById(R.id.recieve_text);

        //通过获取系统服务得到蓝牙管理者对象
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();

        /* 打开手机蓝牙*/
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BluetoothDevice mDevice;
    final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            String name = device.getName();
            if (name != null) {
                if (name.contains("SV-H1")) {
                    deviceName.setText(name);
                    Log.e(TAG, "名称:  " + name + ",远程设备广告记录的内容"+bytes2HexString(scanRecord));
                    mDevice = device;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }
        }
    };


    ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice bluetoothDevice = result.getDevice();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothDevice != null && bluetoothDevice.getName() != null) {
                        if (bluetoothDevice.getName().equals("SV-H1")){
                            Log.e("222222","蓝牙设备==" + bluetoothDevice.getName() + bluetoothDevice.getAddress());
                        }
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            super.onBatchScanResults(results);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("222222","扫描onBatchScanResults==" + results.size());
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("222222","扫描失败onScanFailed， errorCode==" + errorCode);
        }
    };

    /**
     * 扫描
     */
    private void scanLeDevice() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
//                scanner.stopScan(scanCallback);
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, 30000);
        //手机蓝牙扫描30秒结束
        mScanning = true;
        // 定义一个回调接口供扫描结束处理
//        scanner.startScan(scanCallback);
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    private String TAG = "haha";
    private boolean isServiceConnected;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange: " + newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                String err = "Cannot connect device with error status: " + status;
                gatt.close();
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
                }
                Log.e(TAG, err);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {//当蓝牙设备已经连接
                //获取ble设备上面的服务
                //Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Attempting to start service discovery:" +
                mBluetoothGatt.discoverServices());//开始扫描服务　//连接成功，开始搜索服务，一定要调用此方法，否则获取不到服务

                Log.d(TAG, "onConnectionStateChange: " + "连接成功");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                gatt.close();
                if (mDevice != null) {
                    mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
                }
            }
        }

        //发现服务回调。
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: " + "发现服务 : " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServiceConnected = true;
                boolean serviceFound;
                Log.d(TAG, "onServicesDiscovered: " + "发现服务 : " + status);
                Log.d(TAG, "onServicesDiscovered: " + "读取数据0");

                if (mBluetoothGatt != null && isServiceConnected) {
                    BluetoothGattService gattService = mBluetoothGatt.getService(UUID_SERVICE);
                    BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID_READ);
                    boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    if (b) {
                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            if (b1) {
                                mBluetoothGatt.writeDescriptor(descriptor);
                                Log.d(TAG, "startRead: " + "监听收数据");
                            }
                        }
                    }
                }
                serviceFound = true;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d(TAG, "read value: " + bytes2HexString(characteristic.getValue()));

            Log.d(TAG, "callback characteristic read status " + status
                    + " in thread " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "read value: " + characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + "设置成功");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + "发送成功");

            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        @Override
        public final void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            String response = bytes2HexString(characteristic.getValue());
            Log.e(TAG,  "The response is "+ response);

            byte[] value = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged: " + value);
            String s0 = Integer.toHexString(value[0] & 0xFF);
            String s = Integer.toHexString(value[1] & 0xFF);
            Log.d(TAG, "onCharacteristicChanged: " + s0 + "、" + s);
//            textView1.setText("收到: " + s0 + "、" + s);
            for (byte b : value) {
                Log.d(TAG, "onCharacteristicChanged: " + b);
            }
        }
    };



    /**
     * 字节数组转16进制字符串
     */
    public static String bytes2HexString(byte[] array) {
        StringBuilder builder = new StringBuilder();

        for (byte b : array) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex);
        }
        return builder.toString().toUpperCase();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startScan(View view) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        scanLeDevice();
    }

    public void startConnect(View view) {
        if (mDevice != null) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            mBluetoothGatt = mDevice.connectGatt(MainActivity.this, false, mGattCallback);
        }
    }


    public void startSend(View view) {
        if (mBluetoothGatt != null && isServiceConnected) {
            BluetoothGattService gattService = mBluetoothGatt.getService(UUID_SERVICE);
            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID_WRITE);
            byte[] bytes = new byte[2];
            bytes[0] = 04;
            bytes[1] = 01;
            characteristic.setValue(bytes);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }

    }

    public void startRead(View view) {
        if (mBluetoothGatt != null && isServiceConnected) {
            BluetoothGattService gattService = mBluetoothGatt.getService(UUID_SERVICE);
            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID_READ);
//            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUID_NOTIFICATION);
            mBluetoothGatt.readCharacteristic(characteristic);
            if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                Log.e(TAG,"characteristic属性为可读"+characteristic.getUuid());
                // Log.e("nihao","gattCharacteristic的UUID为:"+gattCharacteristic.getUuid());
                // Log.e("nihao","gattCharacteristic的属性为:  可读");
            }
//            boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
//            if (b) {
//                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
//                for (BluetoothGattDescriptor descriptor : descriptors) {
//                    boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//                    if (b1) {
//                        mBluetoothGatt.writeDescriptor(descriptor);
//                        Log.d(TAG, "startRead: " + "监听收数据");
//                    }
//                }
//            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        super.onDestroy();

    }

    public void stopConnect(View view) {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }
}
