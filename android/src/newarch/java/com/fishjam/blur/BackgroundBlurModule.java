package com.fishjam.blur;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.fishjam.blur.NativeBackgroundBlurSpec;
import com.oney.WebRTCModule.videoEffects.ProcessorProvider;

public class BackgroundBlurModule extends NativeBackgroundBlurSpec {

    public static final String NAME = "BackgroundBlur";
    private static boolean isInitialized = false;

    public BackgroundBlurModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize() {
        if (isInitialized) {
            return;
        }

        ProcessorProvider.addProcessor("backgroundBlur", new BackgroundBlurProcessorFactory());
        isInitialized = true;
    }

    @Override
    public void deinitialize() {
        if (!isInitialized) {
            return;
        }

        ProcessorProvider.removeProcessor("backgroundBlur");
        isInitialized = false;
    }

    @Override
    public void setBlurRadius(double radius) {
        BackgroundBlurProcessor.setBlurRadius((float) radius);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
