package com.stick.gsliu.mediacodecevo.activity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.stick.gsliu.mediacodecevo.R;
import com.stick.gsliu.mediacodecevo.utils.PermissionUtils;
import com.stick.gsliu.mediacodecevo.yh.MediaCodecUtil;
import com.stick.gsliu.mediacodecevo.yh.OnVideoDecodeListener;
import com.stick.gsliu.mediacodecevo.yh.OnVideoEncodeListener;
import com.stick.gsliu.mediacodecevo.yh.VideoConfiguration;
import com.stick.gsliu.mediacodecevo.yh.YUVInputEncoder;
import com.stick.gsliu.mediacodecevo.yh.YUVInputVideoController;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.media.MediaFormat.KEY_HEIGHT;
import static android.media.MediaFormat.KEY_MIME;
import static android.media.MediaFormat.KEY_WIDTH;

public class Demo2Activity extends AppCompatActivity {

    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String TEST_INPUT_FILE_PATH = SDCARD_PATH + "/1/aa/d11.mp4";
    private static final String TEST_OUTPUT_FILE_PATH = SDCARD_PATH + "/1/aa/d3.mp4";

    private TextView mLogView;//触摸开始走流程
    private static final String VIDEO_MEDIA_TYPE = "video/avc";//视频的轨道类型
    private static int BYTE_BUFFER_LENGTH = 1024 * 1024 * 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo2);

        PermissionUtils.performCodeWithStoragePermission(this, new PermissionUtils
                .PermissionCallback() {
            @Override
            public void hasPermission() {

            }
        });
        mLogView = (TextView) findViewById(R.id.LogView);
    }

    public void startWorking(View view) {
        //开始走流程
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isFileExists(TEST_INPUT_FILE_PATH)) {
                    try {
                        //exportFileCopy(TEST_INPUT_FILE_PATH, TEST_OUTPUT_FILE_PATH);
                        exportFileCopyWith();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showState("IOException e : " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    public void deleteCopyFile(View view) {
        //删除生成的文件
        File file = new File(TEST_OUTPUT_FILE_PATH);
        if (file.exists()) {
            boolean delete = file.delete();
            Toast.makeText(this, "删除生成的文件 ？ " + delete, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "文件不存在。。。。", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isFileExists(String filePath) {
        //文件是否存在
        boolean exists = new File(filePath).exists();
        if (!exists) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Demo2Activity.this, "文件不存在。。。。", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return exists;
    }

    public void showState(final String content) {
        //打印working信息
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("MediaDemo", content);
                mLogView.setText(mLogView.getText() + "\n" + content);
            }
        });
    }

    //获取指定类型媒体文件所在轨道
    private int getMediaTrackIndex(MediaExtractor videoExtractor, String mediaType) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            //获取视频所在轨道
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(KEY_MIME);
            if (mime.startsWith(mediaType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    /**
     * 将缓冲区传给解码器
     *
     * @param extractor
     * @param decoder
     * @param inputBuffers
     * @return
     */
    private boolean putBufferToCoder(MediaExtractor extractor, MediaCodec decoder, ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = decoder.dequeueInputBuffer(10000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
                Log.v("xxx", "media eos");
            } else {
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return isMediaEOS;
    }


    /**
     * //暂时下载一个方法里面 流程如下：
     * 旧的mp4视频文件 经过插接器  ==》  合成器 生成新的mp4视频文件
     *
     * @param oldFilePath 旧文件
     * @param newFilePath 新文件
     */
    private void exportFileCopy(@NonNull String oldFilePath, @NonNull String newFilePath) throws IOException {
        if (!isFileExists(oldFilePath)) {
            return;
        }
        showState("getAacFile ready");

        MediaMuxer mediaMuxer = null;

        MediaExtractor extractor = new MediaExtractor();
        showState("extractor Constructor");
        extractor.setDataSource(oldFilePath);
        showState("extractor setDataSource");

        //获取视频轨道
        int mVideoTrackIndex = -1;
        mVideoTrackIndex = getMediaTrackIndex(extractor, VIDEO_MEDIA_TYPE);
        if (mVideoTrackIndex >= 0) {
            showState("getMediaTrackIndex success videoIndex : " + mVideoTrackIndex);
            //获取MediaFormat
            MediaFormat format = extractor.getTrackFormat(mVideoTrackIndex);
            showState("getTrackFormat success");
            extractor.selectTrack(mVideoTrackIndex);
            showState("Extractor selectTrack index : " + mVideoTrackIndex);
            showState("Extractor is ready 。。。。。。。。。");

            //初始化 MediaMuxer
            mediaMuxer = new MediaMuxer(newFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            showState("mediaMuxer Constructor");
            mediaMuxer.addTrack(format);
            showState("mediaMuxer addTrack");
            showState("mediaMuxer is ready 。。。。。。。。。");
            mediaMuxer.start();
            showState("mediaMuxer getAacFile working");
        }

        //MediaMuxer开始工作
        if (mediaMuxer == null) {
            showState("mediaMuxer == null ，no working");
            return;
        }

        //用Mediacodec.BufferInfo 处理数据存储
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        showState("new MediaCodec.BufferInfo()  info");
        info.presentationTimeUs = 0;
        //TODO
        ByteBuffer buffer = ByteBuffer.allocate(BYTE_BUFFER_LENGTH);
        showState("new MediaCodec.BufferInfo()  info  is ready。。。。。。。。");

        while (true) {
            int sampleSize = extractor.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                showState("read sample data failed, break !");
                break;
            }
            info.offset = 0;
            info.size = sampleSize;
            info.flags = extractor.getSampleFlags();
            info.presentationTimeUs = extractor.getSampleTime();
            boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
            showState("write sample " + keyframe + " , " + sampleSize + " , " + info.presentationTimeUs);
            mediaMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
            extractor.advance();
        }

        extractor.release();
        mediaMuxer.stop();
        mediaMuxer.release();
    }

    private void exportFileCopyWith() throws IOException {


        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(TEST_INPUT_FILE_PATH);

        //获取视频轨道
        final int mVideoTrackIndex = getMediaTrackIndex(extractor, VIDEO_MEDIA_TYPE);
        if (mVideoTrackIndex >= 0) {
            int width = 0;
            int height = 0;
            //获取MediaFormat
            MediaFormat format = extractor.getTrackFormat(mVideoTrackIndex);
            extractor.selectTrack(mVideoTrackIndex);
            width = format.getInteger(KEY_WIDTH);
            height = format.getInteger(KEY_HEIGHT);

            MediaCodecUtil mediaCodecUtil = new MediaCodecUtil(null, width, height);
            mediaCodecUtil.isShowSurface = false;
            mediaCodecUtil.MIME_TYPE = format.getString(KEY_MIME);
            mediaCodecUtil.mMediaFormat = format;
            mediaCodecUtil.init2();
            //初始化 MediaMuxer
            final MediaMuxer mediaMuxer = new MediaMuxer(TEST_OUTPUT_FILE_PATH, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaMuxer.addTrack(format);
            mediaMuxer.start();

            final YUVInputVideoController yuvInputEncoder = new YUVInputVideoController(format);
            VideoConfiguration configuration = new VideoConfiguration.Builder().setSize(width, height).build();
            yuvInputEncoder.setVideoConfiguration(configuration);
            yuvInputEncoder.resume();

            //用Mediacodec.BufferInfo 处理数据存储
            final int finalWidth = width;
            final int finalHeight = height;

            final int[] decodeCount = {0};
            mediaCodecUtil.setVideoListener(new OnVideoDecodeListener() {
                @Override
                public void onVideoDecode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
                    bb.position(bi.offset);
                    bb.limit(bi.offset + bi.size);
                    decodeCount[0]++;
                    Log.i(TAG, "decode : decodeCount[0] " + decodeCount[0]+"   bi.presentationTimeUs: "+bi.presentationTimeUs);
                    yuvInputEncoder.queueBufferInfo(bb, finalWidth, finalHeight,bi);
                }
            });


            final int[] encodeCount = {0};

            final int finalMVideoTrackIndex = mVideoTrackIndex;
            yuvInputEncoder.setVideoEncoderListener(new OnVideoEncodeListener() {
                @Override
                public void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
                    encodeCount[0]++;
                    Log.i(TAG, "encode : encodeCount[0] " + encodeCount[0]+"   bi.presentationTimeUs: "+bi.presentationTimeUs);
                    mediaMuxer.writeSampleData(finalMVideoTrackIndex, bb, bi);
                }
            });
            yuvInputEncoder.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteBuffer buffer = ByteBuffer.allocate(BYTE_BUFFER_LENGTH);
            int heCount = 0;
            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize <= 0) {
                    showState("sample size < 0, break ! ");
                    showState("extractor is finished ! ");
                    break;
                }
                info.offset = 0;
                info.size = sampleSize;
                info.flags = extractor.getSampleFlags();
                info.presentationTimeUs = extractor.getSampleTime();
                boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
                boolean ret = mediaCodecUtil.onFrame(buffer.array(), 0, sampleSize, info);
                if (ret) {
                    extractor.advance();
                    heCount++;
                    Log.i(TAG, "extractorCode : heCount " + heCount+"  info.presentationTimeUs:"+info.presentationTimeUs);
                }
                mediaCodecUtil.onOut();
            }
            int waitTime = 5000;
            long l = SystemClock.currentThreadTimeMillis();

            while (true) {
                mediaCodecUtil.onOut();
                //超过10秒
                long c = SystemClock.currentThreadTimeMillis();
                long t = c - l;
                if (t > waitTime) {
                    break;
                }
            }

            yuvInputEncoder.pause();
            yuvInputEncoder.stop();
            extractor.release();
            mediaMuxer.stop();
            mediaMuxer.release();
        }
    }

    private void exportFileCopyWith1() throws IOException {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaMuxer videoMuxer = null;
        //解码器
        MediaCodec decodeCodec = null;
        //编码器
        MediaCodec encodeCodec = null;
        int videoTrackIndex = -1;//视频轨道
        MediaFormat mediaFormat = null;
        int width = 0;
        int height = 0;
        String mime = null;

        videoExtractor.setDataSource(TEST_INPUT_FILE_PATH);
        videoTrackIndex = getMediaTrackIndex(videoExtractor, VIDEO_MEDIA_TYPE);
        if (videoTrackIndex >= 0) {
            mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            videoExtractor.selectTrack(videoTrackIndex);

            //初始化解码器
            decodeCodec = MediaCodec.createDecoderByType(mime);
            decodeCodec.configure(mediaFormat, null, null, 0);
            //编码器
            encodeCodec = MediaCodec.createDecoderByType(mime);
            encodeCodec.configure(mediaFormat, null, null, 0);
        }

        if (decodeCodec == null) {
            showState("MediaCodec  decodeCodec  null");
            return;
        }
        //解码器开始工作
        decodeCodec.start();

        //初始化解码器的 缓存类
        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = decodeCodec.dequeueOutputBuffer(videoBufferInfo, 0);
        if (outputBufferIndex < 0) {
            Log.e(TAG, " outputBufferIndex error : " + outputBufferIndex);
        }
        ByteBuffer[] outBuffers = decodeCodec.getOutputBuffers();
        //循环解码，直到数据全部解码完成
        while (outputBufferIndex >= 0) {
            if (outputBufferIndex >= 0) {
                Log.i(TAG, "get one frame data.");
                ByteBuffer bb = outBuffers[outputBufferIndex];
                //TODO 回调每一帧
                //mListener.onVideoDecode(bb, bufferInfo);
            }
            decodeCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = decodeCodec.dequeueOutputBuffer(videoBufferInfo, 0);
            Log.i("gsliu","");
        }

    }

    private static final String TAG = "gsliu";

    //public native ByteBuffer getIMUData(ByteBuffer packet);

}
