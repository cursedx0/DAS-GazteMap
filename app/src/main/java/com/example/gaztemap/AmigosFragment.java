package com.example.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AmigosFragment extends Fragment {
    private RecyclerView recyclerView;
    private AmigosAdapter adapter;
    private List<Amigo> amigosList = new ArrayList<>();

    public AmigosFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_amigos, container, false);
        recyclerView = view.findViewById(R.id.recyclerAmigos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AmigosAdapter(amigosList);
        recyclerView.setAdapter(adapter);

        cargarAmigosDesdeServidor();

        return view;
    }

    private void cargarAmigosDesdeServidor() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        //SharedPreferences.Editor editor = prefs.edit();
        // Aquí usas WorkManager para hacer una solicitud a tu servidor PHP
        Data inputData = new Data.Builder()
                .putString("accion", "get_amigos")
                .putString("usuario", prefs.getString("nombre","error"))
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String json = workInfo.getOutputData().getString("amigos_json");
                        // Parsear JSON a lista de amigos y actualizar el adaptador
                        amigosList.clear();
                        amigosList.addAll(parsearJson(json));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private List<Amigo> parsearJson(String json) {
        // Parsea JSON a lista de objetos Amigo
        Gson gson = new Gson();
        TypeToken<List<Amigo>> typeToken = new TypeToken<List<Amigo>>() {};
        return gson.fromJson(json, typeToken.getType());
    }
}

