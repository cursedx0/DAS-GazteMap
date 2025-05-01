package com.example.gaztemap;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.i("BUENAS",s);
        switch (s) {
            case "idioma":
                Log.i("BUENAS", "SOY YO");
                String newLanguage = sharedPreferences.getString(s, "ES");

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).setLocale(newLanguage); // 🔹 Aplicar el nuevo idioma
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
