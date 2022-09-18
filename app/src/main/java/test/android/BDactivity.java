package test.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.util.Locale;

public class BDactivity extends AppCompatActivity {
    private SQLiteDatabase myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bdactivity);

        conectarBanco();
        popularHistorico();
    }

    //Método que cria a referência com o banco de dados
    private void conectarBanco() {
        myDB = openOrCreateDatabase("DBposto", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS resultadosDB (ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Tipo INTEGER, ValPri REAL, ValSec REAL, data VARCHAR(18))");
    }

    /*
      Método responsável por pegar os os dados no banco de dados e adicionar as linhas na activity
      histórico, e por fim finalizar a conexão com o banco e liberar o cursor que não vai ser mais utilizado.
     */
    private void popularHistorico() {
        String[] tipos = {"GASOLINA", "ÁLCOOL"};
        TableLayout tabela = findViewById(R.id.tabelaHistorico);
        try {
            Cursor myCursor = myDB.rawQuery("SELECT * from resultadosDB", null);
            myCursor.moveToLast();
            do{
                View tr = getLayoutInflater().inflate(R.layout.linha_db_v2, null, false);

                ((TextView) tr.findViewById(R.id.TipoPrincipal)).setText(tipos[myCursor.getInt(1)]);
                ((TextView) tr.findViewById(R.id.TipoSecundario)).setText(tipos[1 - myCursor.getInt(1)]);
                ((TextView) tr.findViewById(R.id.ValorPrincipal)).setText(String.format(Locale.getDefault(),"R$ %.2f", myCursor.getDouble(2)));
                ((TextView) tr.findViewById(R.id.ValorSecundario)).setText(String.format(Locale.getDefault(),"R$ %.2f", myCursor.getDouble(3)));

                if(myCursor.getInt(1) == 0){
                    ((RoundedImageView) tr.findViewById(R.id.corTipo)).setImageResource(R.drawable.shape_red);
                }else{
                    ((RoundedImageView) tr.findViewById(R.id.corTipo)).setImageResource(R.drawable.shape_green);
                }

                ((TextView) tr.findViewById(R.id.Data)).setText(myCursor.getString(4).split(" ")[0]);
                ((TextView) tr.findViewById(R.id.Hora)).setText(myCursor.getString(4).split(" ")[1]);


                tabela.addView(tr, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT));
            }while(myCursor.moveToPrevious());
            myDB.close();
            myCursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}