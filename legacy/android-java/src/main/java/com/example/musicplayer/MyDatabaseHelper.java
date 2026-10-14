package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class MyDatabaseHelper  extends SQLiteOpenHelper {
    public static final String CREATE_Songlist = "create table songlist ("
            + "id integer primary key autoincrement, "
            + "songName text, "
            + "songPath text)";

    private Context mContext;
    private AlertDialog dialog;
    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_Songlist);
//        Toast.makeText(mContext, "创建成功", Toast.LENGTH_SHORT).show();
//        Toast.makeText(mContext, "列表无歌曲，请先扫描本地歌曲", Toast.LENGTH_SHORT).show();
        MyAlertDialogFirst();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void MyAlertDialogFirst(){
        dialog = new AlertDialog.Builder(mContext)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("欢迎使用本软件")//设置对话框的标题
                .setMessage("列表暂无数据\n首先请先点击扫描歌曲哦。")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(MainActivity.this, "点击了好的按钮", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();

    }

}

