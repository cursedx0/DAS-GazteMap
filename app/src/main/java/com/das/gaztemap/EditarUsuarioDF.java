package com.das.gaztemap;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditarUsuarioDF extends DialogFragment {

    private EditText editNombre, editEmail, editPw, myPw;
    private Button btnGuardar;
    private OnUsuarioEditadoListener listener;
    private String nombre;
    private ImageView imgPerfil;
    private Button buttonCam;
    private Button buttonGallery;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private String currentPhotoPath;
    private Uri selectedGalleryImageUri;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY = 1001;
    private static final int REQUEST_PERMISSION_GALLERY = 2001;
    public interface OnUsuarioEditadoListener {
        void onUsuarioEditado(); // Para que el fragmento padre sepa cuándo actualizar
    }

    public void setOnUsuarioEditadoListener(OnUsuarioEditadoListener listener) {
        this.listener = listener;
    }

    public static EditarUsuarioDF newIntance(String result){
        EditarUsuarioDF myDialog = new EditarUsuarioDF();
        myDialog.nombre=result;
        return myDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit, null);

        editNombre = view.findViewById(R.id.etEditName);
        editEmail = view.findViewById(R.id.etEditEmail);
        editPw = view.findViewById(R.id.etEditPassword);
        myPw = view.findViewById(R.id.etMyPassword);
        btnGuardar = view.findViewById(R.id.btnSaveEdit);
        imgPerfil = view.findViewById(R.id.imgEditProfile);
        buttonCam = view.findViewById(R.id.buttonCam);
        buttonGallery = view.findViewById(R.id.buttonGallery);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Opcional: fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
        obtenerPfpNav();

        //--LÓGICA DE CAMARA Y GALERÍA--\\

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean readGranted = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    if (readGranted) {
                        abrirGaleria();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.requierePermisosGal), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        //configurar launcher de camara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        subirPfp();
                    }
                }
        );
        //configurar launcher de galeria
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        selectedGalleryImageUri = imageUri;

                        String path = uri2path(requireContext(), imageUri);
                        if (path != null) {
                            currentPhotoPath = path;
                            subirPfp();
                        } else {
                            Toast.makeText(requireContext(), "No se pudo obtener la ruta de la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


        //onClick camara
        buttonCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    //no tiene permisos, pedirlos
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION);
                    Toast.makeText(getContext(), getString(R.string.requierePermisosCam), Toast.LENGTH_SHORT).show();
                } else {
                    //ya tiene permisos, lanzar cámara
                    lanzarCamara();
                }


            }
        });

        //onClick galeria
        buttonGallery.setOnClickListener(v -> {
            String[] permissions = getStoragePermissions();
            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                permissionLauncher.launch(permissions);
            } else {
                abrirGaleria();
            }
        });


        return dialog;
    }

    private void guardarCambios() {
        String nuevoNombre = editNombre.getText().toString().trim();
        String nuevoEmail = editEmail.getText().toString().trim();
        String nuevaPw = editEmail.getText().toString().trim();
        String miPw = myPw.getText().toString().trim();

        if (nuevoNombre.isEmpty() && nuevoEmail.isEmpty() && nuevaPw.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
            return;
        }

        if(miPw.isEmpty()){
            Toast.makeText(getContext(), getString(R.string.necesitaContraseña), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
            Toast.makeText(getContext(), getString(R.string.errorEmail), Toast.LENGTH_SHORT).show();
            return;
        }

        Data inputData = new Data.Builder()
                .putString("accion", "editar")
                .putString("usuario",nombre)
                .putString("nombre", nuevoNombre)
                .putString("email", nuevoEmail)
                .putString("pw",miPw)
                .putString("nuevaPw",nuevaPw)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String mensaje = workInfo.getOutputData().getString("message");
                        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();

                        if (listener != null) {
                            listener.onUsuarioEditado();
                        }
                        dismiss();
                    }
                });
    }

    public void obtenerPfpNav(){
        if(nombre!=null) {
            Data datos = new Data.Builder()
                    .putString("url","2") //url a php gestor de monedas
                    .putString("accion", "getpfp") //obtiene monedas de usuario
                    .putString("nombre", nombre)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);

            //escuchar resultado
            WorkManager.getInstance(requireContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String mensaje = workInfo.getOutputData().getString("message");
                                Log.d("WORKER", "¡200! " + mensaje);
                                String code = workInfo.getOutputData().getString("code");
                                if(code.equals("0")) {
                                    //coger foto
                                    //String fotoraw = workInfo.getOutputData().getString("imagen");
                                    String url = workInfo.getOutputData().getString("url");
                                    String urlConId = url + "?nocache=" + System.currentTimeMillis();
                                    if (url!=null) {
                                        //Log.d("URL_DEBUG", fotoraw);
                                        /*byte[] decodedString = Base64.decode(fotoraw, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        pfp.setImageBitmap(decodedByte);*/
                                        Glide.with(this)
                                                .load(urlConId)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(imgPerfil); // Tu ImageView
                                    }else{
                                        imgPerfil.setImageResource(R.drawable.placeholder);
                                    }
                                }else{
                                    //error total
                                    Log.d("OBETENER IMAGEN", "FALLÓ");
                                }
                            } else {
                                Log.e("WORKER", "Algo falló.");
                            }
                        }
                    });

        }else{
            Toast.makeText(requireContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
        }
    }


    private void lanzarCamara(){
        Log.d("CAM MANAGER", "CLICK");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null || true) {
            Log.d("CAM MANAGER", "ENTRA");
            File photoFile = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(
                        imageFileName,
                        ".jpg",
                        storageDir
                );
                currentPhotoPath = photoFile.getAbsolutePath();
                Log.d("CAM MANAGER", currentPhotoPath);
            } catch (IOException ex) {
                Log.d("CAM MANAGER","Excepción: "+ex);
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri fotoUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private String uri2path(Context context, Uri uri) {//obtiene el path de la imagen desde la foto elegida en la galería
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void subirPfp(){
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        imgPerfil.setImageBitmap(bitmap);

        if(nombre!=null) {
            //String laimagen = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            Data datos = new Data.Builder()
                    .putString("url","2") //url a php gestor de pfps
                    .putString("accion", "setpfp") //obtiene monedas de usuario
                    //.putInt("id",id)
                    .putString("nombre",nombre)
                    .putString("pic", currentPhotoPath) //se le pasa la ruta porque la imagen en sí es demasiado grande
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);

            WorkManager.getInstance(requireContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String mensaje = workInfo.getOutputData().getString("message");
                                Log.d("WORKER", "¡200! " + mensaje);
                                String code = workInfo.getOutputData().getString("code");
                                if(code.equals("0")) {
                                    String url = workInfo.getOutputData().getString("url");
                                    String urlConId = url + "?nocache=" + System.currentTimeMillis();
                                    if (url != null) {
                                        Glide.with(this)
                                                .load(urlConId)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(imgPerfil);
                                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("lastPfp", urlConId);
                                        editor.apply();
                                        Toast.makeText(requireContext(),getString(R.string.cambiosTardan), Toast.LENGTH_LONG).show();
                                    } else {
                                        imgPerfil.setImageResource(R.drawable.placeholder);
                                    }
                                } else {
                                    Log.d("OBTENER IMAGEN", "FALLÓ con código: " + code);
                                }
                            } else {
                                Log.e("WORKER", "Algo falló en setpfp.");
                            }
                        }
                    });
        }else{
            Toast.makeText(requireContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Log.d("GALLERY MANAGER","BUENAS");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        galleryLauncher.launch(intent);
    }

    private String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Toast.makeText(requireContext(),getString(R.string.cambiosTardan), Toast.LENGTH_LONG).show();
        super.onDismiss(dialog);
    }
}

