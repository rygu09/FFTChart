界面图

![image](https://github.com/rygu09/image_cache/blob/master/QQ%E6%88%AA%E5%9B%BE20180101215819.png)

1. 运用Android的AudioRecord类收集麦克风的声音
```
audioRecord.startRecording(); //开始接收声音，Starts recording from the AudioRecord instance
audioRecord.read(); //开始读取PCM数据流，Reads audio data from the audio hardware for recording into a short array
```
2. 通过FFT将时域信号数据转化为频域，信号值转化为dB分贝

3. 运用自定义view绘制中的ondraw()方法去绘制坐标、数据值

```
canvas.drawLine/drawtext
```


4. 通过seekbar滑动监听获取点击位置

```
SeekBar.OnSeekBarChangeListener
```

5. 调用postInvalidate()刷新界面
