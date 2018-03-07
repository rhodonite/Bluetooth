package com.rhodonite.bluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


public class DeviceListActivity extends Activity {

    // Debugging for LOGCAT
    private static final String TAG = "DeviceListActivity";
    MyHandler mHandler;
    private BluetoothAdapter mBtAdapter;
    //Button bt_search,bt_stop;
    ArrayAdapter<String> mPairedDevicesArrayAdapter, mPairedDevicesArrayAdapter_1;
    ListView listView;
    int k_height = 2560, k_width = 1440;
    DisplayMetrics metrics;
    RelativeLayout rl;
    TextView tv;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(this, permissions, 10);
                return;
            }
        }
        mHandler = new MyHandler(this);
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        double monitor_h = (double) metrics.heightPixels / (double) k_height;
        double monitor_w = (double) metrics.widthPixels / (double) k_width;
        rl = (RelativeLayout) this.findViewById(R.id.relative_layout);
        RelativeLayout.LayoutParams rl_hw = (RelativeLayout.LayoutParams) rl.getLayoutParams();
        rl_hw.height = (int) (1904 * monitor_h);
        rl_hw.width = (int) (1187 * monitor_w);
        tv = (TextView) this.findViewById(R.id.tv_list);
        RelativeLayout.LayoutParams tv_hw = (RelativeLayout.LayoutParams) tv.getLayoutParams();
        tv_hw.topMargin = (int) (60 * monitor_h);
        tv_hw.leftMargin = (int) (80 * monitor_w);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (85 * monitor_w));
        lv = (ListView) this.findViewById(R.id.listView);
        RelativeLayout.LayoutParams lv_hw = (RelativeLayout.LayoutParams) lv.getLayoutParams();
        lv_hw.topMargin = (int) (300 * monitor_h);
        lv_hw.bottomMargin = (int) (50 * monitor_h);
        lv_hw.leftMargin = (int) (50 * monitor_w);
        lv_hw.rightMargin = (int) (50 * monitor_w);

    }

    int record_pairedDevices_1_size = 0;

    @Override
    public void onResume() {
        super.onResume();
        checkBTState();
        mPairedDevicesArrayAdapter_1 = new ArrayAdapter<String>(this, R.layout.device_name, R.id.myCheckedTextView1);
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mPairedDevicesArrayAdapter_1);
        listView.setOnItemClickListener(mDeviceClickListener);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        record_pairedDevices_1_size = pairedDevices.size();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter_1.add(device.getName() + Constants.NEW_LINE + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter_1.add(noDevices);
        }
    }

    String connect_name = "";
    String connect_address = "";
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v.findViewById(R.id.myCheckedTextView1)).getText().toString();
            ImageView iv = ((ImageView) v.findViewById(R.id.icon));
            if (info.equals("No devices have been paired")) {
            } else {
                iv.setImageDrawable(getResources().getDrawable(R.mipmap.choose_2));
                String address = info.substring(info.length() - Constants.MAC_ADRESS_LENGHT);
                Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
                i.putExtra(Constants.EXTRA_DEVICE_ADDRESS, address);
                startActivity(i);
            }

        }
    };

    private OnItemClickListener mDeviceSearch = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            adapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - Constants.MAC_ADRESS_LENGHT);
            connect_name = address;
            connect_address = address;
            mPairedDevicesArrayAdapter.remove(info);
            setFillter();
            mPairedDevicesArrayAdapter.notifyDataSetChanged();
        }
    };

    private void checkBTState() {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            String message = getResources().getText(R.string.bluetooth_not_supported).toString();
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                System.out.println("find: " + device.getName());
                System.out.println("Address: " + device.getAddress());

                if (device.getAddress().equalsIgnoreCase(connect_address)) {
                    int connectState = device.getBondState();
                    switch (connectState) {
                        case BluetoothDevice.BOND_NONE:
                            try {
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                createBondMethod.invoke(device);
                                System.out.println("createBondMethod.invoke(device)");
                                new Thread(sendable).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            try {
                                connect(device);
                                System.out.println("connect(device)");

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            System.out.println("in default");
                            break;
                    }
                } else {
                    if (device.getName() != null)
                        mPairedDevicesArrayAdapter.add(device.getName() + Constants.NEW_LINE + device.getAddress());
                    else
                        mPairedDevicesArrayAdapter.add(device.getAddress() + Constants.NEW_LINE + device.getAddress());
                }

            }
        }
    };
    BluetoothAdapter adapter;

    private void setFillter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getApplicationContext().registerReceiver(receiver, intentFilter);
        adapter.startDiscovery();
    }

    private void connect(BluetoothDevice device) throws IOException {

        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        UUID BTMODULEUUID;
        ParcelUuid[] BTMODULEUUID_1;
        BTMODULEUUID_1 = device.getUuids();
        BTMODULEUUID = UUID.fromString(BTMODULEUUID_1[0].toString());
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        socket.connect();
    }

    Set<BluetoothDevice> pairedDevices_1;
    Runnable sendable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Message msg = new Message();
            try {
                msg.what = 1;
                mHandler.sendMessage(msg);
                do {
                    System.out.println("sendable running...");
                    mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                    pairedDevices_1 = mBtAdapter.getBondedDevices();
                    Thread.sleep(500);
                }
                while (record_pairedDevices_1_size == pairedDevices_1.size());
                msg = new Message();
                msg.what = 2;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public class MyHandler extends Handler {
        public final WeakReference<DeviceListActivity> mActivity;

        public MyHandler(DeviceListActivity activity) {
            mActivity = new WeakReference<DeviceListActivity>(activity);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    record_pairedDevices_1_size = pairedDevices_1.size();
                    mPairedDevicesArrayAdapter_1.clear();
                    System.out.println("pairedDevices_1.size():  " + pairedDevices_1.size());
                    if (pairedDevices_1.size() > 0) {
                        for (BluetoothDevice device_1 : pairedDevices_1) {
                            mPairedDevicesArrayAdapter_1.add(device_1.getName() + Constants.NEW_LINE + device_1.getAddress());
                        }
                    } else {
                        String noDevices = getResources().getText(R.string.none_paired).toString();
                        mPairedDevicesArrayAdapter_1.add(noDevices);
                    }
                    mPairedDevicesArrayAdapter_1.notifyDataSetChanged();
                    connect_address = "";
                    break;
                case 1:

                    break;
            }
        }
    }
    BluetoothSocket socket;
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        final String name = (String) ((TextView) info.targetView).getText();
        String address = name.substring(name.length() - Constants.MAC_ADRESS_LENGHT);
        switch (item.getItemId()) {
            case 0:
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                pairedDevices_1 = mBtAdapter.getBondedDevices();
                for (BluetoothDevice device_1 : pairedDevices_1) {
                    if (device_1.getAddress().equals(address)) {
                        try {
                            Method m = device_1.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(device_1, (Object[]) null);
                            mPairedDevicesArrayAdapter_1.remove(name);
                            mPairedDevicesArrayAdapter_1.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                break;
            case 1:
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                pairedDevices_1 = mBtAdapter.getBondedDevices();
                for (BluetoothDevice device_1 : pairedDevices_1) {
                    if (device_1.getAddress().equals(address)) {
                        try {
                            BluetoothDevice mBluetoothDevice = adapter.getRemoteDevice(address);
                            final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
                            UUID uuid = UUID.fromString(SPP_UUID);
                            try {
                                socket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                            } catch (IOException ignored) {
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }

                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
}
