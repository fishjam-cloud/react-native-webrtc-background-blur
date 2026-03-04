package com.fishjam.blur;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.oney.WebRTCModule.videoEffects.ProcessorProvider;

public class BlurRegistrar extends ContentProvider {
    @Override
    public boolean onCreate() {
        ProcessorProvider.addProcessor("backgroundBlur", new BackgroundBlurProcessorFactory());
        return false;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
