package com.fishjam.blur;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GlBlurRenderer {

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;\n" +
            "}\n";

    private static final String VERTEX_SHADER_SIMPLE =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_OES_TO_RGBA =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}\n";

    private static final String FRAGMENT_PASSTHROUGH =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}\n";

    private static final String FRAGMENT_BLUR =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec2 uDirection;\n" +
            "uniform float uWeights[9];\n" +
            "uniform float uOffsets[9];\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(uTexture, vTexCoord) * uWeights[0];\n" +
            "  for (int i = 1; i < 9; i++) {\n" +
            "    vec2 off = uDirection * uOffsets[i];\n" +
            "    color += texture2D(uTexture, vTexCoord + off) * uWeights[i];\n" +
            "    color += texture2D(uTexture, vTexCoord - off) * uWeights[i];\n" +
            "  }\n" +
            "  gl_FragColor = color;\n" +
            "}\n";

    private static final String FRAGMENT_COMPOSITE =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uOriginal;\n" +
            "uniform sampler2D uBlurred;\n" +
            "uniform sampler2D uMask;\n" +
            "void main() {\n" +
            "  vec4 original = texture2D(uOriginal, vTexCoord);\n" +
            "  vec4 blurred = texture2D(uBlurred, vTexCoord);\n" +
            "  float mask = texture2D(uMask, vTexCoord).r;\n" +
            "  gl_FragColor = vec4(mix(blurred.rgb, original.rgb, mask), 1.0);\n" +
            "}\n";

    private static final float[] QUAD_COORDS = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f,
    };

    private static final float[] TEX_COORDS = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
    };

    private static final int SEGMENTATION_WIDTH = 256;
    private static final int SEGMENTATION_HEIGHT = 144;
    private static final int BLUR_DOWNSCALE = 2;

    private final FloatBuffer quadBuffer;
    private final FloatBuffer texBuffer;

    private int oesProgram;
    private int rgbProgram;
    private int passthroughProgram;
    private int blurProgram;
    private int compositeProgram;

    private int rgbaFbo;
    private int rgbaTexture;
    private int segFbo;
    private int segTexture;
    private int blurFboA;
    private int blurTextureA;
    private int blurFboB;
    private int blurTextureB;
    private int outputFbo;
    private int outputTexture;
    private int maskTexture;

    private int currentWidth;
    private int currentHeight;
    private boolean initialized;

    private final float[] blurWeights = new float[9];
    private final float[] blurOffsets = new float[9];

    private ByteBuffer segPixelBuffer;

    public GlBlurRenderer() {
        quadBuffer = createFloatBuffer(QUAD_COORDS);
        texBuffer = createFloatBuffer(TEX_COORDS);
        computeGaussianKernel(12.0f);
    }

    public void ensureSetup(int width, int height) {
        if (initialized && width == currentWidth && height == currentHeight) {
            return;
        }

        if (initialized) {
            releaseGlResources();
        }

        currentWidth = width;
        currentHeight = height;

        oesProgram = createProgram(VERTEX_SHADER, FRAGMENT_OES_TO_RGBA);
        rgbProgram = createProgram(VERTEX_SHADER, FRAGMENT_PASSTHROUGH);
        passthroughProgram = createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_PASSTHROUGH);
        blurProgram = createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_BLUR);
        compositeProgram = createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_COMPOSITE);

        int[] result = createFboAndTexture(width, height);
        rgbaFbo = result[0];
        rgbaTexture = result[1];

        result = createFboAndTexture(SEGMENTATION_WIDTH, SEGMENTATION_HEIGHT);
        segFbo = result[0];
        segTexture = result[1];

        int blurW = width / BLUR_DOWNSCALE;
        int blurH = height / BLUR_DOWNSCALE;
        result = createFboAndTexture(blurW, blurH);
        blurFboA = result[0];
        blurTextureA = result[1];

        result = createFboAndTexture(blurW, blurH);
        blurFboB = result[0];
        blurTextureB = result[1];

        result = createFboAndTexture(width, height);
        outputFbo = result[0];
        outputTexture = result[1];

        maskTexture = createTexture2D();

        initialized = true;
    }

    public void renderToRgbaFbo(int textureId, float[] transformMatrix, boolean isOes) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, rgbaFbo);
        GLES20.glViewport(0, 0, currentWidth, currentHeight);

        int program = isOes ? oesProgram : rgbProgram;
        GLES20.glUseProgram(program);

        int posLoc = GLES20.glGetAttribLocation(program, "aPosition");
        int texLoc = GLES20.glGetAttribLocation(program, "aTexCoord");
        int texMatLoc = GLES20.glGetUniformLocation(program, "uTexMatrix");
        int samplerLoc = GLES20.glGetUniformLocation(program, "uTexture");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer);

        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glUniformMatrix4fv(texMatLoc, 1, false, transformMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int target = isOes ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;
        GLES20.glBindTexture(target, textureId);
        GLES20.glUniform1i(samplerLoc, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void renderDownscaled() {
        drawTextureToFbo(passthroughProgram, rgbaTexture, segFbo, SEGMENTATION_WIDTH, SEGMENTATION_HEIGHT);
    }

    public ByteBuffer readSegmentationPixels() {
        if (segPixelBuffer == null) {
            segPixelBuffer = ByteBuffer.allocateDirect(SEGMENTATION_WIDTH * SEGMENTATION_HEIGHT * 4);
            segPixelBuffer.order(ByteOrder.nativeOrder());
        }
        segPixelBuffer.clear();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, segFbo);
        GLES20.glReadPixels(0, 0, SEGMENTATION_WIDTH, SEGMENTATION_HEIGHT,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, segPixelBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        segPixelBuffer.rewind();
        return segPixelBuffer;
    }

    public void uploadMask(ByteBuffer maskData, int maskWidth, int maskHeight) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                maskWidth, maskHeight, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, maskData);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void renderBlur() {
        int blurW = currentWidth / BLUR_DOWNSCALE;
        int blurH = currentHeight / BLUR_DOWNSCALE;

        drawTextureToFbo(passthroughProgram, rgbaTexture, blurFboA, blurW, blurH);

        renderBlurPass(blurTextureA, blurFboB, blurW, blurH, 1.0f / blurW, 0.0f);
        renderBlurPass(blurTextureB, blurFboA, blurW, blurH, 0.0f, 1.0f / blurH);
    }

    public void renderComposite() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFbo);
        GLES20.glViewport(0, 0, currentWidth, currentHeight);

        GLES20.glUseProgram(compositeProgram);

        int posLoc = GLES20.glGetAttribLocation(compositeProgram, "aPosition");
        int texLoc = GLES20.glGetAttribLocation(compositeProgram, "aTexCoord");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, rgbaTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(compositeProgram, "uOriginal"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurTextureA);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(compositeProgram, "uBlurred"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(compositeProgram, "uMask"), 2);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int getOutputTextureId() {
        return outputTexture;
    }

    public int getSegmentationWidth() {
        return SEGMENTATION_WIDTH;
    }

    public int getSegmentationHeight() {
        return SEGMENTATION_HEIGHT;
    }

    public void release() {
        if (!initialized) return;
        releaseGlResources();
        segPixelBuffer = null;
        initialized = false;
    }

    private void renderBlurPass(int inputTexture, int outputFbo, int width, int height,
                                float dirX, float dirY) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, outputFbo);
        GLES20.glViewport(0, 0, width, height);

        GLES20.glUseProgram(blurProgram);

        int posLoc = GLES20.glGetAttribLocation(blurProgram, "aPosition");
        int texLoc = GLES20.glGetAttribLocation(blurProgram, "aTexCoord");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(blurProgram, "uTexture"), 0);

        GLES20.glUniform2f(GLES20.glGetUniformLocation(blurProgram, "uDirection"), dirX, dirY);
        GLES20.glUniform1fv(GLES20.glGetUniformLocation(blurProgram, "uWeights"), 9, blurWeights, 0);
        GLES20.glUniform1fv(GLES20.glGetUniformLocation(blurProgram, "uOffsets"), 9, blurOffsets, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void drawTextureToFbo(int program, int textureId, int fbo, int width, int height) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, width, height);

        GLES20.glUseProgram(program);

        int posLoc = GLES20.glGetAttribLocation(program, "aPosition");
        int texLoc = GLES20.glGetAttribLocation(program, "aTexCoord");

        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer);
        GLES20.glEnableVertexAttribArray(texLoc);
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void computeGaussianKernel(float sigma) {
        float sum = 0;
        for (int i = 0; i < 9; i++) {
            blurOffsets[i] = (float) i;
            blurWeights[i] = (float) (Math.exp(-(i * i) / (2.0 * sigma * sigma))
                    / (Math.sqrt(2.0 * Math.PI) * sigma));
            sum += (i == 0) ? blurWeights[i] : 2.0f * blurWeights[i];
        }
        for (int i = 0; i < 9; i++) {
            blurWeights[i] /= sum;
        }
    }

    private int[] createFboAndTexture(int width, int height) {
        int texture = createTexture2D();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        int[] fboId = new int[1];
        GLES20.glGenFramebuffers(1, fboId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return new int[]{fboId[0], texture};
    }

    private int createTexture2D() {
        int[] texId = new int[1];
        GLES20.glGenTextures(1, texId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return texId[0];
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private void releaseGlResources() {
        int[] textures = {rgbaTexture, segTexture, blurTextureA, blurTextureB, outputTexture, maskTexture};
        GLES20.glDeleteTextures(textures.length, textures, 0);

        int[] fbos = {rgbaFbo, segFbo, blurFboA, blurFboB, outputFbo};
        GLES20.glDeleteFramebuffers(fbos.length, fbos, 0);

        GLES20.glDeleteProgram(oesProgram);
        GLES20.glDeleteProgram(rgbProgram);
        GLES20.glDeleteProgram(passthroughProgram);
        GLES20.glDeleteProgram(blurProgram);
        GLES20.glDeleteProgram(compositeProgram);
    }

    private static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }
}
