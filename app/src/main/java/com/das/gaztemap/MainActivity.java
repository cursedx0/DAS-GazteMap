package com.das.gaztemap;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity {
    private Dialog loginDialog;
    private Dialog registerDialog;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String currentPhotoPath;
    private ImageView previewImage;
    private ActivityResultLauncher<Intent> takePicture;
    private ActivityResultLauncher<String> pickImage;

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

        registerImagePicker();

        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        editor = prefs.edit();

        setupWelcomeScreen();
        initializeDialogs();
        checkUserLoggedIn();
        registerCamera();
    }

    private void registerCamera() {
        takePicture = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (currentPhotoPath != null) {
                            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                            if (bitmap != null) {
                                previewImage.setImageBitmap(bitmap);
                            }
                        }
                    }
                });
    }

    private void registerImagePicker() {
        pickImage = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            previewImage.setImageURI(uri);

                            currentPhotoPath = createImageFileFromUri(uri);
                        } catch (Exception e) {
                            Log.e("ImagePicker", "Error al procesar la imagen", e);
                            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String createImageFileFromUri(Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("ImageFile", "Error creating image file", e);
            return null;
        }
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
        previewImage = registerDialog.findViewById(R.id.fotoPerfilreg);
        MaterialButton btnCamera = registerDialog.findViewById(R.id.buttonCamReg);
        MaterialButton btnGallery = registerDialog.findViewById(R.id.buttonGalleryReg);

        btnGallery.setOnClickListener(v -> pickImage.launch("image/*"));

        btnCamera.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                File photoFile = createImageFile();

                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            getPackageName() + ".provider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePicture.launch(takePictureIntent);
                }
            } catch (IOException ex) {
                Toast.makeText(this, "Error creando archivo de imagen", Toast.LENGTH_SHORT).show();
                Log.e("CAMERA", "Error creating image file", ex);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
            }
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

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getApplicationContext(), "Correo electrónico no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 4) {
                Toast.makeText(getApplicationContext(), "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getApplicationContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            performRegistration(nombre, email, password);
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void performRegistration(String nombre, String email, String password) {
        Data.Builder dataBuilder = new Data.Builder()
                .putString("accion", "insertar")
                .putString("usuario", nombre)
                .putString("email", email)
                .putString("pw", password);

        Data datos = dataBuilder.build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(MainActivity.this).enqueue(request);

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(MainActivity.this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String code = workInfo.getOutputData().getString("code");
                            if (code != null && code.equals("0")) {
                                Toast.makeText(getApplicationContext(), "Registro completado", Toast.LENGTH_SHORT).show();

                                if (currentPhotoPath != null) {
                                    uploadProfileImage(nombre);
                                }

                                registerDialog.dismiss();
                                performLogin(email, password, true);
                            } else {
                                Toast.makeText(getApplicationContext(), "Error en el registro, email already registered?", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Error en el registro", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void uploadProfileImage(String nombreUsuario) {
        File imageFile = new File(currentPhotoPath);
        if (!imageFile.exists() || !imageFile.canRead()) {
            Log.e("UPLOAD", "File does not exist or cannot be read: " + currentPhotoPath);
            Toast.makeText(getApplicationContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageFile.length() == 0) {
            Log.e("UPLOAD", "File is empty: " + currentPhotoPath);
            Toast.makeText(getApplicationContext(), "Archivo de imagen vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            if (bitmap == null) {
                Log.e("UPLOAD", "Failed to decode bitmap from file: " + currentPhotoPath);
                Toast.makeText(getApplicationContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            Data imageData = new Data.Builder()
                    .putString("url","2") // luken....
                    .putString("accion", "setpfp")
                    .putString("nombre", nombreUsuario)
                    .putString("pic", currentPhotoPath)
                    .build();

            OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(imageData)
                    .setBackoffCriteria(
                            androidx.work.BackoffPolicy.LINEAR,
                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS)
                    .build();

            WorkManager.getInstance(this).enqueue(uploadRequest);

            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(uploadRequest.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String code = workInfo.getOutputData().getString("code");
                                if (code != null && code.equals("0")) {
                                    String url = workInfo.getOutputData().getString("url");
                                    if (url != null) {
                                        Log.d("UPLOAD", "Imagen subida exitosamente: " + url);

                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("lastPfp", url);
                                        editor.apply();

                                        Toast.makeText(getApplicationContext(),
                                                "Imagen de perfil actualizada",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.e("UPLOAD", "Error al subir la imagen. Código: " + code);
                                }
                            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                String errorMsg = workInfo.getOutputData().getString("message");
                                Log.e("UPLOAD", "Error al subir la imagen: " +
                                        (errorMsg != null ? errorMsg : "Error desconocido"));
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e("UPLOAD", "Exception when preparing upload", e);
            Toast.makeText(getApplicationContext(), "Error al preparar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUserLoggedIn() {
        if (prefs.getBoolean("rember", false)) {
            navigateToMapActivity();
            finish();
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
        previewImage.setImageResource(R.drawable.person2);
        currentPhotoPath = null;

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

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(request.getId())
                .observe(MainActivity.this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String mensaje = workInfo.getOutputData().getString("message");
                            Log.d("WORKER", "¡200! " + mensaje);
                            String code = workInfo.getOutputData().getString("code");
                            if (code != null && code.equals("0")) {
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
                            } else if (code != null && code.equals("1")) {
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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}