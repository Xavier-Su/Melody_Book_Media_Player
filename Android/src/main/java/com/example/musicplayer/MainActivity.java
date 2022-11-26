package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    public ListDatabase myDbList;

    private LinearLayout MainLayout;
    private MyReceiverIn myReceiverIn;

    private SeekBar SeekB;
    private TextView TvSongNow;
    private TextView TvSongName;
    private Button BtnPlayPause;

    private RecyclerView RvItem;
    private TextView TSongTimeNow;
    private TextView TSongTimeAll;
    private Button BtnFindAll;
    private EditText ESearchName;
    private Button BtnOrder;
    private Button BtnTimer;
    private Button BtnPicture;
    private TextView TTimer;

    private PlayAdapter playAdapter;
    private ObjectAnimator animator;

    private Permission permission;
    private MyHandler handlerBar;
    static public Boolean ifSeek = false;
    static public Boolean ifCycle = false;
    public int positionCur;

    public File file;
    final int TIMER_MSG = 0X001;
    public int musicTimeAll;
    public int musicTimeNow;
    public String showTimeAll;
    public String showTimeNow;

    public int timerLong=0;
    public int timerPrepare=0;


    final String CHANNEL_ID = "CHANNEL_ID";

    public static final String PLAY_PAUSE_SONG="play_pause_song";
    public static final String NEXT_SONG="next_song";
    public static final String PRE_SONG="pre_song";
    public static final String DB_READ="db_read";

    public static final String PLAY_PAUSE_SONG_MAIN = "play_pause_song_main";
    //    static final String PAUSE_SONG="pause_song";
    public static final String NEXT_SONG_MAIN  = "next_song_main";
    public static final String PRE_SONG_MAIN  = "pre_song_main";
    public static final String CLOSE_SONG_MAIN  = "close_song_main";
//    public static final String DB_READ="db_read";

    private int songType=0;
    private int skinPos=1;

    private MyService.MpControl mpControl;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mpControl = (MyService.MpControl) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    public List<String> listSongName = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    private String songNameNow = "";
    private AlertDialog dialog;
    public boolean stopIf = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MusicPlayer);
        super.onCreate(savedInstanceState);
        permission = new Permission();
        permission.checkerPermission(this);

        initReceiver();

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

        Intent intent = new Intent(this, MyService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

//        handlerBar = new MyHandler();
        handlerBar = new MyHandler(MainActivity.this);

        MainLayout = (LinearLayout) findViewById(R.id.layout);


        RvItem = findViewById(R.id.RvItem);
        RvItem.setItemViewCacheSize(0);

        BtnPlayPause = findViewById(R.id.BtnPlayPause);
        Button btnStop = findViewById(R.id.BtnStop);
        Button bthSelect = findViewById(R.id.BtnSelect);
        BtnFindAll = findViewById(R.id.BtnFindAll);
        SeekB = findViewById(R.id.MusicSeekBar);
        TvSongNow = findViewById(R.id.TvSongNow);
        TvSongName = findViewById(R.id.TvSongName);
        TSongTimeNow = findViewById(R.id.TSongTimeNow);
        TSongTimeAll = findViewById(R.id.TSongTimeAll);
        ESearchName = findViewById(R.id.ESearchName);
        Button BSearchSong = findViewById(R.id.BSearchSong);
        Button btnPre = findViewById(R.id.BtnPre);
        Button btnNext = findViewById(R.id.BtnNext);
        Button btnQuit = findViewById(R.id.BtnQuit);
        Button btnLocation = findViewById(R.id.BtnLocation);
        BtnOrder = findViewById(R.id.BtnOrder);
        BtnTimer = findViewById(R.id.BtnTimer);
        BtnPicture = findViewById(R.id.BtnPicture);
        TTimer = findViewById(R.id.TTimer);
        ImageView imCd = findViewById(R.id.ImCd);

        animator = ObjectAnimator.ofFloat(imCd, "rotation", 0, 360.0F);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);

        databaseRead();

        int randomSkin = (int) (Math.random() * 20);
        skinPos=randomSkin;
        SkinEnable(randomSkin);

        bthSelect.setOnClickListener(view -> SelectSkin());

        BtnTimer.setOnClickListener(view -> {
            final String[] items = {"不定时", "5分钟", "10分钟",
                    "15分钟", "20分钟", "25分钟",
                    "30分钟", "35分钟", "40分钟",
                    "45分钟","50分钟"};
            AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                    .setTitle("选择时间定时关闭")//设置对话框的标题
                    .setSingleChoiceItems(items, 0, (dialog1, which) -> {
                        timerPrepare=which*5*60;
//                        timerPrepare=which*10;

                        Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton("好了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            timerLong=timerPrepare;
                            mpControl.setTimerLong(timerLong);


                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        });
        BtnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ifCycle = !ifCycle;
                if (ifCycle) {
                    Toast.makeText(MainActivity.this, "单曲循环模式", Toast.LENGTH_SHORT).show();
                    BtnOrder.setBackgroundResource(R.drawable.cycle);
                } else {
                    Toast.makeText(MainActivity.this, "列表循环模式", Toast.LENGTH_SHORT).show();
                    BtnOrder.setBackgroundResource(R.drawable.order);
                }
            }
        });
        btnLocation.setOnClickListener(view -> {
            RvItem.smoothScrollToPosition(playAdapter.getSearchPosition());
//                toSendBroadcast();
        });

        BSearchSong.setOnClickListener(view -> {
            String searchName = ESearchName.getText().toString();
            playAdapter.getFilter().filter(searchName);
            ESearchName.setText("");
        });
        ESearchName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
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

        BtnPlayPause.setOnClickListener(view -> playOrPause());

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MyOnlineVideoAlertDialog();

