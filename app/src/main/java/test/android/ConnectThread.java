package test.android;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

class ConnectThread extends Thread {
    private static final String TAG = "SokecetTag";
    private BluetoothSocket mmSocket = null;
    private final BluetoothDevice mmDevice;
    private MainActivity main;
    private Handler handler;
    private ConnectedThread connectedThread;

    public ConnectThread(BluetoothDevice device, MainActivity main, Handler handler) {
        this.main = main;
        this.handler = handler;

        BluetoothSocket tmp = null;
        mmDevice = device;

        try {
            UUID MY_UUID = UUID.fromString("4999099e-b61f-4b2b-955b-5df2455a3231");
            if (ActivityCompat.checkSelfPermission(main, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    //Método que faz a conexão com o dispositivo alvo, quando a conexão é estabelecida
    //o objeto ConnectedThread é criado
    public void run() {
        try {
            if (ActivityCompat.checkSelfPermission(main, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        connectedThread = new ConnectedThread(mmSocket, handler);
        connectedThread.start();
    }

    //Método que faz um novo pedido dos valores de gasolina e álcool, isso é necessário para que o
    //disposivo "sevidor" só mande os valores quando o dispositivo "cliente" necessitar.
    public void write(){
        if(connectedThread != null){
            connectedThread.write("PedirValores");
        }else{
            Log.e("t", "connectedThread existe");
        }
    }
}