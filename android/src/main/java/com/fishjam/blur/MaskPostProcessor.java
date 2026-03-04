package com.fishjam.blur;

public class MaskPostProcessor {

    private static final float BINARIZE_THRESHOLD = 0.5f;
    private static final float EMA_ALPHA = 0.85f;
    private static final float THRESHOLD = EMA_ALPHA + 0.05f;
    private static final float GAUSSIAN_SIGMA = 2.0f;
    private static final int GAUSSIAN_RADIUS = 3;

    private float[] smoothedMask;
    private float[] tempA;
    private float[] tempB;
    private final float[] gaussianKernel;
    private int maskWidth;
    private int maskHeight;
    private boolean hasHistory;

    public MaskPostProcessor() {
        gaussianKernel = computeGaussianKernel(GAUSSIAN_SIGMA, GAUSSIAN_RADIUS);
    }

    public void process(float[] rawMask, int w, int h) {
        ensureBuffers(w, h);
        int len = w * h;

        binarize(rawMask, tempA, len);
        erode(tempA, tempB, w, h);
        dilate(tempB, tempA, w, h);
        applyEMA(tempA, len);
        threshold(smoothedMask, tempA, len);
        gaussianBlurHorizontal(tempA, tempB, w, h);
        gaussianBlurVertical(tempB, tempA, w, h);

        System.arraycopy(tempA, 0, rawMask, 0, len);
    }

    public void reset() {
        hasHistory = false;
    }

    private void ensureBuffers(int w, int h) {
        if (w != maskWidth || h != maskHeight) {
            int len = w * h;
            smoothedMask = new float[len];
            tempA = new float[len];
            tempB = new float[len];
            maskWidth = w;
            maskHeight = h;
            hasHistory = false;
        }
    }

    private static void binarize(float[] src, float[] dst, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = src[i] > BINARIZE_THRESHOLD ? 1.0f : 0.0f;
        }
    }

    private void erode(float[] src, float[] dst, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float min = 1.0f;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) { min = 0.0f; continue; }
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w) { min = 0.0f; continue; }
                        float v = src[ny * w + nx];
                        if (v < min) min = v;
                    }
                }
                dst[y * w + x] = min;
            }
        }
    }

    private void dilate(float[] src, float[] dst, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float max = 0.0f;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w) continue;
                        float v = src[ny * w + nx];
                        if (v > max) max = v;
                    }
                }
                dst[y * w + x] = max;
            }
        }
    }

    private void applyEMA(float[] current, int len) {
        if (!hasHistory) {
            System.arraycopy(current, 0, smoothedMask, 0, len);
            hasHistory = true;
        } else {
            float oneMinusAlpha = 1.0f - EMA_ALPHA;
            for (int i = 0; i < len; i++) {
                smoothedMask[i] = EMA_ALPHA * smoothedMask[i] + oneMinusAlpha * current[i];
            }
        }
    }

    private static void threshold(float[] src, float[] dst, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = src[i] > THRESHOLD ? 1.0f : 0.0f;
        }
    }

    private void gaussianBlurHorizontal(float[] src, float[] dst, int w, int h) {
        int r = GAUSSIAN_RADIUS;
        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int k = -r; k <= r; k++) {
                    int sx = Math.min(Math.max(x + k, 0), w - 1);
                    sum += src[rowOffset + sx] * gaussianKernel[k + r];
                }
                dst[rowOffset + x] = sum;
            }
        }
    }

    private void gaussianBlurVertical(float[] src, float[] dst, int w, int h) {
        int r = GAUSSIAN_RADIUS;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int k = -r; k <= r; k++) {
                    int sy = Math.min(Math.max(y + k, 0), h - 1);
                    sum += src[sy * w + x] * gaussianKernel[k + r];
                }
                dst[y * w + x] = sum;
            }
        }
    }

    private static float[] computeGaussianKernel(float sigma, int radius) {
        int size = 2 * radius + 1;
        float[] kernel = new float[size];
        float sum = 0;
        for (int i = 0; i < size; i++) {
            int d = i - radius;
            kernel[i] = (float) Math.exp(-(d * d) / (2.0 * sigma * sigma));
            sum += kernel[i];
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
}
