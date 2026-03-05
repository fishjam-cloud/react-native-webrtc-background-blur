package com.fishjam.blur;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.oney.WebRTCModule.videoEffects.ProcessorProvider;

@ReactModule(name = BackgroundBlurModule.NAME)
public class BackgroundBlurModule extends ReactContextBaseJavaModule {

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

    @ReactMethod
    public void initialize() {
        if (isInitialized) {
            return;
        }

        ProcessorProvider.addProcessor("backgroundBlur", new BackgroundBlurProcessorFactory());
        isInitialized = true;
    }

    @ReactMethod
    public void deinitialize() {
        if (!isInitialized) {
            return;
        }

        ProcessorProvider.removeProcessor("backgroundBlur");
        isInitialized = false;
    }

    @ReactMethod
    public void setBlurRadius(double radius) {
        BackgroundBlurProcessor.setBlurRadius((float) radius);
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isAvailable() {
        return true;
    }
}
