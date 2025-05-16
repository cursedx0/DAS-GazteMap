package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import com.google.gson.reflect.TypeToken;

import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SolicitudesFragment extends Fragment {

    private RecyclerView recyclerView;
    private SolicitudesAdapter adapter;
    private List<Amigo> listaSolicitudes = new ArrayList<>();
    private String nombre;
    private TextView txtSinSoli;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_solicitudes, container, false);

        recyclerView = view.findViewById(R.id.recyclerSolicitudes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SolicitudesAdapter(listaSolicitudes, requireContext(), new OnSolicitudClickListener() {
            @Override
            public void onAceptarClick(Amigo amigo) {
                responderSolicitud(amigo.getNombre(), true);
            }

            @Override
            public void onRechazarClick(Amigo amigo) {
                responderSolicitud(amigo.getNombre(), false);
            }
        });
        recyclerView.setAdapter(adapter);

        txtSinSoli = view.findViewById(R.id.textSinSoli);
        cargarSolicitudes();

        Button buttonUpdate = view.findViewById(R.id.btnUpdate);
        buttonUpdate.setOnClickListener(a -> {
            cargarSolicitudes();
        });
        return view;
    }

    private void cargarSolicitudes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        nombre = prefs.getString("nombre", "error");

        Data inputData = new Data.Builder()
                .putString("accion", "obtener_solis")
                .putString("url", "1")
                .putString("usuario", nombre)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String json = workInfo.getOutputData().getString("solis");
                        listaSolicitudes.clear();
                        listaSolicitudes.addAll(parsearJson(json));
                        if(listaSolicitudes.isEmpty()){
                            txtSinSoli.setText(getString(R.string.sinSoli));
                            txtSinSoli.setVisibility(View.VISIBLE);
                        }else{
                            txtSinSoli.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private List<Amigo> parsearJson(String json) {
        Gson gson = new Gson();
        TypeToken<List<Amigo>> typeToken = new TypeToken<List<Amigo>>() {};
        return gson.fromJson(json, typeToken.getType());
    }

    private void responderSolicitud(String nombreRemitente, boolean aceptar) {
        String a = "";
        if(aceptar){ //en php es más sencillo usar parámetros string
            a = "true";
        }else{
            a = "false";
        }
        Data inputData = new Data.Builder()
                .putString("accion", "responder_soli")
                .putString("url", "1")
                .putString("usuario", nombre)
                .putString("remitente", nombreRemitente)
                .putString("aceptar", a)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String codigo = workInfo.getOutputData().getString("code");
                        if(codigo.equals("00")){
                            Toast.makeText(getContext(), getString(R.string.soliAceptada), Toast.LENGTH_SHORT).show();
                        }else if (codigo.equals("0")){
                            Toast.makeText(getContext(), getString(R.string.soliRechazada), Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getContext(), getString(R.string.soliError), Toast.LENGTH_SHORT).show();
                        }
                        cargarSolicitudes();
                    }
                });
    }
}

