package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayAdapter extends RecyclerView.Adapter <RecyclerView.ViewHolder> implements Filterable {
    @NonNull
    private Context mContext;
    public OnItemClickListener mListener;
    private List<String> mObjects;
    private List<String> songNameList;
    public boolean two=false;
    public int searchPosition=0;
    public boolean ifSearch=false;
    private Map<String,String> listMap = new HashMap<>();
    public PlayListMapAdapter playListMapAdapter;
    private MyDatabaseHelper dbHelper;


    public PlayAdapter(Context context , List<String> list,OnItemClickListener listener){
        this.mContext = context;
        this.mObjects = list;
        this.songNameList = list;
        this.mListener = listener;
        this.two=false;
    }
    public PlayAdapter(Context context , List<String> list,List<String> listSongName,OnItemClickListener listener){
        this.mContext = context;
        this.mObjects = list;
        this.songNameList = listSongName;
        this.mListener = listener;
        this.two=false;
    }
    public PlayAdapter(Context context , List<String> list){
        this.mContext = context;
        this.mObjects = list;
        this.songNameList = list;
        this.two=true;

    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener=listener;
    }


    @Override
    public  RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //viewType可以通过这个，展示不同的item
        return new LinearViewHolder(LayoutInflater.from(mContext).inflate(R.layout.song_item, parent, false));
        //这里需要传入每个item长什么样的布局，需要去layout中去画我们的布局
    }

    @Override
    //通过getItemViewType的返回值来选择具体的item显示
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        String str = mObjects.get(position).toString();
        ((LinearViewHolder)holder).textView.setText(str);

        //如果是直接用viewholder的话，是不能用test view的
        //将点击事件放到外面
        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @SuppressLint("NotifyDataSetChanged")
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext,"click..."+position,Toast.LENGTH_SHORT).show();
                if (!two){
                    String curSongName = ((LinearViewHolder) holder).textView.getText().toString();
                    System.out.println("curSongName = "+curSongName);

                    System.out.println("songNameList="+songNameList);
                    for(int i = 0;i<songNameList.size();i++){
                        if (songNameList.get(i).equals(curSongName)){
                            searchPosition = i ;
                        }
                    }
//                    searchPosition=position;
                    System.out.println("searchPosition = "+searchPosition);
                    mListener.onClick(searchPosition);
//                    notifyItemChanged(position);
                    notifyDataSetChanged();
                }
            }
        });


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressLint({"Recycle", "NotifyDataSetChanged"})
            @Override
            public boolean onLongClick(View v) {
//                Toast.makeText(mContext, "发现有人在长按"+position+"位置", Toast.LENGTH_SHORT).show();
//                Toast.makeText(mContext, "长按功能还没做"+position, Toast.LENGTH_SHORT).show();
//                deleteUpdateList=1;
//                String wantDeleteSong = ((LinearViewHolder) holder).textView.getText().toString();
//
////                String rawQuerySql = "delete from songlist where songName = " + "\"" + wantDeleteSong + "\"";
//                String rawQuerySql = "delete from songlist where songName=?";
//
//                System.out.println("wantDeleteSong="+wantDeleteSong);
//                SQLiteDatabase db = dbHelper.getWritableDatabase();
//                db.execSQL(rawQuerySql,new Object[]{wantDeleteSong});


//                db.rawQuery(rawQuerySql, null);
                System.out.println("wantDeleteSong 1");
                searchPosition=position;
                mListener.onLongClick(searchPosition);
                System.out.println("wantDeleteSong 2");
//                    notifyItemChanged(position);
                notifyDataSetChanged();
                System.out.println("wantDeleteSong 3");
//                return mLongClickListener.onLongClick(position);

                return true;
            }
        });


        if(position==getmPosition()){
//                    holder.itemView.setBackgroundColor(R.drawable.select_color);
            holder.itemView.setBackgroundColor(Color.rgb(231, 238, 244));
        }else{
            //否则 初始化字体颜色背景
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }


    //去控制viewType的方法，根据位置的奇偶性来区分
    @Override
    public int getItemViewType(int position) {
        if(position % 2 == 0){
            return 0;//偶数
        }else{
            return 1;
        }
    }
    //设置item个数
    @Override
    public int getItemCount() {
        if (mObjects == null || mObjects.size() == 0)
            return 0;
        return mObjects.size();
    }

    private List<String>  mSourceList;
//    private List<String>  mObjects;

    public void appendList(List<String> list) {
        this.mSourceList = list;
//        System.out.println("list= "+mSourceList);
        //这里需要初始化filterList
        this.mObjects = list;
    }


    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mObjects = (ArrayList<String>) filterResults.values;
                //刷新数据
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                System.out.println("str= "+mSourceList);
                if (charString.isEmpty()) {
                    mObjects = mSourceList;

                } else {
                    List<String>  filteredList = new ArrayList<String> ();
                    int i=0;
                    for (String str : mSourceList) {

//                        System.out.println("for str= "+str);
                        //这里根据需求，添加匹配规则
                        if (str.contains(charString)) {
                            filteredList.add(str);
                        }
                        i++;
                    }

                    mObjects = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mObjects;
                filterResults.count = mObjects.size();
                return filterResults;
            }


        };
    }

    class LinearViewHolder extends RecyclerView.ViewHolder{
        private TextView textView;

        public LinearViewHolder(View itemView){
            super(itemView);
            textView = itemView.findViewById(R.id.RvSongName);
        }
    }
    /**
     * @描述 ：根据下标判断颜色背景
     */
    private  int mPosition;
    public int getmPosition() {
        return mPosition;
    }

    public void setSongNameList(List<String> songNameList) {
        this.songNameList = songNameList;
    }

    public void setmPosition(int mPosition) {
        this.mPosition = mPosition;
    }
    public void setMap(Map<String,String> listMap){
        this.listMap=listMap;
    }

    public void setDbHelper(MyDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public int getSearchPosition() {
        return searchPosition;
    }

    public void setSearchPosition(int searchPosition) {
        this.searchPosition = searchPosition;
        mListener.onClick(searchPosition);
    }

    //接口
    public interface  OnItemClickListener{
        void onClick(int pos);

        void onLongClick(int searchPosition);
    }

//    public interface OnLongClickListener{
//        boolean onLongClick(int pos);
//    }


}

