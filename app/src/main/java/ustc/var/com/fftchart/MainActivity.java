package ustc.var.com.fftchart;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private Spectrum spectrum;
    private FreqScale scale;
    private TextView text;
    private TextView text3;

    private SeekBar mSeekBar;

    private Audio audio;

    protected static final int SIZE = 20;
    protected static final float SMALL_SCALE = 200;
    protected static final float LARGE_SCALE = 200000;


    // On create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spectrum = (Spectrum) findViewById(R.id.spectrum);
        scale = (FreqScale) findViewById(R.id.freqscale);
        text = findViewById(R.id.textView2);
        text3 = findViewById(R.id.textView3);

        mSeekBar = findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);

        audio = new Audio();

        if (spectrum != null)
            spectrum.audio = audio;

        if (scale != null)
            scale.audio = audio;
    }

    // On Resume
    @Override
    protected void onResume() {
        super.onResume();

        // Start the audio thread
        audio.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop audio thread
        audio.stop();
    }

    /**
     *
     * @param seekBar
     * @param i
     * @param b
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
          spectrum.index=i;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    // Audio
    protected class Audio implements Runnable {
        private static final String TAG = "Spectrum";

        protected int input;
        protected int sample;
        protected boolean lock;
        protected boolean fill;

        // Data
        protected double frequency;
        protected double fps;

        private AudioRecord audioRecord;

        private static final int OVERSAMPLE = 4;
        private static final int SAMPLES = 4096;
        private static final int RANGE = SAMPLES / 2;
        private static final int STEP = SAMPLES / OVERSAMPLE;

        private static final int N = 4;
        private static final int M = 16;

        private static final double MIN = 0.5;
        private static final double expect = 2.0 * Math.PI * STEP / SAMPLES;

        private long counter;

        private Thread thread;
        private short data[];
        private double buffer[];

        private double xr[];
        private double xi[];

        protected double xa[];

        private double xp[];
        private double xf[];


        // Constructor
        public Audio() {
            data = new short[STEP];  /**  data[1024]     STEP = SAMPLES / OVERSAMPLE  */
            buffer = new double[SAMPLES];/**  buffer[4096]   */

            xr = new double[SAMPLES];/**  xr[4096]   */
            xi = new double[SAMPLES];/**  xi[4096]   */

            xa = new double[RANGE];/**  xa[2048]   */
            xp = new double[RANGE];
            xf = new double[RANGE];
        }

        // Start audio
        protected void start() {
            // Start the thread
            thread = new Thread(this, "Audio");
            thread.start();
        }

        // Run
        @Override
        public void run() {
            processAudio();
        }

        // Stop
        protected void stop() {
            // Stop and release the audio recorder
            cleanUpAudioRecord();

            Thread t = thread;
            thread = null;

            // Wait for the thread to exit
            while (t != null && t.isAlive())
                Thread.yield();
        }

        // Stop and release the audio recorder
        private void cleanUpAudioRecord() {
            if (audioRecord != null &&
                    audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

                if (audioRecord.getRecordingState() ==
                        AudioRecord.RECORDSTATE_RECORDING)
                    audioRecord.stop();

                audioRecord.release();
            }
        }

        // Process Audio
        protected void processAudio() {
            // Assume the output sample will work on the input as
            // there isn't an AudioRecord.getNativeInputSampleRate()
            /*****  44100  *******/
            sample =
                    AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

            // Get buffer size
            int size =
                    AudioRecord.getMinBufferSize(sample,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

            // Create the AudioRecord object
            audioRecord =
                    new AudioRecord(input, sample,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            size);

//            // Check state
//            int state = audioRecord.getState();
//
//            if (state != AudioRecord.STATE_INITIALIZED) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        showAlert(R.string.app_name,
//                                R.string.error_init);
//                    }
//                });
//
//                audioRecord.release();
//                thread = null;
//                return;
//            }

            // Calculate fps
            fps = (double) sample / SAMPLES;/*********  44100/4096 **********/

            // Start recording
            audioRecord.startRecording();

            // Max data
            double dmax = 0.0;

            // Continue until the thread is stopped
            while (thread != null) {
                // Read a buffer of data
                /**
                 * 每次从buffer[4096]中读1024个点
                 */
                size = audioRecord.read(data, 0, STEP);

                // Stop the thread if no data or error state
                if (size <= 0) {
                    thread = null;
                    break;
                }
/**
 * 应该是为了更新数据,每次将buffer[]中的后SAMPLE-STEP移到开始,data[i]给后STEP个数据
 */
                // Move the main data buffer up
                //将buffer中的从STEP开始的SAMPLES - STEP长度的数据拷贝
                //拷贝位置是buffer中的0位开始
                System.arraycopy(buffer, STEP, buffer, 0, SAMPLES - STEP);

                //再将data[i]赋值给buffer中的SAMPLES - STEP以后的数据，长度为STEP
                for (int i = 0; i < STEP; i++)
                    buffer[(SAMPLES - STEP) + i] = data[i];

                // Maximum value
                if (dmax < 4096.0)
                    dmax = 4096.0;

                // Calculate normalising value
                //计算归一化值
                double norm = dmax;

                dmax = 0.0;

                // Copy data to FFT input arrays
                for (int i = 0; i < SAMPLES; i++) {
                    // Find the magnitude
                    if (dmax < Math.abs(buffer[i]))
                        dmax = Math.abs(buffer[i]);

                    // Calculate the window
                    double window =
                            0.5 - 0.5 * Math.cos(2.0 * Math.PI *
                                    i / SAMPLES);
/******* 将buffer[i]赋值给xr[i] ********/
                    // Normalise and window the input data
                    xr[i] = buffer[i] / norm * window;
//                      xr[i] = buffer[i];

                }

                // do FFT
                fftr(xr, xi);

                // Process FFT output
                for (int i = 1; i < RANGE; i++) {
                    double real = xr[i];
                    double imag = xi[i];

                    xa[i] = Math.hypot(real, imag);

                    // Do frequency calculation
                    double p = Math.atan2(imag, real);
                    double dp = xp[i] - p;

                    xp[i] = p;

                    // Calculate phase difference
                    dp -= i * expect;

                    int qpd = (int) (dp / Math.PI);

                    if (qpd >= 0)
                        qpd += qpd & 1;

                    else
                        qpd -= qpd & 1;

                    dp -= Math.PI * qpd;

                    // Calculate frequency difference
                    double df = OVERSAMPLE * dp / (2.0 * Math.PI);

                    // Calculate actual frequency from slot frequency plus
                    // frequency difference and correction value
                    xf[i] = i * fps + df * fps;
                }

                // Do a full process run every N
                //每N个点计算一次
                if (++counter % N != 0)
                    continue;

/**重绘  Update spectrum
 * postInvalidate(); 与invalidate()方法区别就是，postInvalidate()方法可以在UI线程执行，也可以在工作线程执行
   而invalidate()只能在UI线程操作。但是从重绘速率讲：invalidate()效率高。
  */
                spectrum.postInvalidate();

                // Update frequency and dB every M
                //每M个点重绘一次
                if (counter % M != 0)
                    continue;
//
//                // Maximum FFT output
//                double max = 0.0;
//
//                // Find maximum value
//                for (int i = 1; i < RANGE; i++) {
//                    if (xa[i] > max) {
//                        max = xa[i];
//                        frequency = xf[i];
//                    }
//                }
//
                // Level
                double level = 0.0;
/**
 * 计算1024个点的平均dB值
 */
//                for (int i = 0; i < STEP; i++)
//                    level += ((double) data[i] /1) *
//                            ((double) data[i] / 1);
//                level = level / STEP;
//                double dB = Math.log10(level) * 10.0;
//                final String s = String.format(Locale.getDefault(),
//                        "%1.1fdB", dB);
//                text.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        text.setText(s);
//                    }
//                });
                //16bit采样 2的16次方/2=32768
                for (int i = 0; i < STEP; i++)
                    level += ((double) data[i] / 32768.0) *
                            ((double) data[i] / 32768.0);

                level = level / STEP;

                double dB = Math.log10(level) * 10.0 +90;

                double level2=0;
                for (int i = 0; i < STEP; i++){

                    level2 += ((double) data[i] /1) *
                            ((double) data[i] / 1);
                }
                level2 = level2 / STEP;
                double dB2 = Math.log10(level2) * 10.0;


                frequency = 0.0;
                final String s = String.format(Locale.getDefault(),
                            "%1.1fdB", dB);
                final String s3 = String.format(Locale.getDefault(),
                            "%1.1fdB", dB2);
                text.post(new Runnable() {
                    @Override
                    public void run() {
                        text.setText(s);
                        text3.setText(s3);
                    }
                });

            }

            // Stop and release the audio recorder
            cleanUpAudioRecord();
        }

        /**
         * 这里是fft处理
         * @param ar
         * @param ai
         */
        // Real to complex FFT, ignores imaginary values in input array
        //ar是实部，ai是虚部
        private void fftr(double ar[], double ai[]) {
            final int n = ar.length;
            final double norm = Math.sqrt(1.0 / n);

            for (int i = 0, j = 0; i < n; i++) {
                if (j >= i) {
                    double tr = ar[j] * norm;

                    ar[j] = ar[i] * norm;
                    ai[j] = 0.0;

                    ar[i] = tr;
                    ai[i] = 0.0;
                }

                int m = n / 2;
                while (m >= 1 && j >= m) {
                    j -= m;
                    m /= 2;
                }
                j += m;
            }

            for (int mmax = 1, istep = 2 * mmax; mmax < n;
                 mmax = istep, istep = 2 * mmax) {
                double delta = Math.PI / mmax;
                for (int m = 0; m < mmax; m++) {
                    double w = m * delta;
                    double wr = Math.cos(w);
                    double wi = Math.sin(w);

                    for (int i = m; i < n; i += istep) {
                        int j = i + mmax;
                        double tr = wr * ar[j] - wi * ai[j];
                        double ti = wr * ai[j] + wi * ar[j];
                        ar[j] = ar[i] - tr;
                        ai[j] = ai[i] - ti;
                        ar[i] += tr;
                        ai[i] += ti;
                    }
                }
            }
        }
    }


}
