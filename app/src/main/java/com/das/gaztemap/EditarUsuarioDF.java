package com.das.gaztemap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.jetbrains.annotations.Nullable;

public class EditarUsuarioDF extends DialogFragment {

    private EditText editNombre, editEmail, editPw, myPw;
    private Button btnGuardar;
    private OnUsuarioEditadoListener listener;
    private String nombre;

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

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Opcional: fondo transparente
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());

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
                .putInt("id",id)
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
}

