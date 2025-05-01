package com.example.gaztemap;

import android.Manifest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedTheme = prefs.getString("tema", "system");
        setThemeMode(savedTheme, false); //establecer tema al crear actividad

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) {
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
    }

    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String savedLanguage = prefs.getString("idioma", "ES");

        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);

        /*String savedTheme = prefs.getString("tema", "SY");
        Log.i("BUENAS", savedTheme);
        setThemeMode(savedTheme);*/

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);

        super.attachBaseContext(context); //Aplica el contexto con el nuevo idioma
    }

    protected void setLocale(String languageCode) {
        Log.i("BaseActivity", "Cambiando idioma a: " + languageCode);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("idioma", languageCode).apply(); //Guarda el idioma correctamente

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        recreate(); //Reinicia la actividad para aplicar los cambios
    }

    protected void setThemeMode(String theme, boolean reset) {
        Log.d("BaseActivity", "Cambiando tema a: " + theme);

        int mode;
        switch (theme) {
            case "BR": //bright
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "DK": //dark
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "SY": //system
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("tema", theme).apply();

        AppCompatDelegate.setDefaultNightMode(mode);

        if(reset){
            recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        super.onOptionsItemSelected(item);
        return true;
    }

}