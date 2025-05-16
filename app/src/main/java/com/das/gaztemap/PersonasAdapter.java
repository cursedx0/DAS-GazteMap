package com.das.gaztemap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PersonasAdapter extends RecyclerView.Adapter<PersonasAdapter.PersonaViewHolder> {

    private List<Amigo> lista;
    private Context context;

    public PersonasAdapter(List<Amigo> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    @NonNull
    @Override
    public PersonaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_persona, parent, false);
        return new PersonaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonaViewHolder holder, int position) {
        Amigo amigo = lista.get(position);
        holder.nombre.setText(amigo.getNombre());

        Glide.with(context)
                .load(amigo.getFoto())
                .placeholder(R.drawable.placeholder)
                .into(holder.imagen);

        holder.botonEnviar.setOnClickListener(v -> {
            // Aquí deberías lanzar una solicitud al servidor para enviar solicitud de amistad
            Toast.makeText(context, "Solicitud enviada a " + amigo.getNombre(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class PersonaViewHolder extends RecyclerView.ViewHolder {
        ImageView imagen;
        TextView nombre;
        Button botonEnviar;

        public PersonaViewHolder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgPersona);
            nombre = itemView.findViewById(R.id.txtNombre);
            botonEnviar = itemView.findViewById(R.id.btnSolicitar);
        }
    }
}
