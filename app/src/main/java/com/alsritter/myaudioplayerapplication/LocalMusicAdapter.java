package com.alsritter.myaudioplayerapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LocalMusicAdapter extends RecyclerView.Adapter<LocalMusicAdapter.LocalMusicViewHolder> {

    Context context;
    List<LocalMusicBean> mData;
    MyItemOnClickCallback itemOnClickCallback;


    //创建一个接口用来做回调函数
    // 创建一个回调函数
    public interface MyItemOnClickCallback {
        void onClick(View v,int pos);
    }



    public LocalMusicAdapter(Context context, List<LocalMusicBean> mData,MyItemOnClickCallback itemOnClickCallback) {
        this.context = context;
        this.mData = mData;
        this.itemOnClickCallback = itemOnClickCallback;
    }

    @NonNull
    @Override
    public LocalMusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_music_item,parent,false);
        return new LocalMusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocalMusicViewHolder holder, final int position) {
        LocalMusicBean musicBean = mData.get(position);
        holder.idTv.setText(String.valueOf(position+1));
        holder.songTv.setText(musicBean.getSong());
        holder.singerTv.setText(musicBean.getSinger());
        holder.albumTv.setText(musicBean.getAlbum());
        holder.timeTv.setText(musicBean.getDuration());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemOnClickCallback.onClick(v,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class LocalMusicViewHolder extends RecyclerView.ViewHolder{
        TextView idTv,songTv,singerTv,albumTv,timeTv;

        public LocalMusicViewHolder(@NonNull View itemView) {
            super(itemView);
            idTv = itemView.findViewById(R.id.item_local_music_num);
            songTv = itemView.findViewById(R.id.item_local_music_name);
            singerTv = itemView.findViewById(R.id.item_local_music_singer);
            albumTv = itemView.findViewById(R.id.item_local_music_album);
            timeTv = itemView.findViewById(R.id.item_local_music_time);

        }
    }
}
