package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BuscarAmigosFragment extends Fragment {

    private EditText buscador;
    private RecyclerView recyclerView;
    private PersonasAdapter adapter;
    private List<Amigo> listaAmigos = new ArrayList<>();
    private String nombre;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buscar_amigos, container, false);

        buscador = view.findViewById(R.id.editBuscar);
        recyclerView = view.findViewById(R.id.recyclerAmigos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PersonasAdapter(listaAmigos, requireContext(),persona -> {
            // Aquí va la lógica de añadir amigo, por ejemplo:
            enviarSolicitudAmistad(persona.getNombre());
        });
        recyclerView.setAdapter(adapter);

        buscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscarAmigos(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        /*
        Button buttonAtras = view.findViewById(R.id.buttonAtrasBAF);
        buttonAtras.setOnClickListener(v -> {
            finish();
        });
        */
        return view;
    }

    private void buscarAmigos(String query) {
        listaAmigos.clear(); //aunque no sean amigos, sino personas
        if (!query.isEmpty()) {
            Data inputData = new Data.Builder()
                    .putString("accion", "buscar_personas")
                    .putString("url","1")
                    .putString("usuario", query)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);
            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                    .observe(getViewLifecycleOwner(), workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String json = workInfo.getOutputData().getString("personas");
                            // Parsear JSON a lista de amigos y actualizar el adaptador
                            listaAmigos.clear();
                            listaAmigos.addAll(parsearJson(json));
                            Log.d("AMIGOS",listaAmigos.toString());
                            adapter.notifyDataSetChanged();
                            if(listaAmigos.isEmpty()){
                                Toast.makeText(requireContext(), getString(R.string.sinResultados), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        }
        adapter.notifyDataSetChanged();
    }

    private List<Amigo> parsearJson(String json) {
        // Parsea JSON a lista de objetos Amigo
        Gson gson = new Gson();
        TypeToken<List<Amigo>> typeToken = new TypeToken<List<Amigo>>() {}; //se reutiliza la clase amigo por conveniencia
        return gson.fromJson(json, typeToken.getType());
    }

    private void enviarSolicitudAmistad(String nombrePersona) {
        //lógica de solicitud
        Log.d("SOLICITANTE","ACCIONADO");
        if (!nombrePersona.isEmpty()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            nombre = prefs.getString("nombre","error");
            Data inputData = new Data.Builder()
                    .putString("accion", "enviar_soli")
                    .putString("url","1")
                    .putString("usuarioSolicitado", nombrePersona)
                    .putString("usuario",nombre)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);
            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                    .observe(getViewLifecycleOwner(), workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String codigo = workInfo.getOutputData().getString("code");
                            if(codigo.equals("0")){
                                Toast.makeText(getContext(), getString(R.string.solicitudMandada), Toast.LENGTH_SHORT).show();
                            }else if(codigo.equals("3")){
                                Toast.makeText(getContext(), getString(R.string.solicitudPendiente), Toast.LENGTH_SHORT).show();
                            }else if(codigo.equals("4")){
                                Toast.makeText(getContext(), getString(R.string.yaAmigos), Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

        }
    }
}

