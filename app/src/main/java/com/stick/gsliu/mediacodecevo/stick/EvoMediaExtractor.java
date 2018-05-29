package com.stick.gsliu.mediacodecevo.stick;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/**
 * Created by 1505019 on 2017/12/2.
 * <p>
 * (对MediaExtractor封装)
 * <p>
 * 负责从文件中提取数据
 */

public class EvoMediaExtractor {

    private MediaExtractor mMediaExtractor;

    private String mFilePath;

    public EvoMediaExtractor() {

    }

    public void setDataSource(String filePath) {
        this.mFilePath = filePath;
    }

    /**
     * 获取指定类型媒体文件所在轨道
     *
     * @param videoExtractor MediaExtractor提取器
     * @param mediaType 指定类型
     * @return
     */
    public int getMediaTrackIndex(MediaExtractor videoExtractor, String mediaType) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            //获取视频所在轨道
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mediaType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }
}
