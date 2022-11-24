package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.receiver.MyReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    public static int deleteUpdateList= 0;
//    private MediaPlayer mp;
//    private mpControl myService;
    public ListDatabase myDbList;

    private LinearLayout MainLayout;
//    private MyReceiver myReceiver;
    private MyReceiverIn myReceiverIn;

    private SeekBar SeekB;
    private TextView TvSongNow;
    private TextView TvSongName;
    private Button BthSelect;
    private Button BtnPlayPause;
    private Button BtnStop;

    private RecyclerView RvItem;
    private TextView songTimeNow;
    private TextView songTimeAll;
    private Button BtnFindAll;
    private EditText ESearchName;
    private Button BSearchSong;
    private Button BtnPre;
    private Button BtnNext;
    private Button BtnQuit;
    private Button BtnLocation;
    private Button BtnOrder;
    private ImageView ImCd;

    private PlayAdapter playAdapter;
    private ObjectAnimator animator;

    private Permission permission;
    private MyHandler handlerBar;
    private Boolean ifSeek = false;
    private Boolean ifCycle = false;
    private int ifCycleCount = 0;
    public int positionCur;

    public File file;
    //    private String[] songList = {file.getName(),file.getName(),file.getName(),file.getName()};
    final int TIMER_MSG = 0X001;
    public int musicTimeAll;
    public int musicTimeNow;
    public String showTimeAll;
    public String showTimeNow;


    final String CHANNEL_ID="CHANNEL_ID";
    public static final String PLAY_PAUSE_SONG="play_pause_song";
//    static final String PAUSE_SONG="pause_song";
    public static final String NEXT_SONG="next_song";
    public static final String PRE_SONG="pre_song";
    public static final String DB_READ="db_read";

    private MyService.MpControl mpControl;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mpControl = (MyService.MpControl) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    private int backDrawOrder = 0;

//    public PlayListMapAdapter playListMapAdapter;
    public List<String> listSongName = new ArrayList<String>();
    private List<String> pathList = new ArrayList<String>();
    private String songNameNow="";
//    private Map<String, String> listMap;

    private MyDatabaseHelper dbHelper;
    private AlertDialog dialog;
    private int boring=0;
    private boolean searchIf=false;

//    private ListDatabase myDbList;

    public boolean stopIf=false;
    public boolean dBStart=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MusicPlayer);
        super.onCreate(savedInstanceState);
        permission = new Permission();
        permission.checkerPermission(this);

        myDbList = new ListDatabase(this);
//        sendNotificationMsg();
        createNotificationChannel();
//        startService(new Intent(this,MyService.class));
        StatusBar statusBar = new StatusBar(MainActivity.this);
        //设置颜色为半透明
        statusBar.setColor(R.color.translucent);
        //设置颜色为透明
//        statusBar.setColor(R.color.transparent);
        //隐藏状态栏
//        statusBar.hide();
        setContentView(R.layout.activity_main);

        Intent intent=new Intent(this,MyService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);



//        myService = new mpControl();

//        mp = new MediaPlayer();
//        mp = mpControl.createMediaPlayer();
//        mp = mpControl.mp;
        handlerBar = new MyHandler();
//        if (mp==null){System.out.println("service null");}




        MainLayout = (LinearLayout) findViewById(R.id.layout);


        RvItem = findViewById(R.id.RvItem);
        RvItem.setItemViewCacheSize(0);

        BtnPlayPause = findViewById(R.id.BtnPlayPause);
        BtnStop = findViewById(R.id.BtnStop);
        BthSelect = findViewById(R.id.BtnSelect);
        BtnFindAll = findViewById(R.id.BtnFindAll);
        SeekB = findViewById(R.id.MusicSeekBar);
        TvSongNow = findViewById(R.id.TvSongNow);
        TvSongName = findViewById(R.id.TvSongName);
        songTimeNow = findViewById(R.id.songTimeNow);
        songTimeAll = findViewById(R.id.songTimeAll);
        ESearchName = findViewById(R.id.ESearchName);
        BSearchSong = findViewById(R.id.BSearchSong);
        BtnPre = findViewById(R.id.BtnPre);
        BtnNext = findViewById(R.id.BtnNext);
        BtnQuit = findViewById(R.id.BtnQuit);
        BtnLocation = findViewById(R.id.BtnLocation);
        BtnOrder = findViewById(R.id.BtnOrder);
        ImCd = findViewById(R.id.ImCd);

        animator = ObjectAnimator.ofFloat(ImCd,"rotation",0,360.0F);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);

        databaseRead();
