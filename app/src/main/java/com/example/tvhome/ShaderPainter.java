package com.example.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RuntimeShader;

/**
 * Fondo con shader AGSL (Android 13 o superior): ondas azules y cian que fluyen.
 * Si el shader no compila o el sistema es viejo, AnimatedBackground usa el fondo de manchas.
 */
public class ShaderPainter {

    private static final String SRC =
            "uniform float2 resolution;\n"
            + "uniform float time;\n"
            + "half4 main(float2 fragCoord) {\n"
            + "  float2 uv = fragCoord / resolution;\n"
            + "  float t = time;\n"
            + "  float2 p = uv * float2(resolution.x / resolution.y, 1.0) * 2.2;\n"
            + "  p += 0.35 * float2(sin(p.y * 1.7 + t * 0.35), cos(p.x * 1.5 - t * 0.30));\n"
            + "  p += 0.25 * float2(sin(p.y * 2.9 - t * 0.25 + 1.3), cos(p.x * 2.4 + t * 0.40));\n"
            + "  float v = sin(p.x * 1.6 + t * 0.20) + sin(p.y * 1.9 - t * 0.25) + sin((p.x + p.y) * 1.2 + t * 0.15);\n"
            + "  v = v / 3.0 * 0.5 + 0.5;\n"
            + "  float3 deep = float3(0.01, 0.03, 0.16);\n"
            + "  float3 blue = float3(0.05, 0.28, 1.0);\n"
            + "  float3 cyan = float3(0.0, 0.85, 1.0);\n"
            + "  float3 col = mix(deep, blue, smoothstep(0.15, 0.55, v));\n"
            + "  col = mix(col, cyan, smoothstep(0.55, 0.90, v));\n"
            + "  float2 c = uv - 0.5;\n"
            + "  col *= 1.0 - dot(c, c) * 0.9;\n"
            + "  return half4(half3(col), 1.0);\n"
            + "}\n";

    private final RuntimeShader shader = new RuntimeShader(SRC);
    private final Paint paint = new Paint();

    public ShaderPainter() {
        paint.setShader(shader);
    }

    public void draw(Canvas canvas, int w, int h, float timeSeconds) {
        shader.setFloatUniform("resolution", (float) w, (float) h);
        shader.setFloatUniform("time", timeSeconds);
        canvas.drawRect(0, 0, w, h, paint);
    }
}
