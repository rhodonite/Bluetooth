package com.rhodonite.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class ConnectedThread extends Thread {

    // Debugging for LOGCAT
    private static final String TAG = "ConnectionThread";

    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private Handler bluetoothIn;
    // creation of the connect thread //
    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {

            //Create I/O streams for connection
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, " ConnectedThread " + e.getMessage());
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        bluetoothIn = handler;
    }


    public void run() {
        byte[] buffer = new byte[256];
        int bytes;

        // keep looping to listen for received messages //
        while (true) {
            try {
                // read bytes from input buffer //
                bytes = mmInStream.read(buffer);
                // String readMessage = hd.hexToString(hd.stringTobytes(new String(buffer, 0, bytes)));
                // Send recived message to handler in mainActivity //
                byte[] temp = new byte[bytes];
                temp = Arrays.copyOf(buffer, bytes);
                String readMessage_1 = HexData.hexToString(temp);
                Log.e("read msg",readMessage_1);
                bluetoothIn.obtainMessage(Constants.MESSAGE_HANDLER_STATE, bytes, -1, readMessage_1).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "read " + e.getMessage());
                break;
            }
        }
    }

    // write method //
    public void write(byte[] input) {
        // converts entered String into bytes //
       // byte[] msgBuffer = input.getBytes();

        try {
            //write bytes over BT connection via outstream //
            mmOutStream.write(input);

        } catch (IOException e) {
            Log.e(TAG, "write :" + e.getMessage());
        }
    }

}
