package com.example.musicplayer;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListDatabase {
    private MyDatabaseHelper dbHelper;
    private Context mContext;
    private SQLiteDatabase db;

    public ListDatabase(Context mContext){
        this.mContext=mContext;
        this.dbHelper = new MyDatabaseHelper(mContext, "SongList2.db", null, 1);
        this.db = dbHelper.getWritableDatabase();

    }
    public void NewList(){

    }
    public void insertListSingle(ContentValues values){
        db.insert("songlist", null, values);
    }

    public void Delete(String wantDeleteSong ){
        String rawQuerySql = "delete from songlist where songName=?";
        System.out.println("wantDeleteSong="+wantDeleteSong);
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(rawQuerySql,new Object[]{wantDeleteSong});
        System.out.println("wantDeleteSong OK");

    }
    public void DeleteTable(){
        String deleteTableSql = "delete from songlist";
        db.execSQL(deleteTableSql);
    }

    public List<String> getNameList(){
        List<String> dataBaseNameList = new ArrayList<>();
        String rawQuerySql = null;
        rawQuerySql = "select * from songlist";
        Cursor cursor = db.rawQuery(rawQuerySql, null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String songName = cursor.getString(cursor.getColumnIndex("songName"));
//                        System.out.println("songName==="+songName);
            dataBaseNameList.add(songName);
        }
        cursor.close();
        return dataBaseNameList;
    }
    public List<String> getPathList(){
        List<String> dataBasePathList = new ArrayList<>();
        String rawQuerySql = null;
        rawQuerySql = "select * from songlist";
        Cursor cursor = db.rawQuery(rawQuerySql, null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String songPath = cursor.getString(cursor.getColumnIndex("songPath"));
//                        System.out.println("songPath==="+songPath);
            dataBasePathList.add(songPath);
        }
        cursor.close();
        return dataBasePathList;
    }
    public String getPathViaName(String songName){
        String songPathViaName = "";
        String rawQuerySql = null;
        rawQuerySql = "select * from songlist where songName = "+"\""+songName+"\"";
        Cursor cursor = db.rawQuery(rawQuerySql, null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String songPath = cursor.getString(cursor.getColumnIndex("songPath"));
//                        System.out.println("songPath==="+songPath);
            songPathViaName=songPath;
        }
        cursor.close();
        return songPathViaName;
    }

    public int getPosViaName(String songName){
        int posViaName=0;
        List<String> dataBaseNameList = getNameList();
        for (int i = 0; i < dataBaseNameList.size(); i++) {

            if (Objects.equals(dataBaseNameList.get(i), songName)){
                posViaName=i;
            }
        }
        return posViaName;
    }
        public int getPosCount(){
        List<String> dataBaseNameList = getNameList();
        return dataBaseNameList.size();
    }



}
