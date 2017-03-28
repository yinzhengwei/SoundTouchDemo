package com.yzw.soundtouch;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.smp.soundtouchandroid.SoundStreamAduioRecorder;
import com.smp.soundtouchandroid.SoundTouch;

import java.io.File;

public class RecordActivity extends Activity {

    private static final String TAG = RecordActivity.class.getSimpleName();

    private SoundStreamAduioRecorder soundTouchRec;
    private TextView recorderText;
    private TextView playerText;
    private LinearLayout recorderLayout;
    private LinearLayout playerLayout;
    private ImageView recorderImage;
    private ImageView playerImage;

    private TextView pitchShow;
    private SeekBar pitchSeekBar;
    private Button pitchResetButton;

    private TextView tempoShow;
    private SeekBar tempoSeekBar;
    private Button tempoResetButton;

    private String lastRecordFile;

    private boolean isPlaying = false;
    private boolean finishEncodeDecode = false;
    private boolean needEncodeDecode = false;

    private TextView log;

    private SoundTouch soundTouch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        recorderText = (TextView) findViewById(R.id.text_recorder);
        recorderLayout = (LinearLayout) findViewById(R.id.layout_recorder);
        recorderImage = (ImageView) findViewById(R.id.image_recorder);
        recorderLayout.setOnTouchListener(recordTouchedListener);

        playerText = (TextView) findViewById(R.id.text_player);
        playerLayout = (LinearLayout) findViewById(R.id.layout_player);
        playerImage = (ImageView) findViewById(R.id.image_player);
        playerLayout.setOnClickListener(playListener);
        playerLayout.setClickable(false);

        pitchSeekBar = (SeekBar) findViewById(R.id.pitch_seek);
        pitchSeekBar.setOnSeekBarChangeListener(onPitchSeekBarListener);
        pitchResetButton = (Button) findViewById(R.id.button_reset_pitch);
        pitchResetButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                pitchSeekBar.setProgress(1000);
                onPitchSeekBarListener.onStartTrackingTouch(pitchSeekBar);
                onPitchSeekBarListener.onProgressChanged(pitchSeekBar, 1000, false);
                onPitchSeekBarListener.onStopTrackingTouch(pitchSeekBar);
            }
        });

        pitchShow = (TextView) findViewById(R.id.pitch_show);

        tempoSeekBar = (SeekBar) findViewById(R.id.tempo_seek);
        tempoSeekBar.setOnSeekBarChangeListener(onTempoSeekBarListener);
        tempoResetButton = (Button) findViewById(R.id.button_reset_tempo);
        tempoResetButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                tempoSeekBar.setProgress(5000);
                onTempoSeekBarListener.onStartTrackingTouch(tempoSeekBar);
                onTempoSeekBarListener.onProgressChanged(tempoSeekBar, 5000, false);
                onTempoSeekBarListener.onStopTrackingTouch(tempoSeekBar);
            }
        });

        tempoShow = (TextView) findViewById(R.id.tempo_show);

        soundTouch = new SoundTouch(0, 2, 1, 2, 1, 1);
        soundTouchRec = new SoundStreamAduioRecorder(this, soundTouch);

        log = (TextView) findViewById(R.id.log);
        log.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    protected void startEncodeDecode() {
        if (needEncodeDecode && !finishEncodeDecode && lastRecordFile != null) {
            soundTouchRec.stopRecord();
            log.setText("开始加密解密\n" + log.getText());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorderText.setText("按住录音");
        recorderImage.setImageResource(R.drawable.record);
        playerLayout.setClickable(false);

        playerText.setText("播放");
        playerImage.setImageResource(R.drawable.play);
        isPlaying = false;

        soundTouchRec.stopRecord();
        soundTouchRec.stopPlay(false);
        log.setText("退出，暂停全部\n" + log.getText());
    }

    private OnTouchListener recordTouchedListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    soundTouchRec.stopPlay(false);
                    playerText.setText("播放");
                    playerImage.setImageResource(R.drawable.play);
                    isPlaying = false;

                    soundTouchRec.startRecord();
                    recorderText.setText("松开结束");
                    recorderImage.setImageResource(R.drawable.stop);
                    playerLayout.setClickable(false);
                    log.setText("开始录音\n" + log.getText());
                    break;
                case MotionEvent.ACTION_UP:
                    recorderText.setText("按住录音");
                    lastRecordFile = soundTouchRec.stopRecord();
                    recorderImage.setImageResource(R.drawable.record);
                    playerLayout.setClickable(true);

                    log.setText("录音完成" + lastRecordFile + "\n" + log.getText());
                    finishEncodeDecode = false;
                    startEncodeDecode();
                    break;
            }
            return true;
        }
    };

    private OnClickListener playListener = new OnClickListener() {

        public void onClick(View v) {
            if (isPlaying) {
                log.setText("停止播放\n" + log.getText());
                soundTouchRec.stopPlay();
                playerText.setText("播放");
                playerImage.setImageResource(R.drawable.play);
            } else {
                log.setText("开始播放" + getFileToPlay() + "\n" + log.getText());
                soundTouchRec.startPlay(getFileToPlay());
                playerText.setText("停止");
                playerImage.setImageResource(R.drawable.stop);
            }
            isPlaying = !isPlaying;
        }
    };

    private OnSeekBarChangeListener onPitchSeekBarListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            float pitch = (seekBar.getProgress() - 1000) / 100.0f;
            soundTouch.setPitchSemi(pitch);
            playerLayout.setClickable(true);
            log.setText("修改音高\n" + log.getText());
            if (isPlaying) {
                soundTouchRec.stopPlay();
                soundTouchRec.startPlay(getFileToPlay());
                log.setText("开始播放" + getFileToPlay() + "\n" + log.getText());
                playerText.setText("停止");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            playerLayout.setClickable(false);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            float pitch = (progress - 1000) / 100.0f;
            pitchShow.setText("音调: " + pitch);
        }
    };

    private OnSeekBarChangeListener onTempoSeekBarListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            float tempo = (seekBar.getProgress() - 5000) / 100.0f;
            soundTouch.setTempoChange(tempo);
            log.setText("修改速度\n" + log.getText());
            playerLayout.setClickable(true);
            if (isPlaying) {
                soundTouchRec.stopPlay();
                soundTouchRec.startPlay(getFileToPlay());
                log.setText("开始播放" + getFileToPlay() + "\n" + log.getText());
                playerText.setText("停止");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            playerLayout.setClickable(false);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            float tempo = (progress - 5000) / 100.0f;
            tempoShow.setText("速度: " + tempo + "%");
        }
    };

    protected String getFileToPlay() {
        return lastRecordFile;
    }

    public void encodeDecodeFinished() {
        finishEncodeDecode = true;
        if (isPlaying) {
            soundTouchRec.stopPlay();
            RecordActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.setText("开始播放" + getFileToPlay() + "\n" + log.getText());
                }
            });

            soundTouchRec.startPlay(getFileToPlay());
        }
    }
}
