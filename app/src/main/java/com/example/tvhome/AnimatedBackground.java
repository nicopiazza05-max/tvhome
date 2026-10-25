package com.example.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

/**
 * Fondo animado azul estilo tvOS.
 * - Android 13+: shader de ondas fluidas (ShaderPainter).
 * - Si falla o el sistema es mas viejo: 3 manchas de luz que se mueven lento.
 */
public class AnimatedBackground extends View {

    // Poner en false para un fondo de manchas mas liviano.
    private static final boolean USE_SHADER = true;
    // Poner en false para un fondo estatico (consumo minimo).
    private static final boolean ANIMATE = true;
    // 33 ms = 30 cuadros por segundo. Subir a 50 si la TV se siente lenta.
    private static final int FRAME_MS = 33;

    private static final int BLOBS = 3;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RadialGradient[] shaders = new RadialGradient[BLOBS];
    private final int[] colors = {0xB000C8FF, 0xB02563FF, 0xB07A3CFF};
    private final float[] speedX = {0.46f, 0.34f, 0.26f};
    private final float[] speedY = {0.38f, 0.54f, 0.42f};
    private final float[] phaseX = {0f, 2f, 4f};
    private final float[] phaseY = {1f, 3f, 5f};
    private final long startTime = SystemClock.uptimeMillis();
    private ShaderPainter shaderPainter;
    private float radius;

    public AnimatedBackground(Context context) {
        super(context);
        if (USE_SHADER && Build.VERSION.SDK_INT >= 33) {
            try {
                shaderPainter = new ShaderPainter();
            } catch (Throwable t) {
                shaderPainter = null;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        radius = Math.max(w, h) * 0.5f;
        for (int i = 0; i < BLOBS; i++) {
            shaders[i] = new RadialGradient(0, 0, radius, colors[i],
                    colors[i] & 0x00FFFFFF, Shader.TileMode.CLAMP);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        long elapsed = (SystemClock.uptimeMillis() - startTime) % 36000000L;
        float t = ANIMATE ? elapsed / 1000f : 0f;

        if (shaderPainter != null) {
            try {
                shaderPainter.draw(canvas, w, h, t);
                scheduleNext();
                return;
            } catch (Throwable e) {
                shaderPainter = null; // seguir con el fondo de manchas
            }
        }

        canvas.drawColor(0xFF020B26);
        if (shaders[0] == null) {
            return;
        }
        for (int i = 0; i < BLOBS; i++) {
            float x = w * (0.5f + 0.38f * (float) Math.sin(t * speedX[i] + phaseX[i]));
            float y = h * (0.5f + 0.38f * (float) Math.cos(t * speedY[i] + phaseY[i]));
            canvas.save();
            canvas.translate(x, y);
            paint.setShader(shaders[i]);
            canvas.drawCircle(0, 0, radius, paint);
            canvas.restore();
        }
        scheduleNext();
    }

    private void scheduleNext() {
        if (ANIMATE) {
            postInvalidateDelayed(FRAME_MS);
        }
    }
}
