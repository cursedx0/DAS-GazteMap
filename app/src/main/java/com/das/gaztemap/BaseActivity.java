package com.das.gaztemap;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
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

    protected void setLocale(String languageCode) {
        Log.i("BaseActivity", "Cambiando idioma a: " + languageCode);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentLang = prefs.getString("idioma", "ES");

        // pa que hacer nada
        if (currentLang.equals(languageCode)) {
            return;
        }

        prefs.edit().putString("idioma", languageCode).apply();

        // reinicio
        Intent intent = new Intent(this, AllActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        //asesinamos el proceso
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContext(newBase));
    }

    private Context updateBaseContext(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lang = prefs.getString("idioma", "ES");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();

        config.setLocale(locale);
        return context.createConfigurationContext(config);
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