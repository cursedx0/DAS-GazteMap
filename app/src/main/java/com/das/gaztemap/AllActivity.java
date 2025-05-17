package com.das.gaztemap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class AllActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private String frag;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ShapeableImageView imgPerfil;
    private String nombre;
    private String email;
    private String lastPfp;
    private TextView txtNombre, txtEmail;
    private FloatingActionButton buttonPersonas;
    private FloatingActionButton buttonSolis;
    private FloatingActionButton buttonAmigos;

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

        drawerLayout = findViewById(R.id.drawer_layout);

        // Configurar el perfil en el menú lateral

        Bundle extras = getIntent().getExtras();
        if (extras != null) {;
            frag = extras.getString("frag");
            nombre = extras.getString("nombre");
            email = extras.getString("email");
        }
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        actualizarNavCheck();
        View headerView = navigationView.getMenu().findItem(R.id.n_perfil).getActionView();
        if (headerView != null) {
            imgPerfil = headerView.findViewById(R.id.imgPerfil);
            txtNombre = headerView.findViewById(R.id.campoNombreNav);
            txtEmail = headerView.findViewById(R.id.campoEmail);
            nombre = getIntent().getStringExtra("nombre");
            email = getIntent().getStringExtra("email");
            obtenerPfpNav();
        }

        //botones drawer
        Button botonEdit = headerView.findViewById(R.id.btnEditUser);
        botonEdit.setOnClickListener(v -> {
            EditarUsuarioDF edf = EditarUsuarioDF.newIntance(nombre);
            edf.show(getSupportFragmentManager(), "edit_dialog");
        });

        Button botonCerrar = headerView.findViewById(R.id.btnLogout);
        botonCerrar.setOnClickListener(a -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AllActivity.this);
            SharedPreferences.Editor editor = prefs.edit();
            Intent logout = new Intent(AllActivity.this, MainActivity.class);
            editor.putBoolean("rember", false);
            editor.apply();
            startActivity(logout);

        });

        buttonPersonas = findViewById(R.id.buttonPersonas);
        buttonSolis = findViewById(R.id.buttonSolis);
        buttonAmigos = findViewById(R.id.buttonAmigos);
        if (Objects.equals(frag, "amigos")) {
            AmigosFragment af = new AmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, af);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
            buttonPersonas.setVisibility(View.VISIBLE);
            buttonSolis.setVisibility(View.VISIBLE);
            buttonAmigos.setVisibility(View.VISIBLE);
        }else{
            buttonPersonas.setVisibility(View.INVISIBLE);
            buttonSolis.setVisibility(View.INVISIBLE);
            buttonAmigos.setVisibility(View.INVISIBLE);
        }

        buttonPersonas.setOnClickListener(v -> {
            BuscarAmigosFragment baf = new BuscarAmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, baf);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        });

        buttonSolis.setOnClickListener(v -> {
            SolicitudesFragment sf = new SolicitudesFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, sf);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        });

        buttonAmigos.setOnClickListener(v -> {
            AmigosFragment af = new AmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, af);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        });

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

    /*
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
            //getSupportFragmentManager().beginTransaction()
                    //.replace(R.id.fragmentContainer, new AmigosFragment())
                    //.commit();
            AmigosFragment af = new AmigosFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
            transaction.replace(R.id.fragmentContainer, af);
            //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
            transaction.commit();
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.Viajar) {
            Intent intent = new Intent(AllActivity.this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            actExtras();

            startActivity(intent);
        } else if (id == R.id.Amigos) {
            if (!Objects.equals(frag, "amigos")) { //solo cambia fragment si no está en amigos
                irAmigos();
            }else{ //sino simplemente cierra drawer
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        } else if (id == R.id.Top500) {
            Intent intent = new Intent(AllActivity.this, LeaderboardActivity.class);
            actExtras();
            intent.putExtra("nombre", nombre);
            intent.putExtra("email", email);
            startActivity(intent);
        } else if (id == R.id.Foro){
            Intent intent = new Intent(AllActivity.this, ForumActivity.class);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.Foro);
            actExtras();
            intent.putExtra("nombre",nombre);
            intent.putExtra("email",email);
            startActivity(intent);
        }
        else if (id == R.id.Opciones) {
            if (!Objects.equals(frag, "opciones")) { //solo cambia fragment si no está en amigos
                irOpciones();
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

    public void actExtras(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AllActivity.this);
        nombre = prefs.getString("nombre","error");
        email = prefs.getString("email","errormail");
    }

    @Override
    public void onResume(){
        super.onResume();
        actualizarNavCheck();
        if(lastPfp!=null && nombre!=null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AllActivity.this);
            String lp = prefs.getString("lastPfp", "error");
            if (!lastPfp.equals(lp)) {
                obtenerPfpNav();
            }
            nombre = prefs.getString("nombre","error");
            email = prefs.getString("email","errormail");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // importante: actualiza el intent que devuelve getIntent()
        manejarFragmentoDesdeIntent(intent);
    }

    private void irOpciones(){
        Preferencias pref = new Preferencias();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
        transaction.replace(R.id.fragmentContainer, pref);
        //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
        transaction.commit();
        navigationView.setCheckedItem(R.id.Opciones);
        frag = "opciones";
        buttonPersonas.setVisibility(View.INVISIBLE);
        buttonSolis.setVisibility(View.INVISIBLE);
        buttonAmigos.setVisibility(View.INVISIBLE);
    }

    private void irAmigos(){
        AmigosFragment af = new AmigosFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        findViewById(R.id.fragmentContainer).setVisibility(View.VISIBLE);
        transaction.replace(R.id.fragmentContainer, af);
        //transaction.addToBackStack(null); //para poder regresar al fragmento anterior
        transaction.commit();
        navigationView.setCheckedItem(R.id.Amigos);
        frag = "amigos";
        buttonPersonas.setVisibility(View.VISIBLE);
        buttonSolis.setVisibility(View.VISIBLE);
        buttonAmigos.setVisibility(View.VISIBLE);
    }

    private void manejarFragmentoDesdeIntent(Intent intent) {
        String frag = intent.getStringExtra("frag");
        if (frag != null) {
            if (frag.equals("amigos")) {
                irAmigos();
            } else if (frag.equals("opciones")) {
                irOpciones();
            }
        }
    }


    public void actualizarNavCheck(){
        if(Objects.equals(frag, "amigos")) {
            navigationView.setCheckedItem(R.id.Amigos);
        }else if(Objects.equals(frag, "opciones")){
            navigationView.setCheckedItem(R.id.Opciones);
        }
    }

    public void obtenerPfpNav(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AllActivity.this);
        nombre = prefs.getString("nombre","error");
        email = prefs.getString("email","errormail");
        txtNombre.setText(nombre);
        txtEmail.setText(email);
        if(nombre!=null) {
            Data datos = new Data.Builder()
                    .putString("url","2") //url a php gestor de monedas
                    .putString("accion", "getpfp") //obtiene monedas de usuario
                    .putString("nombre", getIntent().getStringExtra("nombre"))
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BDConnector.class)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(AllActivity.this).enqueue(request);

            //escuchar resultado
            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(request.getId())
                    .observe(AllActivity.this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String mensaje = workInfo.getOutputData().getString("message");
                                Log.d("WORKER", "¡200! " + mensaje);
                                String code = workInfo.getOutputData().getString("code");
                                if(code.equals("0")) {
                                    //coger foto
                                    //String fotoraw = workInfo.getOutputData().getString("imagen");
                                    String url = workInfo.getOutputData().getString("url");
                                    String urlConId = url + "?nocache=" + System.currentTimeMillis();
                                    if (url!=null) {
                                        //Log.d("URL_DEBUG", fotoraw);
                                        /*byte[] decodedString = Base64.decode(fotoraw, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        pfp.setImageBitmap(decodedByte);*/
                                        Glide.with(this)
                                                .load(urlConId)
                                                .placeholder(R.drawable.placeholder)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(imgPerfil); // Tu ImageView
                                        lastPfp = urlConId;
                                    }else{
                                        imgPerfil.setImageResource(R.drawable.placeholder);
                                    }
                                }else{
                                    //error total
                                    Log.d("OBETENER IMAGEN", "FALLÓ");
                                }
                            } else {
                                Log.e("WORKER", "Algo falló.");
                            }
                        }
                    });

        }else{
            Toast.makeText(getApplicationContext(), getString(R.string.faltanCampos), Toast.LENGTH_SHORT).show();
        }
    }

}