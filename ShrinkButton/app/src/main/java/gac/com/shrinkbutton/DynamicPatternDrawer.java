package gac.com.shrinkbutton;

import android.graphics.Canvas;

public interface DynamicPatternDrawer {
    void onDrawPattern(Canvas canvas, int width, int height, float progressFraction);
}