//        dbHelper = new MyDatabaseHelper(this, "SongList.db", null, 1);
//        myDbList = new ListDatabase(this);


//        handlerBar.sendEmptyMessage(TIMER_MSG);
//        while (dBStart){
//            databaseRead();
//            Log.d("databaseRead", "dBStart_true: ");
//            dBStart=false;
//        }
////
//        Log.d("123456", "databaseRead: ");

        int randomnum = (int) (Math.random() * 14);
        SkinEnable(randomnum);

        BthSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SkinEnable(backDrawOrder);
                SelectSkin();
//                dialog.dismiss();


            }

        });
        BtnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ifCycle=!ifCycle;
                if (ifCycle){

                    Toast.makeText(MainActivity.this, "单曲循环模式", Toast.LENGTH_SHORT).show();
                    BtnOrder.setBackgroundDrawable(getResources().getDrawable(R.drawable.cycle));

                }else {
                    Toast.makeText(MainActivity.this, "列表循环模式", Toast.LENGTH_SHORT).show();
                    BtnOrder.setBackgroundDrawable(getResources().getDrawable(R.drawable.order));
                }


            }
        });
        BtnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                RvItem.smoothScrollToPosition(playAdapter.getSearchPosition());
//                toSendBroadcast();
            }
        });

        BSearchSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchName = ESearchName.getText().toString();
                playAdapter.getFilter().filter(searchName);
                ESearchName.setText("");
            }
        });
        ESearchName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                String name = charSequence.toString();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                playAdapter.getFilter().filter(charSequence);

            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        ESearchName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    String searchName = textView.getText().toString();
                    //进行数据库查询操作
                    playAdapter.getFilter().filter(searchName);
                    textView.setText("");
                }

                return false;
            }
        });

        BtnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playOrPause();
//                if (!mp.isPlaying()) {
//
//                    if (stopIf){
//                        try {
//                            mp.prepare();
//                            stopIf=false;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    mp.start();
//                    SeekB.setMax(mp.getDuration());
////                    BtnPlayPause.setText("暂停");
//                    BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.pause));
//                    TvSongNow.setText("正在播放：");
//                    TvSongName.setText(songNameNow);
//                    animator.resume();
//                    ifPlay = true;
//                } else {
//                    ifPlay = false;
//                    mp.pause();
////                    BtnPlayPause.setText("播放");
//                    BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.playsong));
//                    TvSongNow.setText("暂停播放：");
//                    animator.pause();
//                }
//                    Log.i(TAG, file.getAbsolutePath());
            }
        });

        BtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ifPlay=false;
//                mp.stop();
                mpControl.songStop();
//                mp.reset();
                stopIf=true;
                BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.playsong));
                TvSongNow.setText("停止播放：");
                TvSongName.setText("暂未播放");
                SeekB.setProgress(0);
                animator.pause();
            }
        });
        BtnFindAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TvSongName.setText("扫描本地音乐中...");
                BtnFindAll.setText("扫描中...");
                MyAlertDialog();
                Toast.makeText(MainActivity.this, "扫描本地音乐中...", Toast.LENGTH_SHORT).show();
                new WorkThread().start();

            }
        });

        SeekB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int PositionJump = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                mp.seekTo(i);
                PositionJump = i;
//                System.out.println("i   =  "+i);
//                System.out.println("CurrentPosition   =   "+mp.getCurrentPosition());

