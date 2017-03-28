package com.yzw.soundtouch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smp.soundtouchandroid.SoundStreamAudioPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView text_player_play;
    private TextView text_player_stop;

    private float tempo = 1.0f;//这个是速度，1.0表示正常设置新的速度控制值，
    private float pitchSemi = 1.0f;//这个是音调，1.0表示正常，
    private float rate = 1.0f;//这个参数是变速又变声的，这个参数大于0，否则会报错

    private TextView tempo_show;
    private TextView pitch_show;
    private TextView rate_show;
    private SeekBar tempo_seek;
    private SeekBar pitch_seek;
    private SeekBar rate_seek;

    private RadioGroup radiogroup;

    private SoundStreamAudioPlayer player;
    private File f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_player_play = (TextView) findViewById(R.id.text_player);
        text_player_play.setOnClickListener(this);

        text_player_stop = (TextView) findViewById(R.id.text_player_stop);
        text_player_stop.setOnClickListener(this);

        tempo_show = (TextView) findViewById(R.id.tempo_show);
        pitch_show = (TextView) findViewById(R.id.pitch_show);
        rate_show = (TextView) findViewById(R.id.rate_show);

        tempo_seek = (SeekBar) findViewById(R.id.tempo_seek);
        tempo_seek.setOnSeekBarChangeListener(onTempoSeekBarListener);

        pitch_seek = (SeekBar) findViewById(R.id.pitch_seek);
        pitch_seek.setOnSeekBarChangeListener(onPitchSeekBarListener);

        rate_seek = (SeekBar) findViewById(R.id.rate_seek);
        rate_seek.setOnSeekBarChangeListener(onRateSeekBarListener);

        radiogroup = (RadioGroup) findViewById(R.id.radiogroup);
        radiogroup.setOnCheckedChangeListener(onCheckedChangeListener);

        findViewById(R.id.text_player_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
            }
        });
        initPlayer();
    }

    private void initPlayer() {
        f = new File("/sdcard/bjbj.mp3");
        if (!f.exists()) {
            try {
                //InputStream is = this.getResources().openRawResource(R.raw.bjbj);
                InputStream is = this.getResources().getAssets().open("bjbj.mp3");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private SeekBar.OnSeekBarChangeListener onRateSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float r = seekBar.getProgress() - 50;
            Log.e("AA", "onStopTrackingTouch  pitch" + r + "%");
            rate = r;
            if (player != null)
                player.setRateChange(r);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float r = progress - 50;
            rate_show.setText("变速变音率: " + r);

        }
    };
    private SeekBar.OnSeekBarChangeListener onPitchSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float pi = seekBar.getProgress() - 12;
            Log.e("AA", "onStopTrackingTouch  pitch" + pi + "%");
            pitchSemi = pi;
            if (player != null)
                player.setPitchSemi(pitchSemi);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float pitch = progress - 12;
            pitch_show.setText("音调: " + pitch);
        }
    };

    private SeekBar.OnSeekBarChangeListener onTempoSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float te = seekBar.getProgress() - 50;
            tempo = te;
            if (player != null)
                player.setTempoChange(tempo);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float t = progress - 50;
            tempo_show.setText("速度: " + t);
        }
    };

    private RadioGroup.OnCheckedChangeListener onCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int id) {
            //取值范围是1，2两个声道
            if (player != null)
                if (id == R.id.radiobutton_left) {
                    player.setChannels(1);
                } else {
                    player.setChannels(2);
                }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.text_player:
                try {
                    if (player != null)
                        return;
                    player = new SoundStreamAudioPlayer(0, f.getPath(), tempo, pitchSemi);

                    //player.setChannels(1);//取值范围是1，2两个声道
                    //player.setPitchSemi(9.0f);//取值范围是-12到12，超过声音会很差
                    //player.setTempoChange(-10.0f);//设置变速不变调，取值范围是-50到100超过则不处理
                    //player.setRateChange(50f);//设置声音的速率，取值范围是-50到100超过则不处理
                    //mSoundTouch.setSampleRate(sampleRate);//设置声音的采样频率
                    //m_SoundTouch.setPitchSemiTones(pitchDelta);//设置声音的pitch
                    //quick是一个bool变量，USE_QUICKSEEK具体有什么用我暂时也不太清楚。
                    //mSoundTouch.setSetting(SETTING_USE_QUICKSEEK, quick);
                    //noAntiAlias是一个bool变量，USE_AA_FILTER具体有什么用我暂时也不太清楚。
                    //mSoundTouch.setSetting(SETTING_USE_AA_FILTER, !(noAntiAlias));

                    //这个参数是变速又变声的，这个参数大于0，否则会报错
                    player.setRate(rate);

                    new Thread(player).start();
                    player.start();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.text_player_stop:
                try {
                    if (player != null)
                        player.stop();
                    player = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
