
package com.das.gaztemap;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private static final String TAG = "LeaderboardAdapter";
    private Context context;
    private List<LeaderboardUser> userList;

    public LeaderboardAdapter(Context context, List<LeaderboardUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardUser user = userList.get(position);
        holder.tvRank.setText(String.valueOf(user.getRank()));

        holder.tvUsername.setText(user.getName());

        holder.tvPoints.setText(String.format(context.getString(R.string.points_cantidad), user.getPoints()));

        if (holder.onlineIndicator != null) {
            if (user.isOnline()) {
                holder.onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.onlineIndicator.setVisibility(View.GONE);
            }
        }
        Log.d("fafwreq", user.getProfileImageUrl());
        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.imgProfile);

        if (position < 3) {
            switch (position) {
                case 0:
                    holder.rankBadge.setImageResource(R.drawable.gold);
                    holder.rankBadge.setVisibility(View.VISIBLE);
                    holder.tvRank.setVisibility(View.GONE);
                    break;
                case 1:
                    holder.rankBadge.setImageResource(R.drawable.silver);
                    holder.rankBadge.setVisibility(View.VISIBLE);
                    holder.tvRank.setVisibility(View.GONE);
                    break;
                case 2:
                    holder.rankBadge.setImageResource(R.drawable.bronze);
                    holder.rankBadge.setVisibility(View.VISIBLE);
                    holder.tvRank.setVisibility(View.GONE);
                    break;
            }
        } else {
            holder.rankBadge.setVisibility(View.GONE);
            holder.tvRank.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvUsername;
        TextView tvPoints;
        ShapeableImageView imgProfile;
        ImageView rankBadge;
        View onlineIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvPoints = itemView.findViewById(R.id.tv_points);
            imgProfile = itemView.findViewById(R.id.imgPerfil);
            rankBadge = itemView.findViewById(R.id.rank_medal);
            //onlineIndicator = itemView.findViewById(R.id.online_indicator);
        }
    }
}