//                Log.i("seekbar",String.valueOf(i));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "开始跳动", Toast.LENGTH_SHORT).show();
//                mp.pause();
                mpControl.songPause();

                ifSeek = true;


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "结束跳动", Toast.LENGTH_SHORT).show();
//                mp.start();
//                mp.seekTo(PositionJump);

                mpControl.songStart();
                mpControl.songPositionJump(PositionJump);


                ifSeek = false;

            }
        });
        BtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NextSong();
            }
        });
        BtnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PreSong();
            }
        });
        BtnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyOnlineAlertDialog();
                Uri uri = Uri.parse("https://tools.liumingye.cn/music/#/");
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(uri);
                startActivity(intent);

//                boring++;
//                if (boring<10){
//                    Toast.makeText(MainActivity.this, "你点击了"+boring+"次",Toast.LENGTH_SHORT).show();
//                }
//                if (boring>=10&&boring<20){
//                    Toast.makeText(MainActivity.this, "你点击了"+boring+"次"+"，开始无聊起来了。",Toast.LENGTH_SHORT).show();
//                }
//                if (boring>=20&&boring<30){
//                    Toast.makeText(MainActivity.this, "你点击了"+boring+"次"+"，你这孩子真无聊哈，别按了啊。",Toast.LENGTH_SHORT).show();
//                }
//                if (boring>=30){
//                    Toast.makeText(MainActivity.this, "你点击了"+boring+"次"+"，荣获无聊大王称号，本软件甘拜下风。",Toast.LENGTH_SHORT).show();
//                }


            }
        });

    }

    static public boolean ifPlay = false;

    private void play() {
//        mp.reset();
        mpControl.songRest();

        SeekB.setProgress(0);
        animator.start();


//            mp.setDataSource(file.getAbsolutePath());
//            mp.prepare();

//            mpControl.songPlay(file);
        mpControl.songPlay(file.getAbsolutePath());



//            int musicTimeAll = mp.getDuration();
        musicTimeAll = mpControl.songGetTimeAll();

        showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;

        SeekB.setMax(musicTimeAll);
        handlerBar.sendEmptyMessage(TIMER_MSG);
//            mp.start();
        mpControl.songStart();

        ifPlay = true;
        stopIf = false;
//            BtnPlayPause.setText("暂停");
        songNameNow = file.getName();
        TvSongName.setText(songNameNow);
        songTimeAll.setText(showTimeAll);
        BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.pause));
        mpControl.setPositionSongName(songNameNow);
        sendNotificationChangePlay(songNameNow);

    }
    private void playOrPause() {
//        if (!mp.isPlaying()) {
            if (!mpControl.songIsPlaying()) {

            if (stopIf){
//                try {
//                    mp.prepare();
////                    myService.songPrepare();
//                    stopIf=false;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                mpControl.songPrepare();
                stopIf=false;
            }


//            mp.start();
                mpControl.songStart();



//            SeekB.setMax(mp.getDuration());
            SeekB.setMax(mpControl.songGetTimeAll());
//                    BtnPlayPause.setText("暂停");
            BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.pause));
            TvSongNow.setText("正在播放：");
            TvSongName.setText(songNameNow);
            mpControl.setPositionSongName(songNameNow);
            animator.resume();
            ifPlay = true;
        } else {
            ifPlay = false;
//            mp.pause();
                mpControl.songPause();
//                    BtnPlayPause.setText("播放");
            BtnPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.playsong));
            TvSongNow.setText("暂停播放：");
            animator.pause();
            mpControl.setPositionSongName(songNameNow);
            sendNotificationChangePause(songNameNow);

        }

    }

    public void NextSong() {

        int countSong = playAdapter.getItemCount();
//        int nowCur = positionCur;
        int nowCur = playAdapter.getSearchPosition();
        System.out.println("countSong = " + countSong);
        System.out.println("nowCur = " + nowCur);
//                Drawable drawable=getResources().getDrawable(R.drawable.select_color);

        if (nowCur < countSong-1) {
            nowCur++;
            playAdapter.setSearchPosition(nowCur);
        } else if (nowCur == countSong-1) {
            nowCur = 0;
            playAdapter.setSearchPosition(nowCur);
        }
//        playAdapter.mListener.onClick(nowCur);
    }

    public void PreSong() {
        int countSong = playAdapter.getItemCount();
//        int nowCur = positionCur;
        int nowCur = playAdapter.getSearchPosition();
//        System.out.println("countSong = " + countSong);
//        System.out.println("nowCur = " + nowCur);

//                Drawable drawable=getResources().getDrawable(R.drawable.select_color);
        if (nowCur == 0) {
            nowCur = countSong - 1;
            playAdapter.setSearchPosition(nowCur);
        } else if (nowCur <= countSong && nowCur > 0) {
            nowCur--;
            playAdapter.setSearchPosition(nowCur);
        }
        playAdapter.mListener.onClick(nowCur);


    }

    public void databaseRead() {


        RvItem.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        pathList = myDbList.getPathList();
//        pathList = mpControl.dBGetPathList();
        listSongName = myDbList.getNameList();
//        listSongName = mpControl.dBGetNameList();

        playAdapter = new PlayAdapter(MainActivity.this, pathList,listSongName, new PlayAdapter.OnItemClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(int pos) {
//                        Toast.makeText(MainActivity.this, "click = "+pos,Toast.LENGTH_SHORT).show();
                String song_path=pathList.get(pos);
                positionCur = pos;
                file = new File(song_path);
                playAdapter.setmPosition(pos);

                mpControl.setPositionSongNow(pos);
                mpControl.setPositionSongCount(playAdapter.getItemCount());

                playAdapter.notifyDataSetChanged();
                play();
            }
            @Override
            public void onLongClick(int pos) {
                String wantDeleteName = myDbList.getNameList().get(pos);
//                String wantDeleteName = mpControl.dBGetName(pos);
                String wantDeletePath = myDbList.getPathList().get(pos);
//                String wantDeletePath = mpControl.dBGetPath(pos);
                MyDelectAlertDialog(wantDeleteName,wantDeletePath);
            }
        });
        playAdapter.appendList(listSongName);
        RvItem.setAdapter(playAdapter);

    }

    public ArrayList<String> getAllDataFileName(String folderPath) {
        ArrayList<String> fileList = new ArrayList<>();
        File file = new File(folderPath);
        File[] tempList = file.listFiles();
        assert tempList != null;
        for (File value : tempList) {
            if (value.isFile()) {
//                System.out.println("文件：" + value.getName());
                String fileName = value.getName();
                if (fileName.endsWith(".mp3")) {    //  根据自己的需要进行类型筛选
                    fileList.add(fileName);
                    Log.i("TAG", value.getAbsolutePath());
                    Log.i("TAG", fileName);
                }
            }
        }
        return fileList;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == permission.CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("premission", grantResults[i] + "not");
                    Log.e("premission", permissions[i] + "not");
                }
            }
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {      //判断标志位
                case 1:
                    searchIf=true;
                    dialog.dismiss();
                    BtnFindAll.setText("扫描歌曲");
                    TvSongName.setText("扫描完成");
//                dialog.dismiss();
                    Toast.makeText(MainActivity.this, "扫描本地音乐完成", Toast.LENGTH_SHORT).show();

                    databaseRead();

                    searchIf=false;
                    break;
            }
        }
    };
    public class WorkThread extends Thread {

        @Override
        public void run() {
            super.run();
            displaySongs();
            //从全局池中返回一个message实例，避免多次创建message（如new Message）
            Message msg =Message.obtain();
//            msg.obj = data;
            msg.what=1;   //标志消息的标志
            handler.sendMessage(msg);
        }

    }

    class MyHandler extends Handler {
        public MyHandler() {
        }

        // 子类必须重写此方法,接受数据
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (!ifSeek) {
//                int musicTimeNow = mp.getCurrentPosition();
//                int musicTimeNow = mp.getCurrentPosition();
                musicTimeNow = mpControl.songGetTimeCur();
                showTimeNow = musicTimeNow / 1000 / 60 + ":" + musicTimeNow / 1000 % 60;
                if (stopIf){
                    SeekB.setProgress(0);
                }else {
                    SeekB.setProgress(musicTimeNow);
                }

                songTimeNow.setText(showTimeNow);
//                if (!mp.isPlaying() && ifPlay) {

                    if (Objects.equals(showTimeNow, showTimeAll)) {
                        if (!ifCycle){
                        Toast.makeText(MainActivity.this, "下一曲", Toast.LENGTH_SHORT).show();
                        NextSong();
                        }else {
                            Toast.makeText(MainActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                            playAdapter.setSearchPosition(playAdapter.getSearchPosition());
                        }
                    }



            }

            handlerBar.sendEmptyMessageDelayed(TIMER_MSG, 1000);

        }
    }


    public ArrayList<File> findSong(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File singlefile : files) {
                if (singlefile.isDirectory() && !singlefile.isHidden()) {
                    arrayList.addAll(findSong(singlefile));
                } else {
                    if (singlefile.getName().endsWith(".mp3") || singlefile.getName().endsWith(".wav")) {
                        arrayList.add(singlefile);
//                        BtnFindAll.setText(singlefile.getName());//显示扫描进度
//                        TvSongName.setText(singlefile.getName());
//                        TvSongName.setText("扫描完成");

                    }
                }
            }
        }

        return arrayList;
    }

    public ArrayAdapter<String> songList;
    public List<String> listSong = new ArrayList<String>();
    //    public MyAdapter<String> songListMyTask;
    public MyAdapter<String> adapter;

    public void displaySongs() {
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();//临时变量
        final ArrayList<File> songs = findSong(Environment.getExternalStorageDirectory());
        String[] items = new String[songs.size()];

        String patternName = "[^\\/\\\\]+$";
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(patternName);
        // 现在创建 matcher 对象

//        String deleteTableSql = "delete from songlist";
//        db.execSQL(deleteTableSql);

        myDbList.DeleteTable();
//        mpControl.dBDeleteTable();
        for (int i = 0; i < songs.size(); i++) {
            items[i] = songs.get(i).toString();
//            listSong.add(items[i]);
            values.put("id", i);
            values.put("songPath", items[i]);
            Matcher m = r.matcher(items[i]);
            if (m.find()) {
                String name = m.group(0);
                String nameKey = name.substring(0, name.lastIndexOf("."));
                values.put("songName", nameKey);
//                    System.out.println(" listMap: " + listMap);
            } else {
                System.out.println("NO MATCH");
            }
//            db.insert("songlist", null, values);
            myDbList.insertListSingle(values);
//            mpControl.dBInsertListSingle(values);
        }
//        playListMapAdapter = new PlayListMapAdapter(listSong);




    }

    public void MyAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("扫描本地文件中")//设置对话框的标题
                .setMessage("剩余时间视文件夹数量而定\n扫描完毕会自动关闭本窗口\n也可手动关闭\n请稍等...")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("明白啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "明白就很棒哟", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("好的啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "OK的啦", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();

    }
    public void MyDelectAlertDialog(String wantDeleteName, String wantDeletePath) {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("移除歌曲 "+wantDeleteName)//设置对话框的标题
                .setMessage("是否从列表中移除该歌曲\n（并不会删除本地歌曲）\n（重新扫描歌曲即可恢复）\n歌曲路径："+wantDeletePath)//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("手滑误触", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "那我走？", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确定移除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myDbList.Delete(wantDeleteName);
//                        mpControl.dBDelete(wantDeleteName);
                        Toast.makeText(MainActivity.this, "检测到移除歌曲 "+wantDeleteName, Toast.LENGTH_SHORT).show();
                        databaseRead();
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();

    }

    public void MyOnlineAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("跳转网络寻歌")//设置对话框的标题
                .setMessage("可在跳转的网站下载喜欢的歌曲(选择mp3格式，注意歌曲命名)\n然后使用本软件的扫描歌曲功能以入库本地列表\n接下来享受音乐的旅程吧")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("明白啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "明白就很棒哟", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("好的啦", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "OK的啦", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();

    }

    public void SkinEnable(int pos){
        System.out.println("SkinEnable 1= " + pos);

        if (pos == 0) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
            System.out.println("backDrawOrder 1= " + pos);
        }
        if (pos == 1) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg1));
            System.out.println("backDrawOrder 2= " + pos);
        }
        if (pos == 2) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg2));
        }
        if (pos == 3) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg3));
        }
        if (pos == 4) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg4));
        }
        if (pos == 5) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg5));
        }
        if (pos == 6) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg6));
        }
        if (pos == 7) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg7));

        }
        if (pos == 8) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg8));

        }
        if (pos == 9) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg9));

        }
        if (pos == 10) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg10));

        }
        if (pos == 11) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg11));

        }
        if (pos == 12) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg12));

        }
        if (pos == 13) {
            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg13));
            pos = -1;
        }