//                ifPlay = false;
////                mp.stop();
//                mpControl.songStop();
////                mp.reset();
//                stopIf = true;
//                BtnPlayPause.setBackgroundResource(R.drawable.playsong);
//                TvSongNow.setText("停止播放：");
//                TvSongName.setText("暂未播放");
//                SeekB.setProgress(0);
//                animator.pause();
            }
        });
        BtnFindAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String[] items = {"我全都要", "仅MP3", "仅WAV",
                        "仅FLAC", "仅M4A", "隐藏选项-MP4听声"};

                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                        .setTitle("请选择扫描歌曲类型")//设置对话框的标题
                        .setSingleChoiceItems(items, songType, (dialog1, which) -> {
                            songType=which;
                            Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("手滑误触", (dialog2, which) -> {
//                            Toast.makeText(MainActivity.this, "那我走？", Toast.LENGTH_SHORT).show();
                            dialog2.dismiss();
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                                TvSongName.setText("扫描本地音乐中...");
                                BtnFindAll.setText("扫描中...");
                                MyAlertDialog();
                                Toast.makeText(MainActivity.this, "扫描本地音乐中...", Toast.LENGTH_SHORT).show();
                                new WorkThread().start();
                            }
                        }).create();
                dialog.show();



//                TvSongName.setText("扫描本地音乐中...");
//                BtnFindAll.setText("扫描中...");
//                MyAlertDialog();
//                songType=0;
//                Toast.makeText(MainActivity.this, "扫描本地音乐中...", Toast.LENGTH_SHORT).show();
//                new WorkThread().start();

            }
        });

        SeekB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int PositionJump = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                PositionJump = i;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(MainActivity.this, "开始跳动", Toast.LENGTH_SHORT).show();
                mpControl.songPause();
                ifSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(MainActivity.this, "结束跳动", Toast.LENGTH_SHORT).show();
                mpControl.songStart();
                mpControl.songPositionJump(PositionJump);
                ifSeek = false;

            }
        });
//        btnNext.setOnClickListener(view -> NextSong());
        btnNext.setOnClickListener(view ->
        {   mpControl.NextSong();
            TvSongNow.setText("正在播放：");
            TvSongName.setText(mpControl.getPositionSongNameNow());

        });
