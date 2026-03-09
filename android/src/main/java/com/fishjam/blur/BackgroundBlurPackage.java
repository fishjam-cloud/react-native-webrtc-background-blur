package com.fishjam.blur;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class BackgroundBlurPackage extends TurboReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(@NonNull String name, @NonNull ReactApplicationContext reactContext) {
        if (name.equals(BackgroundBlurModule.NAME)) {
            return new BackgroundBlurModule(reactContext);
        }
        return null;
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            Map<String, ReactModuleInfo> map = new HashMap<>();
            map.put(
                BackgroundBlurModule.NAME,
                new ReactModuleInfo(
                    BackgroundBlurModule.NAME,
                    BackgroundBlurModule.class.getName(),
                    false,
                    false,
                    false,
                    BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
                )
            );
            return map;
        };
    }
}
