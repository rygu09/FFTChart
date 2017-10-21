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
    private TextView tv_Hz;

    private TextView tv_dB_1;
    private TextView tv_dB_2;

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

        spectrum =  findViewById(R.id.spectrum);
        scale =  findViewById(R.id.freqscale);
        tv_Hz = findViewById(R.id.tv_Hz);

        tv_dB_1 = findViewById(R.id.tv_dB_1);
        tv_dB_2 = findViewById(R.id.tv_dB_2);


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

        // Data
        protected double fps;

        private AudioRecord audioRecord;

        private static final int OVERSAMPLE = 4;
        private static final int SAMPLES = 4096;
        private static final int RANGE = SAMPLES / 2;
        private static final int STEP = SAMPLES / OVERSAMPLE;

        private static final int N = 4;
        private static final int M = 16;

        private long counter;

        private Thread thread;
        private short data[];
        private double buffer[];

        private double xr[];//实部数组
        private double xi[];//虚部数组

        protected double[] level_dB_fft;//换算成dB后的数组
        protected double xa[];//幅值数组

        // Constructor
        public Audio() {
            data = new short[STEP];  /**  data[1024]     STEP = SAMPLES / OVERSAMPLE  */
            buffer = new double[SAMPLES];/**  buffer[4096]   */

            xr = new double[SAMPLES];/**  xr[4096]   */
            xi = new double[SAMPLES];/**  xi[4096]   */

            level_dB_fft = new double[RANGE];
            xa = new double[RANGE];/**  xa[2048]   */
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

            // Calculate fps
            fps = (double) sample / SAMPLES;/*********  44100/4096 **********/

            // Start recording
            audioRecord.startRecording();

            // Continue until the thread is stopped
            while (thread != null) {
                // Read a buffer of data
                /**
                 * 每次读1024个点
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

                for (int i = 0; i < SAMPLES; i++)
                    xr[i] = buffer[i];

                //构造要fft的数组
                Complex[] data_need_process=new Complex[4096];
                for (int i = 0; i < data_need_process.length; i++) {
                    data_need_process[i] = new Complex(xr[i], xi[i]);
                }

                //进行fft
                //得到实部+虚部的数组
                Complex[] Y = fft(data_need_process);

                // Process FFT output
                //将数组换算成dB
                for (int i = 0; i < RANGE; i++) {
                    xa[i] = Y[i].abs(); //取模
                    xa[i] = xa[i]/(SAMPLES/2);//换算成实际幅值
                    if(xa[i]<1)
                        xa[i]=1;

                    //-90.3=20log(1/32768)
                    level_dB_fft[i] =10.0* Math.log10(( xa[i] /32768) *( xa[i] /32768))+90.3;

                    if(level_dB_fft[i]<0)
                        level_dB_fft[i]=0;
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

                // Level
                double level = 0.0;
/**
 * 计算4096个点的平均dB值
 */
                //16bit采样 2的16次方/2=32768
                for (int i = 0; i < SAMPLES; i++)
                    level += ( buffer[i] / 32768.0) *
                            ( buffer[i] / 32768.0);

                level = level / SAMPLES;

                double dB = Math.log10(level) * 10.0 + 90.3;
/**
 * 计算1024个点的平均dB值
 */
                double level2=0;
                for (int i = 0; i < STEP; i++){

                    level2 += ((double) data[i] / 32768.0) *
                            ((double) data[i] / 32768.0);
                }
                level2 = level2 / STEP;
                double dB2 = Math.log10(level2) * 10.0 + 90.3;

                final String s1 = String.format(Locale.getDefault(),
                            "%1.1fdB", dB);
                final String s2 = String.format(Locale.getDefault(),
                            "%1.1fdB", dB2);
                tv_dB_1.post(new Runnable() {
                    @Override
                    public void run() {
                        tv_dB_1.setText("每4096个点取均值"+"  "+s1);
                        tv_dB_2.setText("每1024个点取均值"+"  "+s2);
                    }
                });

            }

            // Stop and release the audio recorder
            cleanUpAudioRecord();
        }

        /**
         * fft函数
         * @param x
         * @return
         */

        private Complex[] fft(Complex[] x) {
            int N = x.length;

            // base case
            if (N == 1) return new Complex[] { x[0] };

            // radix 2 Cooley-Tukey FFT
            if (N % 2 != 0) { throw new RuntimeException("N is not a power of 2"); }

            // fft of even terms
            Complex[] even = new Complex[N/2];
            for (int k = 0; k < N/2; k++) {
                even[k] = x[2*k];
            }
            Complex[] q = fft(even);

            // fft of odd terms
            Complex[] odd  = even;  // reuse the array
            for (int k = 0; k < N/2; k++) {
                odd[k] = x[2*k + 1];
            }
            Complex[] r = fft(odd);

            // combine
            Complex[] y = new Complex[N];
            for (int k = 0; k < N/2; k++) {
                double kth = -2 * k * Math.PI / N;
                Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
                y[k]       = q[k].plus(wk.times(r[k]));
                y[k + N/2] = q[k].minus(wk.times(r[k]));
            }
            return y;
        }

    }


}
