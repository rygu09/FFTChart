////////////////////////////////////////////////////////////////////////////////
//
//  Scope - An Android scope written in Java.
//
//  Copyright (C) 2014	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

// Spectrum
public class Spectrum extends View {
    private int width;
    private int height;

    private Path path;
    private Path fillPath;
    private Paint paint;

    private Bitmap graticule;

    private float max;

    protected MainActivity.Audio audio;

    protected float index;

    // Spectrum
    public Spectrum(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create paths and paint
        path = new Path();
        fillPath = new Path();
        paint = new Paint();
    }

    // On size changed
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Get dimensions
        width = w;
        height = h;
    }

    // On draw
    @Override
    protected void onDraw(Canvas canvas) {
        // Check for data
        if ((audio == null) || (audio.level_dB_fft == null)) {
            canvas.drawColor(Color.BLACK);
            return;
        }

        // Calculate x scale
        float xscale = (float) Math.log(audio.level_dB_fft.length) / width;
        // Calculate the scaling
        //canvas y坐标的最大值 大概取了下100
        float yscale = (height / 100);

        // Create graticule
        if (graticule == null || graticule.getWidth() != width ||
                graticule.getHeight() != height) {
            // Create a bitmap for the graticule
            graticule = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(graticule);

            // Black background
            c.drawColor(Color.WHITE);

            // Set up paint
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.argb(30,0,0,0));

            //画背景竖线
            float fa[] = {1};
            float ma[] = {1, 10, 100, 1000, 10000};
            for (float m : ma) {
                for (float f : fa) {
                    /**
                     * x其实是 第几个点对应的 手机屏幕中的位置
                     * 如Fn=(n-1)*Fs/N, Fn是实际频率点，
                     * x=width* log(Fn/ (44100/4096) )/ log2048
                     */
                    float x = (float) Math.log((f * m) / audio.fps) / xscale;
                    c.drawLine(x, 0, x, height, paint);
                }
            }

            //画背景横线
            for (int i = 0; i < height; i += MainActivity.SIZE * 5) {
                c.drawLine(0, i, width, i, paint);
            }
        }
        canvas.translate(0, height);
        canvas.scale(1, -1);

        // Draw the graticule
        canvas.drawBitmap(graticule, 0, 0, null);

//        // Rewind path
//        // path.rewind():清除掉path里的线条和曲线，但是会保留内部的数据结构以便重用；
//        path.rewind();
//        path.moveTo(0, 0);
/**
 * ------------------      画数据       ---------------------------
 */
        double[] fn=new double[2048];
        double[] x_fn=new double[2048];
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3.5f);
//        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<2048;i++){
            fn[i]=(i-1)*44100/4096;
            x_fn[i]=Math.log(fn[i] / audio.fps) / xscale;
            canvas.drawLine((float) x_fn[i], 0, (float) x_fn[i], (float) audio.level_dB_fft[i]*yscale, paint);
        }

        // Draw index
        //就是seekbar滑动时在频谱canvas中滑动的竖线
        if (index > 0 && index < width) {
            // Blue index
            paint.setColor(Color.argb(200,0,0,255));
            paint.setStrokeWidth(1.8f);
            float gap=6f;
            canvas.drawLine(index-gap, 0, index-gap, height-10, paint);
            canvas.drawLine(index+gap, 0, index+gap, height-10, paint);
            canvas.drawLine(index-gap, height-10, index+gap, height-10, paint);

//             Draw frequency value
            canvas.scale(1, -1);
            /**
             * x=log(s/fps)/scale,反求s即是string s,s为频率
             */
            float frequency=(float) Math.pow(Math.E, index * xscale) * 44100 / 4096;
            String s = String.format(Locale.getDefault(), "%1.1fHz",
                    frequency);
            paint.setStrokeWidth(2);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize(height /16);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(s, index, -height / 2, paint);

            int n=Math.round(frequency/(44100/4096)+1);

            String s_value = String.format(Locale.getDefault(), "%1.1fdB",
                    (float) audio.level_dB_fft[n]);
            canvas.drawText(s_value, index, -height / 2 - height / 12, paint);

        }

//        // Create trace
//        int last = 1;
//        float[] value_arr = new float[Math.round(width)];
//        for (int x = 0; x < width; x++) {
//            float value = 0.0f;
//
//            int index = (int) Math.round(Math.pow(Math.E, x * xscale));
//            if (index == last)
//                continue;
//
//            for (int i = last; i <= index; i++) {
//                // Don't show DC component and don't overflow
//                if (i > 0 && i < audio.level_dB_fft.length) {
//                    if (value < audio.level_dB_fft[i])
//                        value = (float) audio.level_dB_fft[i];
//                }
//            }
//
//            // Update last index
//            last = index;
//
//            value_arr[x] = value * yscale;
//            float y = value * yscale;//float yscale = (height / max);
//
//            path.lineTo(x, y);//lineTo(float x, float y) //添加当前点到目标点（x，y）构成的直线到path
//        }
//
//        paint.setAntiAlias(true);
//
//        //始终填充
//        // Copy path
//        fillPath.set(path);
//
//        // Complete path for fill
//        fillPath.lineTo(width, 0);
//        fillPath.close();
//
//        // Colour translucent green
//        paint.setColor(Color.argb(63, 255, 0, 0));
//        paint.setStyle(Paint.Style.FILL);
//
//        // Fill path
//        canvas.drawPath(fillPath, paint);
//
//
//        // Color green
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.STROKE);
//
//        // Draw path
//        canvas.drawPath(path, paint);
//

    }
}

