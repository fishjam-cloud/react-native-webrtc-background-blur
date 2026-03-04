package com.fishjam.blur;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import com.oney.WebRTCModule.videoEffects.VideoFrameProcessor;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.YuvConverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BackgroundBlurProcessor implements VideoFrameProcessor {

    private static final String TAG = "BackgroundBlurProcessor";

    private static volatile float pendingBlurRadius = -1f;

    private final Segmenter segmenter;
    private final GlBlurRenderer renderer;
    private final MaskPostProcessor maskPostProcessor;

    private volatile boolean isProcessing;
    private VideoFrame lastProcessedFrame;
    private YuvConverter yuvConverter;

    private ByteBuffer maskByteBuffer;
    private int maskByteBufferCapacity;
    private float[] rawFloatMask;
    private Bitmap segmentationBitmap;

    public static void setBlurRadius(float radius) {
        pendingBlurRadius = radius;
    }

    public BackgroundBlurProcessor() {
        SelfieSegmenterOptions options = new SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .enableRawSizeMask()
                .build();
        segmenter = Segmentation.getClient(options);
        renderer = new GlBlurRenderer();
        maskPostProcessor = new MaskPostProcessor();
    }

    @Override
    public VideoFrame process(VideoFrame frame, SurfaceTextureHelper textureHelper) {
        if (isProcessing) {
            if (lastProcessedFrame != null) {
                lastProcessedFrame.retain();
                return lastProcessedFrame;
            }
            frame.retain();
            return frame;
        }

        VideoFrame.Buffer buffer = frame.getBuffer();
        if (!(buffer instanceof VideoFrame.TextureBuffer)) {
            frame.retain();
            return frame;
        }

        VideoFrame.TextureBuffer textureBuffer = (VideoFrame.TextureBuffer) buffer;

        isProcessing = true;
        try {
            VideoFrame result = processFrame(frame, textureBuffer, textureHelper);
            if (lastProcessedFrame != null) {
                lastProcessedFrame.release();
            }
            result.retain();
            lastProcessedFrame = result;
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
            frame.retain();
            return frame;
        } finally {
            isProcessing = false;
        }
    }

    private VideoFrame processFrame(VideoFrame frame, VideoFrame.TextureBuffer textureBuffer,
                                    SurfaceTextureHelper textureHelper) throws Exception {
        float radius = pendingBlurRadius;
        if (radius >= 0f) {
            pendingBlurRadius = -1f;
            renderer.setBlurRadius(radius);
        }

        int width = textureBuffer.getWidth();
        int height = textureBuffer.getHeight();

        renderer.ensureSetup(width, height);
        if (yuvConverter == null) {
            yuvConverter = new YuvConverter();
        }

        float[] transformMatrix = convertToGlMatrix(textureBuffer.getTransformMatrix());
        boolean isOes = textureBuffer.getType() == VideoFrame.TextureBuffer.Type.OES;
        renderer.renderToRgbaFbo(textureBuffer.getTextureId(), transformMatrix, isOes);

        renderer.renderDownscaled();
        ByteBuffer rgbaPixels = renderer.readSegmentationPixels();

        int segW = renderer.getSegmentationWidth();
        int segH = renderer.getSegmentationHeight();

        if (segmentationBitmap == null
                || segmentationBitmap.getWidth() != segW
                || segmentationBitmap.getHeight() != segH) {
            segmentationBitmap = Bitmap.createBitmap(segW, segH, Bitmap.Config.ARGB_8888);
        }
        segmentationBitmap.copyPixelsFromBuffer(rgbaPixels);

        InputImage inputImage = InputImage.fromBitmap(segmentationBitmap, 0);
        SegmentationMask mask = Tasks.await(segmenter.process(inputImage));

        int maskWidth = mask.getWidth();
        int maskHeight = mask.getHeight();
        ByteBuffer floatMask = mask.getBuffer();
        floatMask.rewind();

        int requiredCapacity = maskWidth * maskHeight;
        if (maskByteBuffer == null || maskByteBufferCapacity < requiredCapacity) {
            maskByteBuffer = ByteBuffer.allocateDirect(requiredCapacity);
            maskByteBuffer.order(ByteOrder.nativeOrder());
            maskByteBufferCapacity = requiredCapacity;
        }
        if (rawFloatMask == null || rawFloatMask.length < requiredCapacity) {
            rawFloatMask = new float[requiredCapacity];
        }

        for (int i = 0; i < requiredCapacity; i++) {
            rawFloatMask[i] = Math.min(Math.max(floatMask.getFloat(), 0f), 1f);
        }

        maskPostProcessor.process(rawFloatMask, maskWidth, maskHeight);

        maskByteBuffer.clear();
        for (int i = 0; i < requiredCapacity; i++) {
            maskByteBuffer.put((byte) (rawFloatMask[i] * 255));
        }
        maskByteBuffer.rewind();

        renderer.uploadMask(maskByteBuffer, maskWidth, maskHeight);

        renderer.renderBlur();

        renderer.renderComposite();

        TextureBufferImpl outputBuffer = new TextureBufferImpl(
                width, height,
                VideoFrame.TextureBuffer.Type.RGB,
                renderer.getOutputTextureId(),
                new Matrix(),
                textureHelper.getHandler(),
                yuvConverter,
                null
        );

        return new VideoFrame(outputBuffer, frame.getRotation(), frame.getTimestampNs());
    }

    public void release() {
        segmenter.close();
        renderer.release();
        if (yuvConverter != null) {
            yuvConverter.release();
            yuvConverter = null;
        }
        if (lastProcessedFrame != null) {
            lastProcessedFrame.release();
            lastProcessedFrame = null;
        }
        if (segmentationBitmap != null) {
            segmentationBitmap.recycle();
            segmentationBitmap = null;
        }
    }

    private static float[] convertToGlMatrix(Matrix androidMatrix) {
        float[] values = new float[9];
        androidMatrix.getValues(values);
        return new float[]{
                values[0], values[3], 0, values[6],
                values[1], values[4], 0, values[7],
                0, 0, 1, 0,
                values[2], values[5], 0, values[8]
        };
    }
}
