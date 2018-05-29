package com.stick.gsliu.mediacodecevo;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import com.stick.gsliu.mediacodecevo.player.IPlayerCallBack;
import com.stick.gsliu.mediacodecevo.player.PlayerView;
import com.stick.gsliu.mediacodecevo.player.VideoPlayer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements IPlayerCallBack {

    private PlayerView playerView;
    private VideoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = (PlayerView) findViewById(R.id.surfaceView);
        ToggleButton btnControl = (ToggleButton) findViewById(R.id.btnControl);
        player = new VideoPlayer(playerView.getHolder().getSurface(), initData());
        player.setCallBack(this);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.isPlaying()) {
                    player.stop();
                } else {
                    player.play();
                }
            }
        });
    }

    //将raw里video拷贝到文件
    private String initData() {
        File dir = getFilesDir();
        File path = new File(dir, "shape.mp4");
        final BufferedInputStream in = new BufferedInputStream(getResources().openRawResource(R.raw.demo));
        final BufferedOutputStream out;
        try {
            out = new BufferedOutputStream(openFileOutput(path.getName(), Context.MODE_PRIVATE));
            byte[] buf = new byte[1024];
            int size = in.read(buf);
            while (size > 0) {
                out.write(buf, 0, size);
                size = in.read(buf);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path.toString();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void videoAspect(final int width, final int height, final float time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("gsliu", "time: "+time);
                playerView.setAspect((float) width / height);
            }
        });
    }
}
