package com.app.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.player.entity.AudioModel;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder>{

    private Context mContext;
    private List<AudioModel> musicList = new ArrayList<>();

    public MusicAdapter(Context context){
        this.mContext = context;
    }

    public void setData(List<AudioModel> list){
        musicList.addAll(list);
    }

    public void clear(){
        this.musicList.clear();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.musiclist_item, viewGroup, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder musicViewHolder, int i) {
        musicViewHolder.musicTv.setText(musicList.get(i).getDisplayName());
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView musicTv;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            musicTv = itemView.findViewById(R.id.music_name_tv);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(musicOnItemClick != null){
                musicOnItemClick.musicOnItemClick(getLayoutPosition(), musicList.get(getLayoutPosition()).getPath());
            }
        }
    }

    private MusicOnItemClick musicOnItemClick = null;

    public interface MusicOnItemClick{
        void musicOnItemClick(int position, String audioPath);
    }

    public void setOnItemClickListener(MusicOnItemClick musicOnItemClick){
        this.musicOnItemClick = musicOnItemClick;
    }
}
