package com.das.gaztemap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private final Context context;
    private final List<LeaderboardUser> userList;

    public LeaderboardAdapter(Context context, List<LeaderboardUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardUser user = userList.get(position);

        if (position == 0) {
            // Primer lugar - Dorado
            holder.cardRank.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gold));
            holder.rankMedal.setVisibility(View.VISIBLE);
            holder.rankMedal.setImageResource(R.drawable.gold);
            holder.tvRank.setVisibility(View.GONE);
        } else if (position == 1) {
            // Segundo lugar - Plateado
            holder.cardRank.setCardBackgroundColor(ContextCompat.getColor(context, R.color.silver));
            holder.rankMedal.setVisibility(View.VISIBLE);
            holder.rankMedal.setImageResource(R.drawable.silver);
            holder.tvRank.setVisibility(View.GONE);
        } else if (position == 2) {
            // Tercer lugar - Bronce
            holder.cardRank.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bronze));
            holder.rankMedal.setVisibility(View.VISIBLE);
            holder.rankMedal.setImageResource(R.drawable.bronze);
            holder.tvRank.setVisibility(View.GONE);
        } else {
            // Resto de posiciones
            holder.cardRank.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
            holder.rankMedal.setVisibility(View.GONE);
            holder.tvRank.setVisibility(View.VISIBLE);
            holder.tvRank.setText(String.valueOf(user.getRank()));
        }

        // Configurar datos del usuario
        holder.tvUsername.setText(user.getUsername());
        holder.tvPoints.setText(String.format(context.getString(R.string.points_cantidad), user.getPoints()));

        // Cargar imagen de perfil
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            String urlWithCache = user.getProfileImage() + "?nocache=" + System.currentTimeMillis();
            Glide.with(context)
                    .load(urlWithCache)
                    .placeholder(R.drawable.placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRank;
        TextView tvRank;
        ImageView rankMedal;
        ShapeableImageView profileImage;
        TextView tvUsername;
        TextView tvPoints;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRank = itemView.findViewById(R.id.card_rank);
            tvRank = itemView.findViewById(R.id.tv_rank);
            rankMedal = itemView.findViewById(R.id.rank_medal);
            profileImage = itemView.findViewById(R.id.profile_image);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvPoints = itemView.findViewById(R.id.tv_points);
        }
    }
}