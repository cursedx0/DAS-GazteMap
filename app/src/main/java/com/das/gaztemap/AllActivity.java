package com.das.gaztemap;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class AllActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    String frag;
    private DrawerLayout drawerLayout;
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all);
        //setSupportActionBar(findViewById(R.id.toolbar));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //TODO INTENCIONES: ESTA ACTIVIDAD TENDRÁ UN HIDDENDRAWER (LA PESTAÑA QUE SALE DE LA IZQUIERDA CON LA FOTO DE PERFIL Y DETALLES DEL USUARIO) Y UN TOOLBAR, ASI COMO UN FRAGMENT CONTAINER QUE SE MODIFICARÁ DEPENDIENDO DE LA OPCIÓN ELEGIDA (PUEDE HABER EXCEPCIONES).
        // esta metido dentro del mapa <(:)
        //USUARIOS DE PRUEBA
            //email: xxx, pw: xxx
            //email: yyy, pw: yyy

        drawerLayout = findViewById(R.id.drawer_layout);

        // Configurar el perfil en el menú lateral

        Bundle extras = getIntent().getExtras();
        if (extras != null) {;
            frag = extras.getString("frag");
        }
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        actualizarNavCheck();
        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            ShapeableImageView imgPerfil = headerView.findViewById(R.id.imgPerfil);
            TextView txtNombre = headerView.findViewById(R.id.campoNombreNav);
            TextView txtEmail = headerView.findViewById(R.id.campoEmail);

            // poner aqui datos sacados al hacer login
            String nombre = getIntent().getStringExtra("nombre");
            String email = getIntent().getStringExtra("email");
            txtNombre.setText(nombre);
            txtEmail.setText(email);
            // imagen: imgPerfil.setImageResource(fotolukenserver);
        }

        if (Objects.equals(frag, "amigos")) {
            AmigosFragment af = new AmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, af);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        }

        FloatingActionButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    @Override
    public void onBackPressed() {
        // Cerrar el drawer si está abierto cuando se presiona atrás
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
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
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.Viajar) {
            Intent intent = new Intent(AllActivity.this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (id == R.id.Amigos) {
            if (!Objects.equals(frag, "amigos")) { //solo cambia fragment si no está en amigos
                AmigosFragment af = new AmigosFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
                transaction.replace(R.id.fragmentContainer, af);
                //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
                transaction.commit();
                navigationView.setCheckedItem(R.id.Amigos);
                frag = "amigos";
            }else{ //sino simplemente cierra drawer
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        } else if (id == R.id.Top500) {
        } else if (id == R.id.Foro){
            Intent intent = new Intent(AllActivity.this, ForumActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.Opciones) {
            if (!Objects.equals(frag, "opciones")) { //solo cambia fragment si no está en amigos
                Preferencias pref = new Preferencias();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
                transaction.replace(R.id.fragmentContainer, pref);
                //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
                transaction.commit();
                navigationView.setCheckedItem(R.id.Opciones);
                frag = "opciones";
            }else{ //sino simplemente cierra drawer
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        actualizarNavCheck();
    }

    public void actualizarNavCheck(){
        if(Objects.equals(frag, "amigos")) {
            navigationView.setCheckedItem(R.id.Amigos);
        }else if(Objects.equals(frag, "opciones")){
            navigationView.setCheckedItem(R.id.Opciones);
        }
    }

}