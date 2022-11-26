package com.example.musicplayer;

import static com.example.musicplayer.MainActivity.ifCycle;
import static com.example.musicplayer.MainActivity.ifSeek;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class MyService extends Service {

    @SuppressLint("StaticFieldLeak")
    public ListDatabase myDbList;
    private MyService.MyHandler handlerBar;

    public int positionSongNow=0;
    public int positionSongCount=0;
    public String  positionSongName ="";
    public int timerLong=0;

    public int musicTimeAll;
    public int musicTimeNow;
    public int musicTimeOver;
    public String showTimeOver;
    public String showTimeAll;
    public String showTimeNow;

    final String CHANNEL_ID="CHANNEL_ID";
    final int TIMER_MSG = 0X001;
    public static final String PLAY_PAUSE_SONG="play_pause_song";
    public static final String NEXT_SONG="next_song";
    public static final String PRE_SONG="pre_song";
    public static final String DB_READ="db_read";

    public static final String PLAY_PAUSE_SONG_MAIN = "play_pause_song_main";
    public static final String NEXT_SONG_MAIN  = "next_song_main";
    public static final String PRE_SONG_MAIN  = "pre_song_main";
    public static final String CLOSE_SONG_MAIN  = "close_song_main";

    public MediaPlayer mp;
    private MyService.MyReceiverIn myReceiverIn;
    private MpControl mpControl;

    @Override
    public void onCreate() {
        super.onCreate();
        mp = new MediaPlayer();
        myDbList = new ListDatabase(this);

        mpControl=new MpControl();

//        System.out.println("service");
        Log.d("123456", "new myDbList: ");
        initReceiver();
        createNotificationChannel();
//        sendNotificationMsg();
        Intent intentDbRead = new Intent(DB_READ);
        sendBroadcast(intentDbRead);

        handlerBar = new MyHandler();

        handlerBar.sendEmptyMessage(TIMER_MSG);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MpControl();
    }
    public MyService(){
//        mp = new MediaPlayer();

    }

     class MpControl extends Binder{


         public void setTimerLong(int timerNum) {
             timerLong = timerNum;
         }
         public int getTimerLong() {
             return timerLong;
         }
         public String getPositionSongNameNow(){
            return positionSongName;
         }

         public void setPositionSongNow(int positionNow) {
             positionSongNow = positionNow;
         }
         public int getPositionSongNow() {
             return positionSongNow;
         }
         public int getPositionSongCount() {
             return positionSongCount;
         }

         public void setPositionSongName(String positionName) {
             positionSongName = positionName;
         }

         public void setPositionSongCount(int positionCount) {
             positionSongCount = positionCount;
         }

         public void songPlayViaName(String songName){

             try {
                 String filePath=dBGetPathViaName(songName);


                 mp.reset();
                 mp.setDataSource(filePath);
                 mp.prepare();
                 positionSongName=songName;

                 musicTimeAll = mp.getDuration();
                 musicTimeOver=musicTimeAll-1000;
                 showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;
                 showTimeOver = musicTimeOver / 1000 / 60 + ":" + musicTimeOver / 1000 % 60;
                 mp.start();
                 positionSongNow=myDbList.getPosViaName(songName);
                 positionSongCount=myDbList.getPosCount();

             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
         public void songPlay(String filePath){

             try {
                 mp.reset();
                 mp.setDataSource(filePath);
                 mp.prepare();
                 musicTimeAll = mp.getDuration();
                 musicTimeOver=musicTimeAll-1000;
                 showTimeAll = musicTimeAll / 1000 / 60 + ":" + musicTimeAll / 1000 % 60;
                 showTimeOver = musicTimeOver / 1000 / 60 + ":" + musicTimeOver / 1000 % 60;

             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
        public boolean songIsPlaying(){
            return mp.isPlaying();
        }
        public void songStart(){
            mp.start();
        }
        public void songPause(){
            mp.pause();
        }
        public void songStop(){
            mp.stop();
        }
        public void songRest(){
            mp.reset();
        }
        public void songPrepare(){
            try {
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public int songGetTimeAll(){
            musicTimeAll=mp.getDuration();
            return musicTimeAll;
        }
        public int songGetTimeCur(){
             musicTimeNow=mp.getCurrentPosition();
            return musicTimeNow;
        }
        public void songPositionJump(int PositionJump){
            mp.seekTo(PositionJump);
        }
        public void songCycle(){
            mp.seekTo(0);
            mp.start();
        }

        public void PlayOrPause(){
            if (mp.isPlaying()){
                mp.pause();

                sendNotificationChangePause(positionSongName);
            }else {
                mp.start();
                sendNotificationChangePlay(positionSongName);
            }
            Intent intent = new Intent(PLAY_PAUSE_SONG_MAIN);
            sendBroadcast(intent);
        }
         public void PreSong() {
             int countSong = positionSongCount;
             int nowCur = positionSongNow;

            if (nowCur == 0) {
                 nowCur = countSong - 1;
                positionSongNow=nowCur;
                positionSongName=dBGetName(nowCur);
                mpControl.songPlay(dBGetPath(nowCur));
                mpControl.songStart();

             } else if (nowCur <= countSong && nowCur > 0) {
                 nowCur--;
                positionSongNow=nowCur;
                positionSongName=dBGetName(nowCur);
                mpControl.songPlay(dBGetPath(nowCur));
                mpControl.songStart();

             }
             Intent intent = new Intent(PRE_SONG_MAIN);
             sendBroadcast(intent);
             sendNotificationChangePlay(positionSongName);



         }
         public void NextSong() {

             int countSong = positionSongCount;
//        int nowCur = positionCur;
             int nowCur = positionSongNow;
             System.out.println("countSong = " + countSong);
             System.out.println("nowCur = " + nowCur);
//             System.out.println("dBGetPath(nowCur) = " + dBGetPath(nowCur));
//                Drawable drawable=getResources().getDrawable(R.drawable.select_color);

             if (nowCur < countSong-1) {
                 nowCur++;
                 positionSongNow=nowCur;
                 positionSongName=dBGetName(nowCur);
                 mpControl.songPlay(dBGetPath(nowCur));
                 mpControl.songStart();

             } else if (nowCur == countSong-1) {
                 nowCur = 0;
                 positionSongNow=nowCur;
                 positionSongName=dBGetName(nowCur);
                 mpControl.songPlay(dBGetPath(nowCur));
                 mpControl.songStart();

             }
             Intent intent = new Intent(NEXT_SONG_MAIN);
             sendBroadcast(intent);
             sendNotificationChangePlay(positionSongName);
//        playAdapter.mListener.onClick(nowCur);
         }


        public void dBDeleteTable(){
            myDbList.DeleteTable();

        }
        public void dBInsertListSingle(ContentValues values){
            myDbList.insertListSingle(values);
        }
        public void dBDelete(String wantDeleteName){
            myDbList.Delete(wantDeleteName);
        }
        public List<String> dBGetPathList(){

            return myDbList.getPathList();
        }
         public List<String> dBGetNameList(){

             return myDbList.getNameList();
         }
        public String dBGetPath(int pos){

            return myDbList.getPathList().get(pos);
        }
        public String dBGetName(int pos){

            return myDbList.getNameList().get(pos);
        }
        public String dBGetPathViaName(String songName){

            return myDbList.getPathViaName(songName);
        }

    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
//            Log.e("myHandler", "handleMessage");
            if (timerLong>1){timerLong--;}
            else if (timerLong==1){
                timerLong=0;
                Toast.makeText(getApplicationContext(), "定时结束\n关闭应用", Toast.LENGTH_SHORT).show();
//                mp.stop();
                mp.pause();
                Intent intent = new Intent(CLOSE_SONG_MAIN);
                sendBroadcast(intent);
//                onDestroy();
//                stopSelf();

            }

            if (!ifSeek) {
                musicTimeNow = mpControl.songGetTimeCur();
                showTimeNow = musicTimeNow / 1000 / 60 + ":" + musicTimeNow / 1000 % 60;
//                songTimeNow.setText(showTimeNow);
//                Log.e("myHandler", "showTimeNow="+showTimeNow);
//                Log.e("myHandler", "showTimeAll="+showTimeAll);

                if (Objects.equals(showTimeNow, showTimeOver)) {
                    if (!ifCycle) {
                        Toast.makeText(getApplicationContext(), "下一曲", Toast.LENGTH_SHORT).show();
//                        Log.e("myHandler", "NextSong");
                        mpControl.NextSong();
                    } else {
                        Toast.makeText(getApplicationContext(), "单曲循环", Toast.LENGTH_SHORT).show();
//                        mpControl.songPlay(mpControl.dBGetPath(positionSongNow));
                        mpControl.songCycle();

                    }
                }
            }
            handlerBar.sendEmptyMessageDelayed(TIMER_MSG, 1000);


        }
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
        notificationLayout.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.remoteview);

        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);


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
//        notificationLayout.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.remoteview);
//        notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, nSongName);


        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPre, prePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnNext, nextPendingIntent);
//        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NBtnPlayPause, playPausePendingIntent);
        notificationLayoutExpanded.setOnClickPendingIntent(R.id.NImage, playPausePendingIntent);

            notificationLayout.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "播放："+nSongName);
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

//        if (mp.isPlaying()) {
            notificationLayout.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
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

    public void initReceiver() {

        myReceiverIn= new MyReceiverIn();
//        IntentFilter filter=new IntentFilter("ysPlay");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_PAUSE_SONG);
        intentFilter.addAction(PRE_SONG);
        intentFilter.addAction(NEXT_SONG);
        registerReceiver(myReceiverIn,intentFilter);
    }

    public class MyReceiverIn extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case PLAY_PAUSE_SONG:
                    Log.e("ysPlay",PLAY_PAUSE_SONG);
                    mpControl.PlayOrPause();

//                    playOrPause();
                    break;
                case PRE_SONG:
                    Log.e("ysPlay",PRE_SONG);
                    mpControl.PreSong();

                    break;
                case NEXT_SONG:
                    Log.e("ysPlay",NEXT_SONG);
                    mpControl.NextSong();

                    break;
            }

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiverIn);
    }


}