//        pos++;

    }

    public void SelectSkin() {
        final String items[] = {"0号皮肤-璀璨", "1号皮肤-奶茶", "2号皮肤-白猫",
                "3号皮肤-绿意", "4号皮肤-橙沙", "5号皮肤-棱角",
                "6号皮肤-奶糖", "7号皮肤-静谧", "8号皮肤-粉黛",
                "9号皮肤-花语", "10号皮肤-梨落", "11号皮肤-云端", "12号皮肤-秋意", "13号皮肤-眠梦"};

        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("预置皮肤列表")//设置对话框的标题
                .setSingleChoiceItems(items, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SkinEnable(which);
//                        System.out.println(" which= " + which);
                        Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();


    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotificationMsg() {



        Intent intentPre = new Intent(PRE_SONG);
        Intent intentNext = new Intent(NEXT_SONG);
        Intent intentPlayPause = new Intent(PLAY_PAUSE_SONG);

        PendingIntent prePendingIntent = PendingIntent.getBroadcast(this, 0, intentPre, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, intentPlayPause, PendingIntent.FLAG_IMMUTABLE);


        // Get the layouts to use in the custom notification
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.remoteview_play);


        notificationLayout.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.remoteview);

        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);


        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Apply the layouts to the notification
        Notification customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setOngoing(true)
                .build();



        manager.notify(1, customNotification);

    }
    private void sendNotificationChangePlay(String nSongName) {

        Intent intentPre = new Intent(PRE_SONG);
        Intent intentNext = new Intent(NEXT_SONG);
        Intent intentPlayPause = new Intent(PLAY_PAUSE_SONG);

        PendingIntent prePendingIntent = PendingIntent.getBroadcast(this, 0, intentPre, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, intentPlayPause, PendingIntent.FLAG_IMMUTABLE);


        // Get the layouts to use in the custom notification
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.remoteview_play);
//        notificationLayout.setTextViewText(R.id.NTvSongNow, nSongName);





        notificationLayout.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.remoteview);
