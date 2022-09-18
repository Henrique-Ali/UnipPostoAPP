package test.android;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.textfield.TextInputEditText;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.chrono.ChronoLocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


/*
    Mudar imagens no código
    Centralizar o Texto no fotoshop
    Manter mesmo tamanho e espaçamento da lentra no fotoshop
 */
public class MainActivity extends AppCompatActivity {
    private SQLiteDatabase myDB = null;
    //private final String enderecoMacParaConectar = "8C:F1:12:1C:73:A1"; //Teste1
    private final String enderecoMacParaConectar = "88:D7:F6:AD:64:70"; //Teste2
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                    }
                }
            }
         );

    //Classe responsável por receber os valores obtidos pelo objeto ConnectThread e
    //fazer as transformações necessárias para utilização em outros métodos
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MessageConstants.MESSAGE_READ.getVal()) {
                byte[] bytes = (byte[]) msg.obj;
                String newS = new String(bytes, StandardCharsets.UTF_8).split("\0")[0];
                String valorGasolina = newS.split(" ")[0];
                String valorAlcool = newS.split(" ")[1];

                ((EditText) findViewById(R.id.InpGasolina)).setText(valorGasolina);
                ((EditText) findViewById(R.id.InpAlcool)).setText(valorAlcool);

                verificarEntrada(valorGasolina, valorAlcool);

                Toast.makeText(MainActivity.this, "Recebido", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conectarBanco();

        setContentView(R.layout.activity_main);
    }

    //Método que cria a referência ao banco de dados
    private void conectarBanco() {
        myDB = openOrCreateDatabase("DBposto", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS resultadosDB (ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Tipo INTEGER, ValPri REAL, ValSec REAL, data VARCHAR(18))");
    }

    //Método responsável por pegar os valores de Gasolina e Álcool
    public void pegarInformacoes(View view){
        String inputGasolina = ((EditText) findViewById(R.id.InpGasolina)).getText().toString();
        String inputAlcool = ((EditText) findViewById(R.id.InpAlcool)).getText().toString();

        verificarEntrada(inputGasolina, inputAlcool);
    }

    //Verifica se os valores inseridos estão vazios
    public void verificarEntrada(String strGasolina, String strAlcool){
        if (strGasolina.length() > 0 && strAlcool.length() > 0) {
            transformarEntrada(strGasolina, strAlcool);
        }else{
            Toast.makeText(this, "Algum valor está vazio", Toast.LENGTH_SHORT).show();
        }
    }

    //Método que faz a transformação do tipo string para double, para efetuar o cálculo
    public void transformarEntrada(String strGasolina, String strAlcool){
        double valGasolina = 0.0;
        double valAlcool = 0.0;

        valGasolina = Double.parseDouble(strGasolina.replace(",", "."));
        valAlcool = Double.parseDouble(strAlcool.replace(",", "."));

        fazerCalculo(valGasolina, valAlcool);
    }

    //Método que efetua o cálculo e informa o resultado na tela principal
    public void fazerCalculo(double valGasolina, double valAlcool) {
        TextView textResultado = findViewById(R.id.ResultadoText);

        esconderTeclado(this, findViewById(R.id.InpAlcool));
        esconderTeclado(this, findViewById(R.id.InpGasolina));

        if (valAlcool / valGasolina < 0.85) {
            textResultado.setText("Abastecer com Alcool");
            inserirBD(valAlcool, valGasolina, 1);
        } else {
            textResultado.setText("Abastecer com Gasolina");
            inserirBD(valGasolina, valAlcool, 0);
        }
    }

    //Método que fecha o teclado quando o usuário aperta o botão de cálculo
    public static void esconderTeclado(Context context, View editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    //Método que insere os dados obtidos após cálculo
    public void inserirBD(double valPrincipal, double valSecundario, int tipo){
        if (myDB == null) {
            conectarBanco();
        }

        Date data = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm", Locale.getDefault());
        ContentValues newRow = new ContentValues();

        newRow.put("ValPri", valPrincipal);
        newRow.put("ValSec", valSecundario);
        newRow.put("Tipo", tipo);
        newRow.put("data", dateFormat.format(data));

        myDB.insert("resultadosDB", null, newRow);
    }

    //Faz a conexão com um dispositivo pareado ou um pedido dos valores caso já conectado
    public void pegarDadosBluetooth(View view){
        if(connectThread == null) {
            conexaoBluetooth();
        }else{
            connectThread.write();
        }
    }

    //Algoritmo que gerencia o envio e recebimento de dados via bluetooth, código feito com base em:
    //https://developer.android.com/guide/topics/connectivity/bluetooth?hl=pt-br
    private void conexaoBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { //Verifica se o aparelho tem bluetooth
            Toast.makeText(this, "Não tem BlueTooth", Toast.LENGTH_LONG).show();
            return;
        }

        //Pede para abilitar o bluetooth, para API > 31 é necessario outras permissões
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activityResultLauncher.launch(intent);
            }
        }

        //Pega todos os dispositivos pareados
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        /*
        Verifica se endereço MAC de algum dos dispositivos pareados é o alvo da conexão
        onde o alvo é o endereço presente em "enderecoMacParaConectar".
        Se o alvo da conexão estiver entre os dispositivos pareado é criado o objeto connectThread
        que é responsável por fazer e manter a conexão
         */
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceHardwareAddress.equals(enderecoMacParaConectar)) {
                    Toast.makeText(this, device.getAddress(), Toast.LENGTH_SHORT).show();
                    connectThread = new ConnectThread(device, this, handler);
                    connectThread.start();
                    return;
                }
            }
        }
    }

    //Abre a activity histórico e fecha a conexão com o banco de dados
    public void mudarParaHistorico(View view) {
        myDB.close();
        Intent intent = new Intent(this, BDactivity.class);
        startActivity(intent);
    }
}

