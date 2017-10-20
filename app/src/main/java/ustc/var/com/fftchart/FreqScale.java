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

// FreqScale
public class FreqScale extends View
{
    private static final int HEIGHT_FRACTION = 16;
    private int width;
    private int height;

    private Paint paint;

    protected MainActivity.Audio audio;

    // Constructor
    public FreqScale(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Create paint
        paint = new Paint();
    }

    // onMeasure
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get offered dimension
        int w = MeasureSpec.getSize(widthMeasureSpec);

        // Set wanted dimensions
        setMeasuredDimension(w, w / HEIGHT_FRACTION);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get actual dimensions
        width = w;
        height = h;
    }

    // onDraw
    @Override
    protected void onDraw(Canvas canvas)
    {
        // Check for data
        if ((audio != null) && (audio.xa != null))
        {
            // Calculate scale
            //audio.xa.length=2048
            float scale = (float) Math.log(2048) / (float) width;

            // Set up paint
            paint.setStrokeWidth(2);
            paint.setColor(Color.BLACK);
            paint.setTextSize(height * 2 / 3);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setAntiAlias(true);

            // Draw ticks
            canvas.drawLine(0, 0, 0, height / 3, paint);

            float fa[] = {1};
            float sa[] = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f };
            float ma[] = {100, 1000, 10000};
            for (float m : ma)
            {
                for (float f : fa)
                {
                    float x = (float) Math.log((f * m) / audio.fps) / scale;
                    canvas.drawLine(x, 0, x, height / 3, paint);

                    String s;
                    if (m >= 1000)
                        s = String.format(Locale.getDefault(),
                                          "%1.0fK", f * m / 1000);
                    else
                        s = String.format(Locale.getDefault(),
                                          "%1.0f", f * m);

                    canvas.drawText(s, x, height - (height / 6), paint);
                }

                for (float s : sa)
                {
                    float x = (float) Math.log((s * m) / audio.fps) / scale;
                    canvas.drawLine(x, 0, x, height / 6, paint);
                }
            }
        }
    }
}
