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
    private TextView songTimeNow;
    private TextView songTimeAll;
    private Button BtnFindAll;
    private EditText ESearchName;
    private Button BtnOrder;

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


    final String CHANNEL_ID = "CHANNEL_ID";
    public static final String PLAY_PAUSE_SONG = "play_pause_song";
    //    static final String PAUSE_SONG="pause_song";
    public static final String NEXT_SONG = "next_song";
    public static final String PRE_SONG = "pre_song";
//    public static final String DB_READ="db_read";

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
        songTimeNow = findViewById(R.id.songTimeNow);
        songTimeAll = findViewById(R.id.songTimeAll);
        ESearchName = findViewById(R.id.ESearchName);
        Button BSearchSong = findViewById(R.id.BSearchSong);
        Button btnPre = findViewById(R.id.BtnPre);
        Button btnNext = findViewById(R.id.BtnNext);
        Button btnQuit = findViewById(R.id.BtnQuit);
        Button btnLocation = findViewById(R.id.BtnLocation);
        BtnOrder = findViewById(R.id.BtnOrder);
        ImageView imCd = findViewById(R.id.ImCd);

        animator = ObjectAnimator.ofFloat(imCd, "rotation", 0, 360.0F);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);

        databaseRead();

        int randomSkin = (int) (Math.random() * 14);
        SkinEnable(randomSkin);

        bthSelect.setOnClickListener(view -> SelectSkin());
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
                ifPlay = false;
//                mp.stop();
                mpControl.songStop();
//                mp.reset();
                stopIf = true;
                BtnPlayPause.setBackgroundResource(R.drawable.playsong);
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
                PositionJump = i;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "开始跳动", Toast.LENGTH_SHORT).show();
                mpControl.songPause();
                ifSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "结束跳动", Toast.LENGTH_SHORT).show();
                mpControl.songStart();
                mpControl.songPositionJump(PositionJump);
                ifSeek = false;

            }
        });
        btnNext.setOnClickListener(view -> NextSong());
        btnPre.setOnClickListener(view -> PreSong());
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyOnlineAlertDialog();
                Uri uri = Uri.parse("https://tools.liumingye.cn/music/#/");
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                intent.setData(uri);
                startActivity(intent);
            }
        });

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
        TvSongName.setText(songNameNow);
        songTimeAll.setText(showTimeAll);
        BtnPlayPause.setBackgroundResource(R.drawable.pause);
        mpControl.setPositionSongName(songNameNow);
        sendNotificationChangePlay(songNameNow);

    }

    private void playOrPause() {
//        if (!mp.isPlaying()) {
        if (!mpControl.songIsPlaying()) {

            if (stopIf) {

                mpControl.songPrepare();
                stopIf = false;
            }


//            mp.start();
            mpControl.songStart();


//            SeekB.setMax(mp.getDuration());
            SeekB.setMax(mpControl.songGetTimeAll());
//                    BtnPlayPause.setText("暂停");
            BtnPlayPause.setBackgroundResource(R.drawable.pause);
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
            BtnPlayPause.setBackgroundResource(R.drawable.playsong);
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
                String song_path = pathList.get(pos);
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
                if (stopIf) {
                    SeekB.setProgress(0);
                } else {
                    SeekB.setProgress(musicTimeNow);
                }

                songTimeNow.setText(showTimeNow);
//                if (!mp.isPlaying() && ifPlay) {

                if (Objects.equals(showTimeNow, showTimeAll)) {
                    if (!ifCycle) {
                        Toast.makeText(MainActivity.this, "下一曲", Toast.LENGTH_SHORT).show();
                        NextSong();
                    } else {
                        Toast.makeText(MainActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();
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
                if (mainA.stopIf) {
                    mainA.SeekB.setProgress(0);
                } else {
                    mainA.SeekB.setProgress(mainA.musicTimeNow);
                }

                mainA.songTimeNow.setText(mainA.showTimeNow);
//                if (!mp.isPlaying() && ifPlay) {

                if (Objects.equals(mainA.showTimeNow, mainA.showTimeAll)) {
                    if (!ifCycle) {
                        Toast.makeText(mainA.getApplicationContext(), "下一曲", Toast.LENGTH_SHORT).show();
                        mainA.NextSong();
                    } else {
                        Toast.makeText(mainA.getApplicationContext(), "单曲循环", Toast.LENGTH_SHORT).show();
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
                    if (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav")) {
                        arrayList.add(singleFile);
                    }
                }
            }
        }

        return arrayList;
    }

    public void displaySongs() {
        ContentValues values = new ContentValues();//临时变量
        final ArrayList<File> songs = findSong(Environment.getExternalStorageDirectory());
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
                    Toast.makeText(MainActivity.this, "明白就很棒哟", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("好的啦", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "OK的啦", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, "那我走？", Toast.LENGTH_SHORT).show();
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

    public void MyOnlineAlertDialog() {
        dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("跳转网络寻歌")//设置对话框的标题
                .setMessage("可在跳转的网站下载喜欢的歌曲(选择mp3格式，注意歌曲命名)\n然后使用本软件的扫描歌曲功能以入库本地列表\n接下来享受音乐的旅程吧")//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("明白啦", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "明白就很棒哟", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setPositiveButton("好的啦", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "OK的啦", Toast.LENGTH_SHORT).show();
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

    }

    public void SelectSkin() {
        final String[] items = {"0号皮肤-璀璨", "1号皮肤-奶茶", "2号皮肤-白猫",
                "3号皮肤-绿意", "4号皮肤-橙沙", "5号皮肤-棱角",
                "6号皮肤-奶糖", "7号皮肤-静谧", "8号皮肤-粉黛",
                "9号皮肤-花语", "10号皮肤-梨落", "11号皮肤-云端", "12号皮肤-秋意", "13号皮肤-眠梦"};

        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("预置皮肤列表")//设置对话框的标题
                .setSingleChoiceItems(items, 1, (dialog1, which) -> {
                    SkinEnable(which);
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

    @Override
    protected void onStart() {
        super.onStart();

        myReceiverIn = new MyReceiverIn();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_PAUSE_SONG);
        intentFilter.addAction(PRE_SONG);
        intentFilter.addAction(NEXT_SONG);
        registerReceiver(myReceiverIn, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        unregisterReceiver(myReceiver);
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

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PLAY_PAUSE_SONG:
                    Log.e("Activity", PLAY_PAUSE_SONG);
                    playOrPause();
                    break;
                case PRE_SONG:
                    Log.e("Activity", PRE_SONG);
                    PreSong();
                    break;
                case NEXT_SONG:
                    Log.e("Activity", NEXT_SONG);
                    NextSong();
                    break;
            }

        }
    }


}

