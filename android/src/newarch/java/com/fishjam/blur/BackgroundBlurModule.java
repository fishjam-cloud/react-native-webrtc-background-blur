package com.fishjam.blur;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.fishjam.blur.NativeBackgroundBlurSpec;

public class BackgroundBlurModule extends NativeBackgroundBlurSpec {

    public static final String NAME = "BackgroundBlur";

    public BackgroundBlurModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
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
