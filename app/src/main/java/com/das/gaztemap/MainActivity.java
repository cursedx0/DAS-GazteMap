package com.das.gaztemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonReg = findViewById(R.id.buttonToReg);
        EditText iEmail = findViewById(R.id.iEmail);
        EditText iPw = findViewById(R.id.iPw);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        CheckBox cRember = findViewById(R.id.checkRember);
        TextView bienvenido = findViewById(R.id.textBienvenido);
        Button buttonCambiarU = findViewById(R.id.buttonCambiarU);
        Button buttonEntrar = findViewById(R.id.buttonEntrar);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();

        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(MainActivity.this, RegisterActivity.class);
                intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent2);
            }
        });

        buttonCambiarU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iPw.setVisibility(View.VISIBLE);
                iEmail.setVisibility(View.VISIBLE);
                buttonLogin.setVisibility(View.VISIBLE);
                buttonReg.setVisibility(View.VISIBLE);
                cRember.setVisibility(View.VISIBLE);

                buttonCambiarU.setVisibility(View.INVISIBLE);
                bienvenido.setText(getString(R.string.bienvenidoVuelta,prefs.getString("usuario","xxx")));
                bienvenido.setVisibility(View.INVISIBLE);
                buttonEntrar.setVisibility(View.INVISIBLE);
            }
        });

        buttonEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putBoolean("rember", true);
                editor.apply();

                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("id", prefs.getInt("id",0));
                intent.putExtra("nombre", prefs.getString("nombre","error"));
                intent.putExtra("puntos", prefs.getInt("puntos",0));
                intent.putExtra("email",prefs.getString("email","error"));
                intent.putExtra("rember",true);

                startActivity(intent);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = iEmail.getText().toString();
                String pw = iPw.getText().toString();

                Data datos = new Data.Builder()
                        .putString("accion", "login")
                        .putString("email", email)
                        .putString("pw", pw)
                        .build();

                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                        .setInputData(datos)
                        .build();

                WorkManager.getInstance(MainActivity.this).enqueue(request);

                //escuchar resultado
                WorkManager.getInstance(getApplicationContext())
                        .getWorkInfoByIdLiveData(request.getId())
                        .observe(MainActivity.this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                    String mensaje = workInfo.getOutputData().getString("message");
                                    Log.d("WORKER", "¡200! " + mensaje);
                                    String code = workInfo.getOutputData().getString("code");
                                    if(code.equals("0")){
                                        //exito
                                        Toast.makeText(getApplicationContext(), getString(R.string.loginExitoso), Toast.LENGTH_SHORT).show();
                                        int id = workInfo.getOutputData().getInt("id",0);
                                        String nombre = workInfo.getOutputData().getString("nombre");
                                        int puntos = workInfo.getOutputData().getInt("puntos",0);
                                        //String emailOut = workInfo.getOutputData().getString("email");
                                        editor.putInt("id", id);
                                        editor.putString("nombre", nombre);
                                        editor.putString("email", email);
                                        editor.putInt("puntos",0);
                                        editor.putBoolean("rember", cRember.isChecked());
                                        editor.apply();

                                        Intent intent = new Intent(MainActivity.this, MapActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        intent.putExtra("id", id);
                                        intent.putExtra("nombre", nombre);
                                        intent.putExtra("puntos", puntos);
                                        intent.putExtra("email",email);
                                        intent.putExtra("rember", cRember.isChecked());

                                        startActivity(intent);

                                    }else if(code.equals("1")){
                                        //bad credentials
                                        Toast.makeText(getApplicationContext(), getString(R.string.loginIncorrecto), Toast.LENGTH_SHORT).show();
                                    }else{
                                        Toast.makeText(getApplicationContext(), getString(R.string.loginError), Toast.LENGTH_SHORT).show();
                                    }
                                    iEmail.setText("");
                                    iPw.setText(""); //para evitar demasiadas solicitudes
                                } else {
                                    Log.e("WORKER", "Algo falló.");
                                }
                            }
                        });

            }
        });

        if(prefs.getBoolean("rember", false)){
            iPw.setVisibility(View.INVISIBLE);
            iEmail.setVisibility(View.INVISIBLE);
            buttonLogin.setVisibility(View.INVISIBLE);
            buttonReg.setVisibility(View.INVISIBLE);
            cRember.setVisibility(View.INVISIBLE);

            buttonCambiarU.setVisibility(View.VISIBLE);
            bienvenido.setText(getString(R.string.bienvenidoVuelta,prefs.getString("nombre","error")));
            bienvenido.setVisibility(View.VISIBLE);
            buttonEntrar.setVisibility(View.VISIBLE);
        }

    }
}