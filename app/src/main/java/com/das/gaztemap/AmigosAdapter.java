package com.das.gaztemap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AmigosAdapter extends RecyclerView.Adapter<AmigosAdapter.ViewHolder> {

    private List<Amigo> amigosList;

    public AmigosAdapter(List<Amigo> amigosList) {
        this.amigosList = amigosList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_amigo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Amigo amigo = amigosList.get(position);
        holder.textNombre.setText(amigo.getNombre());
        Glide.with(holder.itemView.getContext())
                .load(amigo.getFoto())
                .placeholder(R.drawable.placeholder)
                .into(holder.imagePfp);
    }

    @Override
    public int getItemCount() {
        return amigosList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre;
        ImageView imagePfp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.textUsername);
            imagePfp = itemView.findViewById(R.id.imagePfp);
        }
    }
}