//        btnPre.setOnClickListener(view -> PreSong());
        btnPre.setOnClickListener(view -> mpControl.PreSong());
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyOnlineMusicAlertDialog();

            }
        });
        BtnPicture.setOnClickListener(view -> MyOnlinePictureAlertDialog());

    }

    static public boolean ifPlay = false;

    private void play() {
//        mp.reset();
        mpControl.songRest();

        SeekB.setProgress(0);
        animator.start();

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
//        songNameNow = mpControl.getPositionSongNameNow();

        TvSongName.setText(songNameNow);
        TSongTimeAll.setText(showTimeAll);
        BtnPlayPause.setBackgroundResource(R.drawable.pause);
        mpControl.setPositionSongName(songNameNow);
        sendNotificationChangePlay(songNameNow);

    }
    private void playViaName(String songName) {
//        mp.reset();
        mpControl.songRest();

        animator.start();

        mpControl.songPlayViaName(songName);
//        mpControl.songStart();
//            int musicTimeAll = mp.getDuration();
        musicTimeAll = mpControl.songGetTimeAll();

        showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;
        SeekB.setMax(musicTimeAll);
        SeekB.setProgress(0);
        TSongTimeAll.setText(showTimeAll);
//        System.out.println("songTimeAll"+showTimeAll+"\nmusicTimeAll"+musicTimeAll);
        TvSongNow.setText("正在播放：");
        TvSongName.setText(mpControl.getPositionSongNameNow());
        BtnPlayPause.setBackgroundResource(R.drawable.pause);
        handlerBar.sendEmptyMessage(TIMER_MSG);
////            mp.start();
//
//
//        ifPlay = true;
//        stopIf = false;
////            BtnPlayPause.setText("暂停");
//        songNameNow = file.getName();
////        songNameNow = mpControl.getPositionSongNameNow();
//
//        TvSongName.setText(songNameNow);
//        TSongTimeAll.setText(showTimeAll);
//        BtnPlayPause.setBackgroundResource(R.drawable.pause);
//        mpControl.setPositionSongName(songNameNow);
//        sendNotificationChangePlay(songNameNow);

    }

    private void playOrPause() {
//        if (!mp.isPlaying()) {
        mpControl.PlayOrPause();

//        if (!mpControl.songIsPlaying()) {
//            if (stopIf) {
//                mpControl.songPrepare();
//                stopIf = false;
//            }
//            mpControl.songStart();
//            SeekB.setMax(mpControl.songGetTimeAll());
////                    BtnPlayPause.setText("暂停");
//            BtnPlayPause.setBackgroundResource(R.drawable.pause);
//            TvSongNow.setText("正在播放：");
//            TvSongName.setText(songNameNow);
//            mpControl.setPositionSongName(songNameNow);
//            animator.resume();
//            ifPlay = true;
//        } else {
//            ifPlay = false;
////            mp.pause();
//            mpControl.songPause();
////                    BtnPlayPause.setText("播放");
//            BtnPlayPause.setBackgroundResource(R.drawable.playsong);
//            TvSongNow.setText("暂停播放：");
//            animator.pause();
//            mpControl.setPositionSongName(songNameNow);
//            sendNotificationChangePause(songNameNow);
//
//        }

    }

    public void NextSong() {

        int countSong = playAdapter.getItemCount();
//        int nowCur = positionCur;
        int nowCur = playAdapter.getSearchPosition();
        System.out.println("countSong = " + countSong);
        System.out.println("nowCur = " + nowCur);
//                Drawable drawable=getResources().getDrawable(R.drawable.select_color);

        if (nowCur < countSong - 1) {
            nowCur++;
            playAdapter.setSearchPosition(nowCur);
        } else if (nowCur == countSong - 1) {
            nowCur = 0;
            playAdapter.setSearchPosition(nowCur);
        }
//        playAdapter.mListener.onClick(nowCur);
    }

    public void PreSong() {
        int countSong = playAdapter.getItemCount();
//        int nowCur = positionCur;
        int nowCur = playAdapter.getSearchPosition();

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

        playAdapter = new PlayAdapter(MainActivity.this, pathList, listSongName, new PlayAdapter.OnItemClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(int pos) {
//                        Toast.makeText(MainActivity.this, "click = "+pos,Toast.LENGTH_SHORT).show();
                String songName=listSongName.get(pos);
                playAdapter.setmPosition(pos);
                playViaName(songName);


//                String song_path = pathList.get(pos);
//                positionCur = pos;
//                file = new File(song_path);
//                playAdapter.setmPosition(pos);
//
//                mpControl.setPositionSongNow(pos);
//                mpControl.setPositionSongCount(playAdapter.getItemCount());

                playAdapter.notifyDataSetChanged();
                sendNotificationChangePlay(songName);
//                play();
            }

            @Override
            public void onLongClick(int pos) {
                String wantDeleteName = myDbList.getNameList().get(pos);
//                String wantDeleteName = mpControl.dBGetName(pos);
                String wantDeletePath = myDbList.getPathList().get(pos);
//                String wantDeletePath = mpControl.dBGetPath(pos);
                MyDeleteAlertDialog(wantDeleteName, wantDeletePath);
            }
        });
        playAdapter.appendList(listSongName);
        RvItem.setAdapter(playAdapter);

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == permission.CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("preMission", grantResults[i] + "not");
                    Log.e("preMission", permissions[i] + "not");
                }
            }
        }
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
//            super.handleMessage(msg);
            //判断标志位
            if (msg.what == 1) {

                dialog.dismiss();
                BtnFindAll.setText("扫描歌曲");
                TvSongName.setText("扫描完成");
//                dialog.dismiss();
                Toast.makeText(MainActivity.this, "扫描本地音乐完成", Toast.LENGTH_SHORT).show();
                databaseRead();

            }
            return false;
        }
    });

    public class WorkThread extends Thread {

        @Override
        public void run() {
            super.run();
            displaySongs();
            //从全局池中返回一个message实例，避免多次创建message（如new Message）
            Message msg = Message.obtain();
            msg.what = 1;   //标志消息的标志
            handler.sendMessage(msg);
        }

    }

    class MyHandler1 extends Handler {


        // 子类必须重写此方法,接受数据
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (!ifSeek) {
                musicTimeNow = mpControl.songGetTimeCur();
                showTimeNow = musicTimeNow / 1000 / 60 + ":" + musicTimeNow / 1000 % 60;
                int timerLongPre=mpControl.getTimerLong();
                String timerAct=timerLongPre / 60 + ":" + timerLongPre % 60;

                if (stopIf) {
                    SeekB.setProgress(0);
                } else {
                    SeekB.setProgress(musicTimeNow);
                }
                TTimer.setText(timerAct);

                TSongTimeNow.setText(showTimeNow);
//                if (!mp.isPlaying() && ifPlay) {

                if (Objects.equals(showTimeNow, showTimeAll)) {
                    if (!ifCycle) {
//                        Toast.makeText(MainActivity.this, "下一曲", Toast.LENGTH_SHORT).show();
                        NextSong();
                    } else {
//                        Toast.makeText(MainActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
                        playAdapter.setSearchPosition(playAdapter.getSearchPosition());
                    }
                }


            }

            handlerBar.sendEmptyMessageDelayed(TIMER_MSG, 1000);

        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mTarget;

        public MyHandler(MainActivity activity) {
            mTarget = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
//            Log.e("myHandler", "change textview");
            MainActivity mainA = mTarget.get();

            if (!ifSeek) {
                mainA.musicTimeNow = mainA.mpControl.songGetTimeCur();
                mainA.showTimeNow = mainA.musicTimeNow / 1000 / 60 + ":" + mainA.musicTimeNow / 1000 % 60;
                int timerLongPre=mainA.mpControl.getTimerLong();
//                System.out.println("timerLongPre = "+timerLongPre);
                String timerAct="剩余时间\n"+timerLongPre / 60 + ":" + timerLongPre % 60;
                if (mainA.stopIf) {
                    mainA.SeekB.setProgress(0);
                } else {
                    mainA.SeekB.setProgress(mainA.musicTimeNow);
                }
                mainA.TTimer.setText(timerAct);
//                System.out.println("timerAct = "+timerAct);
                mainA.TSongTimeNow.setText(mainA.showTimeNow);
//                if (!mp.isPlaying() && ifPlay) {

                if (Objects.equals(mainA.showTimeNow, mainA.showTimeAll)) {
                    if (!ifCycle) {
//                        Toast.makeText(mainA.getApplicationContext(), "下一曲", Toast.LENGTH_SHORT).show();
                        mainA.NextSong();
                    } else {
//                        Toast.makeText(mainA.getApplicationContext(), "单曲循环", Toast.LENGTH_SHORT).show();
                        mainA.playAdapter.setSearchPosition(mainA.playAdapter.getSearchPosition());
                    }
                }

            }
            mainA.handlerBar.sendEmptyMessageDelayed(mainA.TIMER_MSG, 1000);


        }
    }

    public ArrayList<File> findSong(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findSong(singleFile));
                } else {
                    if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".flac")|| singleFile.getName().endsWith(".wav")) {
                        arrayList.add(singleFile);
                    }
                }
            }
        }

        return arrayList;
    }

    public ArrayList<File> findSongType(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findSongType(singleFile));
                } else {
                    switch (songType){
                        case 0:
                            if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".flac")|| singleFile.getName().endsWith(".wav")|| singleFile.getName().endsWith(".m4a")) {
                                arrayList.add(singleFile);
                            }
                            break;
                        case 1:
                            if (singleFile.getName().endsWith(".mp3")) {
                                arrayList.add(singleFile);
                            }
                            break;
                        case 2:
                            if (singleFile.getName().endsWith(".wav")) {
                                arrayList.add(singleFile);
                            }
                            break;
                        case 3:
                            if (singleFile.getName().endsWith(".flac")) {
                                arrayList.add(singleFile);
                            }
                            break;
                        case 4:
                            if (singleFile.getName().endsWith(".m4a")) {
                                arrayList.add(singleFile);
                            }
                            break;
                        case 5:
                            if (singleFile.getName().endsWith(".mp4")) {
                                arrayList.add(singleFile);
                            }
                            break;
                    }
