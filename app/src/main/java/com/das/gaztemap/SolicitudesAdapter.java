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

public class SolicitudesAdapter extends RecyclerView.Adapter<SolicitudesAdapter.SolicitudViewHolder> {

    private List<Amigo> lista;
    private Context context;
    private OnSolicitudClickListener listener;

    public SolicitudesAdapter(List<Amigo> lista, Context context, OnSolicitudClickListener listener) {
        this.lista = lista;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SolicitudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_solicitud, parent, false);
        return new SolicitudViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull SolicitudViewHolder holder, int position) {
        Amigo amigo = lista.get(position);
        holder.nombre.setText(amigo.getNombre());

        Glide.with(context)
                .load(amigo.getFoto())
                .placeholder(R.drawable.placeholder)
                .into(holder.imagen);

        holder.btnAceptar.setOnClickListener(v -> listener.onAceptarClick(amigo));
        holder.btnRechazar.setOnClickListener(v -> listener.onRechazarClick(amigo));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class SolicitudViewHolder extends RecyclerView.ViewHolder {
        ImageView imagen;
        TextView nombre;
        Button btnAceptar, btnRechazar;

        public SolicitudViewHolder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgSolicitud);
            nombre = itemView.findViewById(R.id.txtNombreSolicitud);
            btnAceptar = itemView.findViewById(R.id.btnAceptarSolicitud);
            btnRechazar = itemView.findViewById(R.id.btnRechazarSolicitud);
        }
    }
}
