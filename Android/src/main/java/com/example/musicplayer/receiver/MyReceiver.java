package com.example.musicplayer.receiver;


import static com.example.musicplayer.MainActivity.NEXT_SONG_MAIN;
import static com.example.musicplayer.MainActivity.PLAY_PAUSE_SONG_MAIN;
import static com.example.musicplayer.MainActivity.PRE_SONG_MAIN;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case PLAY_PAUSE_SONG_MAIN:
                Log.e("ysPlay123",PLAY_PAUSE_SONG_MAIN);
                break;
            case PRE_SONG_MAIN:
                Log.e("ysPlay123",PRE_SONG_MAIN);
                break;
            case NEXT_SONG_MAIN:
                Log.e("ysPlay123",NEXT_SONG_MAIN);
                break;
        }

    }
}
