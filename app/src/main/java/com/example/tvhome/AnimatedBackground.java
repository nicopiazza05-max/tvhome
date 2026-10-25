package com.example.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/** Fondo azul con manchas de luz que se mueven lento. Liviano: 3 circulos, ~20 fps. */
public class AnimatedBackground extends View {

    private static final int BLOBS = 3;
    // Para un fondo estatico (menos consumo), poner ANIMATE = false.
    private static final boolean ANIMATE = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RadialGradient[] shaders = new RadialGradient[BLOBS];
    private final int[] colors = {0xB000C8FF, 0xB02563FF, 0xB07A3CFF};
    private final float[] speedX = {0.46f, 0.34f, 0.26f};
    private final float[] speedY = {0.38f, 0.54f, 0.42f};
    private final float[] phaseX = {0f, 2f, 4f};
    private final float[] phaseY = {1f, 3f, 5f};
    private final long startTime = SystemClock.uptimeMillis();
    private float radius;

    public AnimatedBackground(Context context) {
        super(context);
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
        canvas.drawColor(0xFF020B26);
        if (shaders[0] == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        float t = ANIMATE ? (SystemClock.uptimeMillis() - startTime) / 1000f : 0f;
        for (int i = 0; i < BLOBS; i++) {
            float x = w * (0.5f + 0.38f * (float) Math.sin(t * speedX[i] + phaseX[i]));
            float y = h * (0.5f + 0.38f * (float) Math.cos(t * speedY[i] + phaseY[i]));
            canvas.save();
            canvas.translate(x, y);
            paint.setShader(shaders[i]);
            canvas.drawCircle(0, 0, radius, paint);
            canvas.restore();
        }
        if (ANIMATE) {
            postInvalidateDelayed(50);
        }
    }
}
