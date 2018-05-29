package com.stick.gsliu.mediacodecevo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.stick.gsliu.mediacodecevo.R;
import com.stick.gsliu.mediacodecevo.audio.VideoMediaExtractorEditor;
import com.stick.gsliu.mediacodecevo.utils.PermissionUtils;

import java.io.File;

public class Main2Activity extends Activity {

    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String TEST_INPUT_FILE_PATH = SDCARD_PATH + "/1/aa/demo1.mp4";
    private static final String TEST_OUTPUT_FILE_PATH = SDCARD_PATH + "/1/aa/d3.aac";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        PermissionUtils.performCodeWithStoragePermission(this, new PermissionUtils
                .PermissionCallback() {
            @Override
            public void hasPermission() {

            }
        });
    }

    public void test(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                delete();
                VideoMediaExtractorEditor editor = new VideoMediaExtractorEditor();
                boolean success = editor.getAacFile(TEST_INPUT_FILE_PATH,TEST_OUTPUT_FILE_PATH);
                editor.setListener(new VideoMediaExtractorEditor.OnWorkListener() {
                    @Override
                    public void onPrepared() {
                        Log.i("liugstest","onPrepared");
                    }

                    @Override
                    public void onWorking(long presentationTimeUs) {
                        Log.i("liugstest","onWorking ,presentationTimeUs: "+presentationTimeUs);
                    }

                    @Override
                    public void onCompletion(String videoPath,String outPutAudioPath) {
                        Log.i("liugstest","onCompletion  videoPath: "+videoPath+" ,outPutAudioPath: "+outPutAudioPath);
                    }

                    @Override
                    public void onError(String errorStr) {
                        Log.i("liugstest","onError errorStr: "+errorStr);
                    }
                });
                Log.i("liugstest","success");
            }
        }).start();
    }

    public void deleteFile(View view) {
        delete();
    }
    private void delete() {
        File file =new File(TEST_OUTPUT_FILE_PATH);
        if (file.exists()){
            file.deleteOnExit();
        }
    }
}
