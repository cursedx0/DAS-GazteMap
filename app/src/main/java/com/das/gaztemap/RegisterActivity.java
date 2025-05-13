package com.das.gaztemap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RegisterActivity extends BaseActivity {

    private CompositeDisposable disposables = new CompositeDisposable();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText iUserReg = findViewById(R.id.iUserReg);
        EditText iPwReg = findViewById(R.id.iPwReg);
        Button buttonReg = findViewById(R.id.buttonReg);
        Button buttonToLogin = findViewById(R.id.buttonToLogin);
        EditText iEmailReg = findViewById(R.id.iEmailReg);
        TextView errorEmail = findViewById(R.id.textBadEmail);

        int defColor = iUserReg.getCurrentTextColor();//guarda el color de texto por defecto del input para recuperarlo luego, al meter un email valido

        //--OBSERVADOR PARA COMPROBACION DINAMICA DE EMAIL--\\
        Observable<String> emailObservable = Observable.create(emitter -> {
            iEmailReg.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    emitter.onNext(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        });

        disposables.add(
                emailObservable
                        .debounce(400, TimeUnit.MILLISECONDS) //retraso entre comprobaciones
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(input -> {
                            boolean isValid = EMAIL_PATTERN.matcher(input).matches(); //verifica si cumple patron

                            if (isValid) {
                                //iEmailReg.setBackgroundResource(R.drawable.edittext_background);
                                aplicarEstiloEditTextPorDefecto(RegisterActivity.this,iEmailReg);
                                buttonReg.setEnabled(true);
                                errorEmail.setVisibility(View.INVISIBLE);
                                iEmailReg.setTextColor(defColor);
                            } else {
                                iEmailReg.setBackgroundResource(R.drawable.edittext_error_background);
                                iEmailReg.setTextColor(Color.BLACK);
                                errorEmail.setVisibility(View.VISIBLE);
                                buttonReg.setEnabled(false);
                            }
                        })
        );

        //--WORKER PARA REGISTRO--\\
        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = String.valueOf(iUserReg.getText());
                String pw = String.valueOf(iPwReg.getText());
                String email = String.valueOf(iEmailReg.getText());
                if(!user.isEmpty() && !pw.isEmpty() && !email.isEmpty()) {
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
                                            Toast.makeText(getApplicationContext(), getString(R.string.usuarioCreado), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
                }


            }
        });

        buttonToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void aplicarEstiloEditTextPorDefecto(Context context, EditText editText) {
        // Elimina cualquier fondo personalizado
        editText.setBackgroundResource(androidx.appcompat.R.drawable.abc_edit_text_material);

        // Restaura paddings por defecto si se han modificado
        int paddingStart = context.getResources().getDimensionPixelSize(R.dimen.edit_text_padding_start);
        int paddingTop = context.getResources().getDimensionPixelSize(R.dimen.edit_text_padding_top);
        int paddingEnd = context.getResources().getDimensionPixelSize(R.dimen.edit_text_padding_end);
        int paddingBottom = context.getResources().getDimensionPixelSize(R.dimen.edit_text_padding_bottom);

        editText.setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom);

        // Restaura fuente por defecto (opcional)
        editText.setTypeface(Typeface.DEFAULT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear(); //evita fugas de memoria
    }
}