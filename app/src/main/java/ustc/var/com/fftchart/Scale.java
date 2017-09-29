////////////////////////////////////////////////////////////////////////////////
//
//  Scope - An Android scope written in Java.
//
//  Copyright (C) 2014	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package ustc.var.com.fftchart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

// Scale
public class Scale extends View {
    private static final int WIDTH_FRACTION = 16;

    private int width;
    private int height;

    private Paint paint;

    protected MainActivity.Audio audio;


    // Constructor
    public Scale(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create paint
        paint = new Paint();
    }

    // onMeasure
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get offered dimension
        int h = MeasureSpec.getSize(heightMeasureSpec);

        // Set wanted dimensions
        setMeasuredDimension(h / WIDTH_FRACTION, h);
    }

    // onSizeChanged
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get actual dimensions
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStrokeWidth(2);
        paint.setColor(Color.BLACK);
        paint.setTextSize(width * 1 / 2);
//        paint.setTextAlign(Paint.Align.RIGHT);
//        canvas.translate(0, height);
//        canvas.scale(1, -1);



        int index = 0;
        // Draw scale ticks

        float yscale = 100 / height;

//        float[] amplitude = {0,20, 40, 60, 80, 100,120,140,160};
        float[] amplitude = {120, 100, 80, 60, 40, 20, 0};


//        for (float a : amplitude) {
//            float y = (float) (a / audio.fps / yscale);
//            String s = String.format(Locale.getDefault(),
//                    "%1.0f", a);
//            canvas.drawText(s, y, 0, paint);
//        }

        for (int i = 4; i < height; i += MainActivity.SIZE *5) {

//            canvas.drawLine(width * 4 / 5, i, width, i, paint);
            String s = String.format(Locale.getDefault(),
                    "%1.0f", (float)amplitude[index]);
            canvas.drawText(s, 0, i, paint);
            index++;
        }


//        String s = String.format(Locale.getDefault(),
//                "%1.0f", f * m);
//
//        paint.setAntiAlias(true);
//        canvas.drawText(s, x, height - (height / 6), paint);
//        paint.setAntiAlias(false);
//        paint.setAntiAlias(true);
//        canvas.drawText(s, 0, width * 2 / 3, paint);


    }
}


