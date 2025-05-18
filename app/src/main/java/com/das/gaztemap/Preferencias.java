package com.das.gaztemap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Preferencias extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.pref_config);

        Preference salirPref = findPreference("salir");
        if (salirPref != null) {
            salirPref.setOnPreferenceClickListener(preference -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()//.popBackStack(); // debería cerrar, pero no lo hace
                            .beginTransaction()
                            .remove(this) // así que ya nos dejamos de chorradas
                            .commit();
                }
                return true;
            });
        }
        Preference compartirUbicacionPref = findPreference("compartir_ubicacion");
        if (compartirUbicacionPref != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            int userId = prefs.getInt("id", -1);

            // Cargar el estado actual desde la base de datos
            if (userId != -1) {
                new Thread(() -> {
                    try {
                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("accion", "getCompartirUbicacion");
                        jsonBody.put("id", userId);

                        OkHttpClient client = new OkHttpClient();
                        RequestBody body = RequestBody.create(
                                jsonBody.toString(),
                                okhttp3.MediaType.parse("application/json; charset=utf-8")
                        );
                        Request request = new Request.Builder()
                                .url("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/pfp.php")
                                .post(body)
                                .build();

                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            boolean compartirUbicacion = jsonResponse.getString("compartir_ubicacion").equals("1");

                            requireActivity().runOnUiThread(() -> {
                                ((SwitchPreferenceCompat) compartirUbicacionPref).setChecked(compartirUbicacion);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Preferencias", "Error al cargar el estado de compartir ubicación", e);
                    }
                }).start();
            }

            // Manejar cambios en el estado del switch
            compartirUbicacionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (boolean) newValue;

                new Thread(() -> {
                    try {
                        JSONObject jsonBody = new JSONObject();
                        jsonBody.put("accion", "setCompartirUbicacion");
                        jsonBody.put("id", userId);
                        jsonBody.put("valor", isChecked ? 1 : 0);

                        OkHttpClient client = new OkHttpClient();
                        RequestBody body = RequestBody.create(
                                jsonBody.toString(),
                                okhttp3.MediaType.parse("application/json; charset=utf-8")
                        );
                        Request request = new Request.Builder()
                                .url("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/lbilbao040/WEB/GazteMap/pfp.php")
                                .post(body)
                                .build();

                        Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            Log.e("Preferencias", "Error al actualizar el estado de compartir ubicación");
                        }
                    } catch (Exception e) {
                        Log.e("Preferencias", "Error al enviar el estado de compartir ubicación", e);
                    }
                }).start();

                return true;
            });
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.i("BUENAS",s);
        switch (s) {
            case "idioma":
                String newLanguage = sharedPreferences.getString(s, "ES");
                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).setLocale(newLanguage);
                    requireActivity().recreate();
                }
                break;
            case "tema":
                String newTheme = sharedPreferences.getString(s, "system");
                Log.d("Preferencias", "Nuevo tema seleccionado: " + newTheme);

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).setThemeMode(newTheme, true);
                }
                break;
            case "notis_login":
                /*boolean valorActual = sharedPreferences.getBoolean(s,true);
                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).NOTIS_LOGIN = !valorActual;
                }*/
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