//                    if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".flac")|| singleFile.getName().endsWith(".wav")) {
//                        arrayList.add(singleFile);
//                    }
                }
            }
        }

        return arrayList;
    }

    public void displaySongs() {
        ContentValues values = new ContentValues();//临时变量
//        final ArrayList<File> songs = findSong(Environment.getExternalStorageDirectory());
        final ArrayList<File> songs = findSongType(Environment.getExternalStorageDirectory());
        String[] items = new String[songs.size()];

//        String patternName = "[^\\/\\\\]+$";
        String patternName = "[^/\\\\]+$";
        // 创建 Pattern 对象
        Pattern r = Pattern.compile(patternName);
        // 现在创建 matcher 对象

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
                assert name != null;
                String nameKey = name.substring(0, name.lastIndexOf("."));
                values.put("songName", nameKey);
            } else {
                System.out.println("NO MATCH");
            }
            myDbList.insertListSingle(values);

        }


    }

    public void MyAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("扫描本地文件中")//设置对话框的标题
                .setMessage("剩余时间视文件夹数量而定\n扫描完毕会自动关闭本窗口\n也可手动关闭\n请稍等...")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("明白啦", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "明白就很棒哟", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("好的啦", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "OK的啦", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).create();
        dialog.show();

    }

    public void MyDeleteAlertDialog(String wantDeleteName, String wantDeletePath) {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("移除歌曲 " + wantDeleteName)//设置对话框的标题
                .setMessage("是否从列表中移除该歌曲\n（并不会删除本地歌曲）\n（重新扫描歌曲即可恢复）\n歌曲路径：" + wantDeletePath)//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("手滑误触", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "那我走？", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("确定移除", (dialog, which) -> {
                    myDbList.Delete(wantDeleteName);
//                        mpControl.dBDelete(wantDeleteName);
                    Toast.makeText(MainActivity.this, "检测到移除歌曲 " + wantDeleteName, Toast.LENGTH_SHORT).show();
                    databaseRead();
                    dialog.dismiss();
                }).create();
        dialog.show();

    }

    public void MyOnlineMusicAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("是否跳转浏览器进行网络寻歌")//设置对话框的标题
                .setMessage("可在跳转的网站下载喜欢的歌曲(可选择mp3/wav/flac/m4a格式，注意歌曲命名)\n然后使用本软件的扫描歌曲功能以入库本地列表\n接下来享受音乐的旅程吧\n(不可相信广告)")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("手滑取消", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "返回", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("立即前往", (dialog, which) -> {
                    Uri uri = Uri.parse("https://tools.liumingye.cn/music/#/");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "向前进！", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).create();
        dialog.show();

    }
    public void MyOnlineVideoAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("是否跳转浏览器进行视频观看")//设置对话框的标题
                .setMessage("本软件将跳转去一个比较好用的视频网站\n可以在里面搜索想要看的视频\n免责申明：(不要相信里面的广告！！！)\n(不要乱点广告链接！！！)")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("手滑取消", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "返回", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("立即前往", (dialog, which) -> {
                    Uri uri = Uri.parse("https://cupfox.app/");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "观影模式", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).create();
        dialog.show();
    }
    public void MyOnlinePictureAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("是否跳转浏览器进行在线寻图")//设置对话框的标题
                .setMessage("本软件将跳转去一个比较高质量的图片壁纸网站\n可以在里面保存想要的图片)")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("手滑取消", (dialog, which) -> {
//                    Toast.makeText(MainActivity.this, "返回", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("立即前往", (dialog, which) -> {
                    Uri uri = Uri.parse("https://wallspic.com/");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "看图模式", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).create();
        dialog.show();
    }

    public void SkinEnable(int pos) {
        System.out.println("SkinEnable = " + pos);

        if (pos == 0) {
//            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
            MainLayout.setBackgroundResource(R.drawable.bg);
        }
        if (pos == 1) {
//            MainLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg1));
            MainLayout.setBackgroundResource(R.drawable.bg1);
        }
        if (pos == 2) {
            MainLayout.setBackgroundResource(R.drawable.bg2);
        }
        if (pos == 3) {
            MainLayout.setBackgroundResource(R.drawable.bg3);
        }
        if (pos == 4) {
            MainLayout.setBackgroundResource(R.drawable.bg4);
        }
        if (pos == 5) {
            MainLayout.setBackgroundResource(R.drawable.bg5);
        }
        if (pos == 6) {
            MainLayout.setBackgroundResource(R.drawable.bg6);
        }
        if (pos == 7) {
            MainLayout.setBackgroundResource(R.drawable.bg7);

        }
        if (pos == 8) {
            MainLayout.setBackgroundResource(R.drawable.bg8);

        }
        if (pos == 9) {
            MainLayout.setBackgroundResource(R.drawable.bg9);

        }
        if (pos == 10) {
            MainLayout.setBackgroundResource(R.drawable.bg10);

        }
        if (pos == 11) {
            MainLayout.setBackgroundResource(R.drawable.bg11);

        }
        if (pos == 12) {
            MainLayout.setBackgroundResource(R.drawable.bg12);

        }
        if (pos == 13) {
            MainLayout.setBackgroundResource(R.drawable.bg13);
        }
        if (pos == 14) {
            MainLayout.setBackgroundResource(R.drawable.bg14);
        }
        if (pos == 15) {
            MainLayout.setBackgroundResource(R.drawable.bg15);
        }
        if (pos == 16) {
            MainLayout.setBackgroundResource(R.drawable.bg16);
        }
        if (pos == 17) {
            MainLayout.setBackgroundResource(R.drawable.bg17);
        }
        if (pos == 18) {
            MainLayout.setBackgroundResource(R.drawable.bg18);
        }
        if (pos == 19) {
            MainLayout.setBackgroundResource(R.drawable.bg19);
        }
        if (pos == 20) {
            MainLayout.setBackgroundResource(R.drawable.bg20);
        }

    }

    public void SelectSkin() {
        final String[] items = {"0号皮肤-璀璨", "1号皮肤-奶茶", "2号皮肤-白猫",
                "3号皮肤-绿意", "4号皮肤-橙沙", "5号皮肤-画盘",
                "6号皮肤-奶糖", "7号皮肤-静谧", "8号皮肤-粉黛",
                "9号皮肤-千山", "10号皮肤-梨落", "11号皮肤-云端",
                "12号皮肤-秋意", "13号皮肤-眠梦", "14号皮肤-雪松",
                "15号皮肤-曲径", "16号皮肤-画境", "17号皮肤-雪晶", "18号皮肤-饼干", "19号皮肤-绿缀", "20号皮肤-嘻嘻"};

        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("预置皮肤列表")//设置对话框的标题
                .setSingleChoiceItems(items, skinPos, (dialog1, which) -> {
                    skinPos=which;
                    SkinEnable(which);
                    dialog1.dismiss();
                    Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
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

        notificationLayout.setTextViewText(R.id.NTvSongNow, "播放：" + nSongName);
        notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "播放：" + nSongName);
        notificationLayout.setImageViewResource(R.id.NImage, R.drawable.pause);
        notificationLayoutExpanded.setImageViewResource(R.id.NImage, R.drawable.pause);

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

        notificationLayout.setTextViewText(R.id.NTvSongNow, "暂停：" + nSongName);
        notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "暂停：" + nSongName);
        notificationLayout.setImageViewResource(R.id.NImage, R.drawable.playsong);
        notificationLayoutExpanded.setImageViewResource(R.id.NImage, R.drawable.playsong);


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

    }

    private void toSendBroadcast() {
        Intent intent = new Intent("ysPlay");
        sendBroadcast(intent);
    }
    private void initReceiver(){
        myReceiverIn = new MyReceiverIn();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_PAUSE_SONG_MAIN);
        intentFilter.addAction(PRE_SONG_MAIN);
        intentFilter.addAction(NEXT_SONG_MAIN);
        intentFilter.addAction(CLOSE_SONG_MAIN);
        registerReceiver(myReceiverIn, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onStop() {
        super.onStop();
//        unregisterReceiver(myReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiverIn);
    }

    //    @NotNull
    private RemoteViews getRemoteViews() {

        Intent intentPrev = new Intent("ysPlay");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, intentPrev, PendingIntent.FLAG_IMMUTABLE);

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

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PLAY_PAUSE_SONG_MAIN:
                    Log.e("Activity", PLAY_PAUSE_SONG_MAIN);
//                    playOrPause();

                    if (mpControl.songIsPlaying()){
                        TvSongNow.setText("正在播放：");
                        BtnPlayPause.setBackgroundResource(R.drawable.pause);
                        animator.resume();
                    }else {
                        TvSongNow.setText("暂停播放：");
                        BtnPlayPause.setBackgroundResource(R.drawable.playsong);
                        animator.pause();
                    }

                    TvSongName.setText(mpControl.getPositionSongNameNow());
                    break;
                case PRE_SONG_MAIN:
                    Log.e("Activity", PRE_SONG_MAIN);

                    musicTimeAll = mpControl.songGetTimeAll();
                    showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;
//                    SeekB.setProgress(0);
                    SeekB.setMax(musicTimeAll);
                    TSongTimeAll.setText(showTimeAll);


                    playAdapter.setmPosition(mpControl.getPositionSongNow());
                    playAdapter.notifyDataSetChanged();

                    TvSongNow.setText("正在播放：");
                    TvSongName.setText(mpControl.getPositionSongNameNow());
                    BtnPlayPause.setBackgroundResource(R.drawable.pause);
                    animator.start();
//                    PreSong();
                    break;
                case NEXT_SONG_MAIN:
                    Log.e("Activity", NEXT_SONG_MAIN);
//                    int countSong = mpControl.getPositionSongCount();
////        int nowCur = positionCur;
//                    int nowCur = mpControl.getPositionSongNow();
//                    String songName="";
//                    System.out.println("activity countSong = " + countSong);
//                    System.out.println("activity nowCur = " + nowCur);
////             System.out.println("dBGetPath(nowCur) = " + dBGetPath(nowCur));
////                Drawable drawable=getResources().getDrawable(R.drawable.select_color);
//
//                    if (nowCur < countSong-1) {
//                        nowCur++;
//                        songName=mpControl.dBGetName(nowCur);
//                        playAdapter.setmPosition(nowCur);
//                        playAdapter.notifyDataSetChanged();
//
//
//                    } else if (nowCur == countSong-1) {
//                        nowCur = 0;
//                        songName=mpControl.dBGetName(nowCur);
//                        playAdapter.setmPosition(nowCur);
//                        playAdapter.notifyDataSetChanged();
//                    }
                    musicTimeAll = mpControl.songGetTimeAll();
                    showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;
//                    SeekB.setProgress(0);
                    SeekB.setMax(musicTimeAll);
                    TSongTimeAll.setText(showTimeAll);

                    playAdapter.setmPosition(mpControl.getPositionSongNow());
                    playAdapter.notifyDataSetChanged();
                    TvSongNow.setText("正在播放：");
                    TvSongName.setText(mpControl.getPositionSongNameNow());
                    BtnPlayPause.setBackgroundResource(R.drawable.pause);
                    animator.start();
//                    NextSong();
                    break;
                case CLOSE_SONG_MAIN:
                    Log.e("Activity", CLOSE_SONG_MAIN);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();

//                    finish();
                    System.exit(0);
                    break;
            }

        }
    }


}

