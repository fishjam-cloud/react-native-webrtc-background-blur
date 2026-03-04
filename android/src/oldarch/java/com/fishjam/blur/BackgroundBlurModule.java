package com.fishjam.blur;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = BackgroundBlurModule.NAME)
public class BackgroundBlurModule extends ReactContextBaseJavaModule {

    public static final String NAME = "BackgroundBlur";

    public BackgroundBlurModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
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
