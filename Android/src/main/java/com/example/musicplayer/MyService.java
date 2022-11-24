package com.example.musicplayer;

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
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MyService extends Service {

    @SuppressLint("StaticFieldLeak")
    public ListDatabase myDbList;

    public int positionSongNow=0;
    public int positionSongCount=0;
    public String  positionSongName ="";

    final String CHANNEL_ID="CHANNEL_ID";
    public static final String PLAY_PAUSE_SONG="play_pause_song";
    public static final String NEXT_SONG="next_song";
    public static final String PRE_SONG="pre_song";
    public static final String DB_READ="db_read";


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

    }

//    static public MediaPlayer createMediaPlayer(){
//
//        return mp;
//    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MpControl();
    }
    public MyService(){
//        mp = new MediaPlayer();

    }

     class MpControl extends Binder{


         public void setPositionSongNow(int positionNow) {
             positionSongNow = positionNow;
         }

         public void setPositionSongName(String positionName) {
             positionSongName = positionName;
         }

         public void setPositionSongCount(int positionCount) {
             positionSongCount = positionCount;
         }

         public void songPlay(String filePath){

             try {
                 mp.reset();
                 mp.setDataSource(filePath);
                 mp.prepare();
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
            return mp.getDuration();
        }
        public int songGetTimeCur(){
            return mp.getCurrentPosition();
        }
        public void songPositionJump(int PositionJump){
            mp.seekTo(PositionJump);
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



         }


         public void NextSong() {

             int countSong = positionSongCount;
//        int nowCur = positionCur;
             int nowCur = positionSongNow;
             System.out.println("countSong = " + countSong);
             System.out.println("nowCur = " + nowCur);
             System.out.println("dBGetPath(nowCur) = " + dBGetPath(nowCur));
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
            notificationLayout.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
            notificationLayoutExpanded.setTextViewText(R.id.NTvSongNow, "暂停："+nSongName);
            notificationLayout.setImageViewResource(R.id.NImage, R.drawable.playsong);
            notificationLayoutExpanded.setImageViewResource(R.id.NImage, R.drawable.playsong);

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

    public void initReceiver() {

//        myReceiver=new MyReceiver();
//        myReceiver=new MyReceiverIn();
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
//                    playOrPause();
//                    mp.pause();
                    if (mp.isPlaying()){
                        mp.pause();
                        sendNotificationChangePause(positionSongName);
                    }else {
                        mp.start();
                        sendNotificationChangePlay(positionSongName);
                    }
//                    System.out.println("addawdaw"+mp.isPlaying());
//                    System.out.println("1dwadaw"+mp.getDuration());
//                    System.out.println("addawdaw"+mp.getCurrentPosition());
//                    playOrPause();
                    break;
                case PRE_SONG:
                    Log.e("ysPlay",PRE_SONG);
                    mpControl.PreSong();
                    sendNotificationChangePlay(positionSongName);
                    break;
                case NEXT_SONG:
                    Log.e("ysPlay",NEXT_SONG);
                    mpControl.NextSong();
                    sendNotificationChangePlay(positionSongName);
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
