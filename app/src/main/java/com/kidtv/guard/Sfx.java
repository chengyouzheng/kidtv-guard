package com.kidtv.guard;

import android.content.Context;
import android.media.MediaPlayer;

/** 语音提示播放器：语音素材打包在 res/raw，不依赖盒子自带的 TTS 引擎 */
public class Sfx {

    private static MediaPlayer mp;

    public static void play(Context c, int resId) {
        try {
            stop();
            MediaPlayer player = MediaPlayer.create(c, resId);
            if (player == null) {
                return;
            }
            mp = player;
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer m) {
                    try {
                        m.release();
                    } catch (Exception ignored) {
                    }
                    if (mp == m) {
                        mp = null;
                    }
                }
            });
            player.start();
        } catch (Exception e) {
            stop();
        }
    }

    public static void stop() {
        MediaPlayer player = mp;
        mp = null;
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
        }
    }
}
