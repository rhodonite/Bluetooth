package com.rhodonite.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity {
    Button bt_send;
    EditText et_sendData;
    TextView tv_receiveData;
    public ConnectedThread mConnectedThread;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static UUID BTMODULEUUID;
    ParcelUuid[] BTMODULEUUID_1;
    private final Handler bluetoothIn = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MESSAGE_HANDLER_STATE) {
                tv_receiveData.setText((String) msg.obj);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_send = (Button)this.findViewById(R.id.bt_send);
        et_sendData = (EditText)this.findViewById(R.id.et_data);
        tv_receiveData = (TextView)this.findViewById(R.id.tv_receive);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendsomething(HexData.stringTobytes(et_sendData.getText().toString()));
                et_sendData.setText("");
            }
        });
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    BluetoothDevice device;
    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String address = intent.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);
        if (address != null) {
            device = btAdapter.getRemoteDevice(address);
            Log.e(TAG, "address :" + btAdapter.getRemoteDevice(address));
            BTMODULEUUID_1 = device.getUuids();
            BTMODULEUUID = UUID.fromString(BTMODULEUUID_1[0].toString());
            Log.e(TAG, "UUID :" + BTMODULEUUID_1[0].toString());
            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                Log.e(TAG, "-----" + e.getMessage());
            }
            try {
                btSocket.connect();
                Log.e(TAG, "btSocket.connect()...");
            } catch (IOException e) {
                Log.e(TAG, "btSocket.connect(IOException e) " + e.getMessage());
                Log.e(TAG, "Try to close connection...");
                try {
                    btSocket.close();
                    Log.e(TAG, "btSocket.close()...");
                    finish();
                    Toast.makeText(this, "請連結正確的裝置", Toast.LENGTH_SHORT).show();
                } catch (IOException e2) {
                    Log.e(TAG, "Failed to connect");
                    Log.e(TAG, e.getMessage());
                }
            }
            mConnectedThread = new ConnectedThread(btSocket, bluetoothIn);
            mConnectedThread.start();
        }
    }
    private static final String TAG = "MainActivity";
    @Override
    public void onPause() {
        super.onPause();
        try {
            btSocket.close();
        } catch (IOException | NullPointerException e2) {
            Log.e(TAG, e2.getMessage());
        }
    }
    private void checkBTState() {
        if (btAdapter == null) {
            String toastMessage = getResources().getText(R.string.bluetooth_not_supported).toString();
            Toast.makeText(getBaseContext(), toastMessage, Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
    public void sendsomething(byte[] data) {
        if (mConnectedThread != null)
            mConnectedThread.write(data);
    }
}