//        notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, nSongName);


        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

//        if (mp.isPlaying()) {
//        if (mpControl.songIsPlaying()) {
            notificationLayout.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
            notificationLayout.setImageViewResource(R.id.NImage, R.drawable.pause);
            notificationLayoutExpanded.setImageViewResource(R.id.NImage, R.drawable.pause);

//        }

//        if (mp.isPlaying()) {
//            notificationLayout.setImageViewResource(R.id.NBtnPlayPause, R.drawable.pause);
//            notificationLayoutExpanded.setImageViewResource(R.id.NBtnPlayPause, R.drawable.pause);
//        } else {
//            notificationLayout.setImageViewResource(R.id.NBtnPlayPause, R.drawable.playsong);
//            notificationLayoutExpanded.setImageViewResource(R.id.NBtnPlayPause, R.drawable.playsong);
//        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Apply the layouts to the notification
        Notification customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setOngoing(true)
                .build();
        manager.notify(1, customNotification);

    }
    private void sendNotificationChangePause(String nSongName) {

        Intent intentPre = new Intent(PRE_SONG);
        Intent intentNext = new Intent(NEXT_SONG);
        Intent intentPlayPause = new Intent(PLAY_PAUSE_SONG);

        PendingIntent prePendingIntent = PendingIntent.getBroadcast(this, 0, intentPre, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, intentPlayPause, PendingIntent.FLAG_IMMUTABLE);


        // Get the layouts to use in the custom notification
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.remoteview_play);
//        notificationLayout.setTextViewText(R.id.NTvSongNow, nSongName);





        notificationLayout.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
