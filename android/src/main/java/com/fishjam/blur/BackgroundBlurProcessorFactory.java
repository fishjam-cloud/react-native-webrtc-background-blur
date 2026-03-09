package com.fishjam.blur;

import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;
import com.oney.WebRTCModule.videoEffects.VideoFrameProcessorFactoryInterface;

public class BackgroundBlurProcessorFactory implements VideoFrameProcessorFactoryInterface {
    @Override
    public VideoFrameProcessor build() {
        return new BackgroundBlurProcessor();
    }
}
