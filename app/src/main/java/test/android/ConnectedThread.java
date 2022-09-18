package test.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private final String TAG = "TESTE";
    private final Handler handler;

    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        this.handler = handler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e("sokectTag", "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    /*
    Método responsável por ler a entrada "mmInStram", é aqui que os valores enviado pelo dispositivo
    "sevidor" são coletados e enviados para o handler onde eles serão utilizados
     */
    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                // Send the obtained bytes to the UI activity.
                Message readMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_READ.getVal(), numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d("sokectTag", "Input stream was disconnected", e);
                break;
            }
        }
    }

    /*
    Método que faz um novo pedido dos valores ao dispositivo "sevidor"
     */
    public void write(String dadosEnviar) {
        byte[] bytes = dadosEnviar.getBytes();
        try {
            mmOutStream.write(bytes);

            Message writtenMsg = handler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE.getVal(), -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);
            Message writeErrorMsg =
                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST.getVal());
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }
}