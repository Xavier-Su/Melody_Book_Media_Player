package com.example.musicplayer.receiver;


import static com.example.musicplayer.MainActivity.NEXT_SONG;
import static com.example.musicplayer.MainActivity.PLAY_PAUSE_SONG;
import static com.example.musicplayer.MainActivity.PRE_SONG;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case PLAY_PAUSE_SONG:
                Log.e("ysPlay123",PLAY_PAUSE_SONG);

                break;
            case PRE_SONG:
                Log.e("ysPlay123",PRE_SONG);
                break;
            case NEXT_SONG:
                Log.e("ysPlay123",NEXT_SONG);
                break;
        }

    }
}
