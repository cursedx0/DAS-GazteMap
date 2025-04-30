package com.example.gaztemap;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AllActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //TODO INTENCIONES: ESTA ACTIVIDAD TENDRÁ UN HIDDENDRAWER (LA PESTAÑA QUE SALE DE LA IZQUIERDA CON LA FOTO DE PERFIL Y DETALLES DEL USUARIO) Y UN TOOLBAR, ASI COMO UN FRAGMENT CONTAINER QUE SE MODIFICARÁ DEPENDIENDO DE LA OPCIÓN ELEGIDA (PUEDE HABER EXCEPCIONES).
        //USUARIOS DE PRUEBA
            //email: xxx, pw: xxx
            //email: yyy, pw: yyy
    }
}