package com.liskovsoft.smartyoutubetv2.tv.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

/**
 * MOD (Inge): hoofdkleur van een afbeelding: gemiddelde met meer gewicht voor verzadigde kleuren,
 * daarna wat donkerder en voller gemaakt zodat witte tekst erop leesbaar blijft.
 */
public final class ColorExtract {
    private ColorExtract() {
    }

    public static int dominant(Bitmap bitmap, int fallback) {
        try {
            Bitmap small = Bitmap.createScaledBitmap(bitmap, 24, 24, true);
            double r = 0, g = 0, b = 0, total = 0;
            float[] hsl = new float[3];
            for (int x = 0; x < small.getWidth(); x++) {
                for (int y = 0; y < small.getHeight(); y++) {
                    int c = small.getPixel(x, y);
                    if (Color.alpha(c) < 128) {
                        continue;
                    }
                    ColorUtils.colorToHSL(c, hsl);
                    double w = 0.05 + hsl[1] * (1 - Math.abs(hsl[2] - 0.5) * 2);
                    r += Color.red(c) * w;
                    g += Color.green(c) * w;
                    b += Color.blue(c) * w;
                    total += w;
                }
            }
            if (total <= 0) {
                return fallback;
            }
            int avg = Color.rgb((int) (r / total), (int) (g / total), (int) (b / total));
            ColorUtils.colorToHSL(avg, hsl);
            hsl[1] = Math.min(1f, hsl[1] * 1.3f);
            hsl[2] = Math.max(0.22f, Math.min(0.38f, hsl[2]));
            return ColorUtils.HSLToColor(hsl);
        } catch (Exception e) {
            return fallback;
        }
    }
}
