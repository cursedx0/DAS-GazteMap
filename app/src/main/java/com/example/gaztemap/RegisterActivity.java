package com.example.gaztemap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText iUserReg = findViewById(R.id.iUserReg);
        EditText iPwReg = findViewById(R.id.iPwReg);
        Button buttonReg = findViewById(R.id.buttonReg);
        Button buttonToLogin = findViewById(R.id.buttonToLogin);
        EditText iEmailReg = findViewById(R.id.iEmailReg);

        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = String.valueOf(iUserReg.getText());
                String pw = String.valueOf(iPwReg.getText());
                String email = String.valueOf(iEmailReg.getText());
                if(!user.isEmpty() && !pw.isEmpty()) {
                    Data datos = new Data.Builder()
                            .putString("accion", "insertar")
                            .putString("usuario", user)
                            .putString("pw", pw)
                            .putString("email", email)
                            .build();

                    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                            .setInputData(datos)
                            .build();

                    WorkManager.getInstance(RegisterActivity.this).enqueue(request);

                    //escuchar resultado
                    WorkManager.getInstance(getApplicationContext())
                            .getWorkInfoByIdLiveData(request.getId())
                            .observe(RegisterActivity.this, workInfo -> {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                        String mensaje = workInfo.getOutputData().getString("message");
                                        Log.d("WORKER", "¡200! " + mensaje);
                                        String code = workInfo.getOutputData().getString("code");
                                        if(code.equals("0")){
                                            Toast.makeText(getApplicationContext(), getString(R.string.usuarioCreado), Toast.LENGTH_SHORT).show();
                                        }else if(code.equals("2")){
                                            Toast.makeText(getApplicationContext(), getString(R.string.errorUsuarioEmailCogidos), Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(getApplicationContext(), getString(R.string.usuarioError), Toast.LENGTH_SHORT).show();
                                        }
                                        iUserReg.setText("");
                                        iEmailReg.setText("");
                                        iPwReg.setText(""); //para evitar demasiadas solicitudes
                                    } else {
                                        Log.e("WORKER", "Algo falló.");
                                    }
                                }
                            });
                }else{
                    Toast.makeText(getApplicationContext(), getString(R.string.usuarioError), Toast.LENGTH_SHORT).show();
                }


            }
        });

    }
}