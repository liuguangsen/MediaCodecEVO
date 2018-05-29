package com.stick.gsliu.mediacodecevo.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;

/**
 * Created by 1505019 on 2018/5/25.
 */

public class VideoMediaExtractorEditor {

    private static final String AUDIO_MEDIA_TYPE = "audio/";
    private static final int BYTE_BUFFER_LENGTH = 2 * 1024 * 1024;

    private String videoPath;
    private String outputAacPath;
    private boolean isCanceled;
    private boolean isFinished;
    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;

    public VideoMediaExtractorEditor() {
    }

    /**
     * 从视频文件中取出音频文件
     *
     * @param videoPath     输入视频路径
     * @param outputAacPath 输出音频路径
     * @return
     */
    public boolean getAacFile(String videoPath, String outputAacPath) {
        this.videoPath = videoPath;
        this.outputAacPath = outputAacPath;
        try {
            run();
        } catch (IOException e) {
            e.printStackTrace();
            return isFinished;
        }
        return isFinished;
    }

    /**
     * 取消任务 并销毁所有资源
     *
     * @param canceled 控制参数 true 停止当前的任务，false 不停止当前的任务
     */
    public void cancel(boolean canceled) {
        isCanceled = canceled;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isFinished() {
        return isFinished;
    }

    private boolean run() throws IOException {

        boolean fileExit = checkFileExit(videoPath);
        if (!fileExit) {
            notifyError("File not exist ！");
            return false;
        }
        boolean isHaveAudio;
        mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoPath);
        if (mediaExtractor == null) {
            notifyError("mediaExtractor is null ！");
            return false;
        }
        int audioTrackIndex = getMediaTrackIndex(mediaExtractor, AUDIO_MEDIA_TYPE);
        isHaveAudio = audioTrackIndex < 0;
        if (isHaveAudio) {
            notifyError("not find audio form video ！");
            return false;
        }
        mediaExtractor.selectTrack(audioTrackIndex);
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioTrackIndex);
        mediaMuxer = new MediaMuxer(outputAacPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int audioIndex = mediaMuxer.addTrack(audioFormat);
        mediaMuxer.start();
        ByteBuffer audioBuffer = ByteBuffer.allocate(BYTE_BUFFER_LENGTH);
        boolean isAudioEnd = false;
        notifyPrepared();
        //准备工具中
        if (checkIsCanceled()) return false;
        while (true) {
            //工作中
            if (checkIsCanceled()) return false;
            int sampleSize = mediaExtractor.readSampleData(audioBuffer, 0);
            if (sampleSize <= 0) {
                isAudioEnd = true;
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.offset = 0;
                info.size = 0;
                info.flags = BUFFER_FLAG_END_OF_STREAM;
                info.presentationTimeUs = 0;
                notifyWorking(info);
                mediaMuxer.writeSampleData(audioIndex, audioBuffer, info);
            } else {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.offset = 0;
                info.size = sampleSize;
                info.flags = mediaExtractor.getSampleFlags();
                info.presentationTimeUs = mediaExtractor.getSampleTime();
                notifyWorking(info);
                mediaMuxer.writeSampleData(audioIndex, audioBuffer, info);
            }
            if (isAudioEnd) {
                break;
            }
            mediaExtractor.advance();
        }
        release();
        isFinished = true;
        notifyCompletion();
        //工作结束
        return true;
    }

    private boolean checkIsCanceled() {
        if (isCanceled) {
            cancel();
            notifyError("the work is canceled ！");
            return true;
        }
        return false;
    }

    private void cancel() {
        isCanceled = false;
        if (!isFinished) {
            release();
        }
        clearFile();
    }

    private void release() {
        try {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
                mediaMuxer = null;
            }
            if (mediaExtractor != null) {
                mediaExtractor.release();
                mediaExtractor.release();
                mediaExtractor = null;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void clearFile() {
        File file = new File(outputAacPath);
        if (file.exists()) {
            file.deleteOnExit();
        }
    }

    private boolean checkFileExit(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private int getMediaTrackIndex(MediaExtractor videoExtractor, String mediaType) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mediaType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    private OnWorkListener listener;

    public void setListener(OnWorkListener listener) {
        this.listener = listener;
    }

    private void notifyPrepared() {
        if (listener != null) {
            listener.onPrepared();
        }
    }

    private void notifyWorking(MediaCodec.BufferInfo info) {
        if (listener != null) {
            listener.onWorking(info.presentationTimeUs);
        }
    }

    private void notifyCompletion() {
        if (listener != null) {
            listener.onCompletion(videoPath, outputAacPath);
        }
    }

    private void notifyError(String errorStr) {
        if (listener != null) {
            listener.onError(errorStr);
        }
    }

    public interface OnWorkListener {
        void onPrepared();

        void onWorking(long presentationTimeUs);

        void onCompletion(String videoPath, String outPutAudioPath);

        void onError(String errorStr);
    }
}
