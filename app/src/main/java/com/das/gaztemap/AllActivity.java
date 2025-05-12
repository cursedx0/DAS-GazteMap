package com.das.gaztemap;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

public class AllActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //TODO INTENCIONES: ESTA ACTIVIDAD TENDRÁ UN HIDDENDRAWER (LA PESTAÑA QUE SALE DE LA IZQUIERDA CON LA FOTO DE PERFIL Y DETALLES DEL USUARIO) Y UN TOOLBAR, ASI COMO UN FRAGMENT CONTAINER QUE SE MODIFICARÁ DEPENDIENDO DE LA OPCIÓN ELEGIDA (PUEDE HABER EXCEPCIONES).
        // esta metido dentro del mapa <(:)
        //USUARIOS DE PRUEBA
            //email: xxx, pw: xxx
            //email: yyy, pw: yyy
    }









    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("GazteMap"); //personalizar título
        }
        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id==R.id.opciones) {
            Preferencias prefs = new Preferencias();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, prefs);
            transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        }
        else if (id==R.id.viajar){
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        }
        else if (id==R.id.logout){

        }
        else if(id==R.id.top500){

        }
        else if(id==R.id.amigos){
            /*getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new AmigosFragment())
                    .commit();*/
            AmigosFragment af = new AmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, af);
            transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        }
        return super.onOptionsItemSelected(item);
    }
}