package com.das.gaztemap;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;

public class MainActivity extends BaseActivity {
    private Dialog loginDialog;
    private Dialog registerDialog;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        EdgeToEdge.enable(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(),
                findViewById(android.R.id.content));
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {

            return insets;
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        editor = prefs.edit();

        setupWelcomeScreen();
        initializeDialogs();
        checkUserLoggedIn();
    }

    private void setupWelcomeScreen() {
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnRegistro = findViewById(R.id.btnRegistro);

        btnLogin.setOnClickListener(v -> showLoginDialog());
        btnRegistro.setOnClickListener(v -> showRegisterDialog());
    }

    private void initializeDialogs() {
        loginDialog = new Dialog(this);
        loginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loginDialog.setContentView(R.layout.dialog_login);
        loginDialog.setCancelable(true);

        if (loginDialog.getWindow() != null) {
            loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = loginDialog.getWindow().getAttributes();
            params.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9); // 90% del ancho de pantalla
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            loginDialog.getWindow().setAttributes(params);

            loginDialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        setupLoginDialog();

        registerDialog = new Dialog(this);
        registerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        registerDialog.setContentView(R.layout.dialog_register);
        registerDialog.setCancelable(true);

        if (registerDialog.getWindow() != null) {
            registerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams params = registerDialog.getWindow().getAttributes();
            params.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9); // 90% del ancho de pantalla
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            registerDialog.getWindow().setAttributes(params);

            registerDialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        setupRegisterDialog();
    }

    private void setupLoginDialog() {
        TextInputEditText etEmail = loginDialog.findViewById(R.id.etEmail);
        TextInputEditText etPassword = loginDialog.findViewById(R.id.etPassword);
        MaterialButton btnIniciarSesion = loginDialog.findViewById(R.id.btnIniciarSesion);

        btnIniciarSesion.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pw = etPassword.getText().toString();

            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(getApplicationContext(), getString(R.string.loginIncorrecto), Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, pw, true);
            loginDialog.dismiss();
        });
    }

    private void setupRegisterDialog() {
        TextInputEditText campoNombre = registerDialog.findViewById(R.id.campoNombre);
        TextInputEditText etEmail = registerDialog.findViewById(R.id.etEmail);
        TextInputEditText etPassword = registerDialog.findViewById(R.id.etPassword);
        TextInputEditText etConfirmarPassword = registerDialog.findViewById(R.id.etConfirmarPassword);
        MaterialButton btnCompletarRegistro = registerDialog.findViewById(R.id.btnCompletarRegistro);
        Button botonSeleccionarFoto = registerDialog.findViewById(R.id.botonSeleccionarFoto);

        botonSeleccionarFoto.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Seleccionar foto funcionalidad", Toast.LENGTH_SHORT).show();
        });

        btnCompletarRegistro.setOnClickListener(v -> {
            String nombre = campoNombre.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmarPassword.getText().toString();

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getApplicationContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            // HACER

            Toast.makeText(getApplicationContext(), "Registro completado con éxito", Toast.LENGTH_SHORT).show();
            registerDialog.dismiss();

            performLogin(email, password, true);
        });
    }

    private void checkUserLoggedIn() {
        if (prefs.getBoolean("rember", false)) {
            navigateToMapActivity();
        }
    }

    private void showLoginDialog() {
        TextInputEditText etEmail = loginDialog.findViewById(R.id.etEmail);
        TextInputEditText etPassword = loginDialog.findViewById(R.id.etPassword);

        etEmail.setText("");
        etPassword.setText("");

        loginDialog.show();
    }

    private void showRegisterDialog() {
        TextInputEditText campoNombre = registerDialog.findViewById(R.id.campoNombre);
        TextInputEditText etEmail = registerDialog.findViewById(R.id.etEmail);
        TextInputEditText etPassword = registerDialog.findViewById(R.id.etPassword);
        TextInputEditText etConfirmarPassword = registerDialog.findViewById(R.id.etConfirmarPassword);

        campoNombre.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmarPassword.setText("");

        registerDialog.show();
    }

    private void performLogin(String email, String pw, boolean rememberMe) {
        Data datos = new Data.Builder()
                .putString("accion", "login")
                .putString("email", email)
                .putString("pw", pw)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(MainActivity.this).enqueue(request);

        // Listen for result
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(MainActivity.this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String mensaje = workInfo.getOutputData().getString("message");
                            Log.d("WORKER", "¡200! " + mensaje);
                            String code = workInfo.getOutputData().getString("code");
                            if (code.equals("0")) {
                                Toast.makeText(getApplicationContext(), getString(R.string.loginExitoso), Toast.LENGTH_SHORT).show();
                                int id = workInfo.getOutputData().getInt("id", 0);
                                String nombre = workInfo.getOutputData().getString("nombre");
                                int puntos = workInfo.getOutputData().getInt("puntos", 0);

                                editor.putInt("id", id);
                                editor.putString("nombre", nombre);
                                editor.putString("email", email);
                                editor.putInt("puntos", puntos);
                                editor.putBoolean("rember", rememberMe);
                                editor.apply();

                                navigateToMapActivity();
                            } else if (code.equals("1")) {
                                Toast.makeText(getApplicationContext(), getString(R.string.loginIncorrecto), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.loginError), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("WORKER", "Algo falló.");
                            Toast.makeText(getApplicationContext(), getString(R.string.loginError), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToMapActivity() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("id", prefs.getInt("id", 0));
        intent.putExtra("nombre", prefs.getString("nombre", "error"));
        intent.putExtra("puntos", prefs.getInt("puntos", 0));
        intent.putExtra("email", prefs.getString("email", "error"));
        intent.putExtra("rember", true);
        startActivity(intent);
    }
}