//        notificationLayout.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.remoteview);
//        notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, nSongName);


        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
//        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

//        if (mp.isPlaying()) {
//        if (mpControl.songIsPlaying()) {
//            notificationLayout.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
//            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
//            notificationLayout.setImageViewResource(R.id.NImage, R.drawable.pause);
//
//        } else {
            notificationLayout.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
            notificationLayout.setImageViewResource(R.id.NImage, R.drawable.playsong);
            notificationLayoutExpanded.setImageViewResource(R.id.NImage, R.drawable.playsong);
//        }

//        if (mp.isPlaying()) {
//            notificationLayout.setImageViewResource(R.id.NBtnPlayPause, R.drawable.pause);
//            notificationLayoutExpanded.setImageViewResource(R.id.NBtnPlayPause, R.drawable.pause);
//        } else {
//            notificationLayout.setImageViewResource(R.id.NBtnPlayPause, R.drawable.playsong);
//            notificationLayoutExpanded.setImageViewResource(R.id.NBtnPlayPause, R.drawable.playsong);
//        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Apply the layouts to the notification
        Notification customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setOngoing(true)
                .build();
        manager.notify(1, customNotification);

    }


    private void sendRecommendMsg() {

//        Intent intent = new Intent(MainActivity.this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
        RemoteViews remoteViews = getRemoteViews();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
        Notification builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("textTitle")
//                .setContentText("textContent")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
//                .setContent(remoteViews)
                .setCustomContentView(remoteViews)
                .build();

        manager.notify(1, builder);

//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//
//        // notificationId is a unique int for each notification that you must define
//        int notificationId=1;
//        notificationManager.notify(notificationId, builder.build());

    }

    private void toSendBroadcast(){
        Intent intent = new Intent("ysPlay");
        sendBroadcast(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();

        myReceiverIn=new MyReceiverIn();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_PAUSE_SONG);
        intentFilter.addAction(PRE_SONG);
        intentFilter.addAction(NEXT_SONG);
        registerReceiver(myReceiverIn,intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        unregisterReceiver(myReceiver);
        unregisterReceiver(myReceiverIn);
    }

    //    @NotNull
    private RemoteViews getRemoteViews() {
//        Intent intent = new Intent(this, MainActivity.class);
        Intent intentPrev = new Intent("ysPlay");
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, 0);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.remoteview);//RemoteView传入布局
        remoteViews.setTextViewText(R.id.NTvSongNow, "播放歌曲");//设置textView内容
//        remoteViews.setTextViewText(R.id.tv_right, "跳转");//设置textView内容
//        remoteViews.setImageViewResource(R.id.icon, R.drawable.ic_launcher);//设置图片样式
//        remoteViews.setOnClickPendingIntent(R.id.NBtnNext, pendingIntent);//点击跳转事件
//

        //为prev控件注册事件
        remoteViews.setOnClickPendingIntent(R.id.NBtnPre, prevPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.NBtnPlayPause, prevPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.NBtnNext, prevPendingIntent);


        return remoteViews;
    }

    public class MyReceiverIn extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case PLAY_PAUSE_SONG:
                    Log.e("Activity",PLAY_PAUSE_SONG);
                    playOrPause();
                    break;
                case PRE_SONG:
                    Log.e("Activity",PRE_SONG);
                    PreSong();
                    break;
                case NEXT_SONG:
                    Log.e("Activity",NEXT_SONG);
                    NextSong();
                    break;
            }

        }
    }









}

