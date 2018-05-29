package com.stick.gsliu.mediacodecevo.activity;

import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.stick.gsliu.mediacodecevo.R;
import com.stick.gsliu.mediacodecevo.utils.PermissionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Demo1Activity extends AppCompatActivity {

    public static final String TAG = "gsliu";
    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String INPUT_FILEPATH = SDCARD_PATH + "/1/aa/d1.mp4";
    private static final String OUTPUT_FILEPATH = SDCARD_PATH + "/1/aa/d2.mp4";
    private TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo1);

        PermissionUtils.performCodeWithStoragePermission(Demo1Activity.this, new PermissionUtils
                .PermissionCallback() {
            @Override
            public void hasPermission() {

            }
        });

        mLogView = (TextView) findViewById(R.id.LogView);
        mLogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            File temp = new File(INPUT_FILEPATH);
                            if (temp.exists()) {
                                transcode(INPUT_FILEPATH, OUTPUT_FILEPATH);
                            } else {
                                Log.e(TAG, "file not exists");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            logout(e.getMessage());
                        }
                    }
                }).start();
            }
        });

    }

    protected boolean transcode(String input, String output) throws IOException {

        logout("getAacFile processing...");

        MediaMuxer muxer = null;

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(input);

        logout("getAacFile demuxer: " + input);

        int mVideoTrackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!mime.startsWith("video")) {
                logout("mime not video, continue search");
                continue;
            }
            extractor.selectTrack(i);
            muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoTrackIndex = muxer.addTrack(format);
            muxer.start();
            logout("getAacFile muxer: " + output);
        }

        if (muxer == null) {
            logout("no video found !");
            return false;
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 2);
        while (true) {
            int sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                logout("read sample data failed , break !");
                break;
            }
            info.offset = 0;
            info.size = sampleSize;
            info.flags = extractor.getSampleFlags();
            info.presentationTimeUs = extractor.getSampleTime();
            boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
            logout("write sample " + keyframe + ", " + sampleSize + ", " + info.presentationTimeUs);
            muxer.writeSampleData(mVideoTrackIndex, buffer, info);
            extractor.advance();
        }

        extractor.release();

        muxer.stop();
        muxer.release();

        logout("process success !");

        return true;
    }

    private void logout(final String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("MediaDemo", content);
                mLogView.setText(mLogView.getText() + "\n" + content);
            }
        });
    }
}
