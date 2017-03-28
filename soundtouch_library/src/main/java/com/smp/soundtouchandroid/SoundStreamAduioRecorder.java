package com.smp.soundtouchandroid;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dongtao on 15/9/17.
 */
public class SoundStreamAduioRecorder {
    private static final String TAG = SoundStreamAduioRecorder.class.getSimpleName();

    private AudioRecord mAudioRecorder = null;

    private AudioTrack mAudioTrack;

    private int minRecBuffSize = 0;

    private int minPlayBufferSize = 0;

    private byte[] recorderBuffer;

    private byte[] playerBuffer;

    private boolean recordingstart = false;

    private boolean playingstart = false;

    private Context context;

    private RecordThread recordThread = null;

    private PlayThread playThread = null;

    private SoundTouch soundTouch;

    public SoundStreamAduioRecorder(Context context, SoundTouch soundtouch) {
        this.soundTouch = soundtouch;
        this.context = context;
    }


    class RecordThread extends Thread {

        private FileOutputStream recordOutputStream = null;

        private String fullFileName;

        RecordThread() {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss_SSS");
            Date curDate = new Date(System.currentTimeMillis());
            String fileName = formatter.format(curDate);

            Log.d(TAG, "recordfile name:" + fileName);

            fullFileName = /* recordFolderPath + */fileName + ".pcm";
            try {
                recordOutputStream = context.openFileOutput(fullFileName, Context.MODE_PRIVATE);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public void run() {
            int totalPCMSamples = 0;
            int totalSTSamples = 0;
            int receiveSTSamples = 0;

            while (recordingstart && recordOutputStream != null) {
                int len = mAudioRecorder.read(recorderBuffer, 0,
                        recorderBuffer.length);

                int inputSamples = len / ((16 * 1) / 8);
                totalPCMSamples += inputSamples;

                if (inputSamples != 0) {
                    try {
                        recordOutputStream.write(recorderBuffer, 0,
                                inputSamples * ((16 * 1) / 8));
                        recordOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }

            Log.d(TAG, "Total input pcm samples:" + totalPCMSamples);
            Log.d(TAG, "total receive ST samoles:" + totalSTSamples);

            mAudioRecorder.stop();
            mAudioRecorder.release();
            recordingstart = false;

            try {
                recordOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class PlayThread extends Thread {

        public static final String TUNED = "tuned";
        private FileInputStream playInputStream = null;
        private FileOutputStream recordOutputStream = null;
        private String fileName;
        private String tuendFileName;

        PlayThread(String fileName) {
            this.fileName = fileName;
            if (fileName != null) {
                try {
                    playInputStream = context.openFileInput(fileName);
                } catch (FileNotFoundException e) {
                }
            }
        }

        @Override
        public void run() {
            int totalPCMSamples = 0;
            int totalSTSamples = 0;
            int receiveSTSamples = 0;

            createTunedFile();

            try {
                playInputStream = context.openFileInput(tuendFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (playingstart && playInputStream != null) {
                int len = 0;
                try {
                    len = playInputStream.read(playerBuffer, 0,
                            playerBuffer.length);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (len == -1) {
                    Log.d(TAG, "file read finish!");
                    try {
                        playInputStream.close();
                        playInputStream = context.openFileInput(tuendFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (len != 0) {
                    mAudioTrack.write(playerBuffer, 0, len);
                }

            }

            mAudioTrack.stop();
            mAudioTrack.release();
            playingstart = false;

            if (playInputStream != null) {
                try {
                    playInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void createTunedFile() {
            tuendFileName = TUNED + fileName;
            try {
                recordOutputStream = context.openFileOutput(tuendFileName,
                        Context.MODE_PRIVATE);

                int totalPCMSamples = 0;
                int totalSTSamples = 0;

                int receiveSTSamples = 0;

                while (playInputStream != null) {
                    int len = playInputStream.read(playerBuffer, 0,
                            playerBuffer.length);

                    if (len == -1) {
                        break;
                    }

                    int inputSamples = len / ((16 * 1) / 8);
                    totalPCMSamples += inputSamples;

                    try {
                        Log.d(TAG, "input ST pcm size :" + inputSamples);
                        soundTouch.putBytes(playerBuffer);
                        do {
                            receiveSTSamples = soundTouch.getBytes(playerBuffer);
                            totalSTSamples += receiveSTSamples;
                            Log.d(TAG, "receive ST pcm samples :"
                                    + receiveSTSamples);
                            if (receiveSTSamples != 0) {
                                recordOutputStream.write(playerBuffer, 0, receiveSTSamples);
                                recordOutputStream.flush();
                            }
                        } while (receiveSTSamples != 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                do {
                    receiveSTSamples = soundTouch.getBytes(playerBuffer);
                    Log.d(TAG, "receive remainder ST samples:"
                            + receiveSTSamples);
                    totalSTSamples += receiveSTSamples;

                    if (receiveSTSamples != 0) {
                        try {
                            recordOutputStream.write(playerBuffer, 0,
                                    receiveSTSamples * ((16 * 1) / 8));
                            recordOutputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } while (receiveSTSamples != 0);
                Log.d(TAG, "Total input pcm samples:" + totalPCMSamples);
                Log.d(TAG, "total receive ST samoles:" + totalSTSamples);
                try {
                    recordOutputStream.close();
                    playInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }


    public void startRecord() {

        if (playThread != null && playThread.isAlive()) {
            Log.w(TAG, "AudioPlayer is running, please stop Player!");
            return;
        }

        if (recordThread != null && recordThread.isAlive()) {
            Log.w(TAG, "AudioRecorder is already start!");
            return;
        }

        minRecBuffSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minRecBuffSize * 3);

        mAudioRecorder.startRecording();

        if (minRecBuffSize != 0) {
            recorderBuffer = new byte[minRecBuffSize * 3];

            recordingstart = true;
            recordThread = new RecordThread();
            recordThread.start();
        }

    }

    public String stopRecord() {
        recordingstart = false;

        String fileName = null;

        if (recordThread != null) {
            fileName = recordThread.fullFileName;
            try {
                recordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordThread = null;
        }

        return fileName;
    }

    public void startPlay(String fileName) {

        if (recordThread != null && recordThread.isAlive()) {
            return;
        }

        if (playThread != null && playThread.isAlive()) {
            return;
        }

        minPlayBufferSize = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minPlayBufferSize * 3, AudioTrack.MODE_STREAM);

        if (minPlayBufferSize != 0) {
            playerBuffer = new byte[minPlayBufferSize * 3];
            mAudioTrack.play();
            playingstart = true;
            playThread = new PlayThread(fileName);
            playThread.start();
        }
    }

    public void stopPlay(boolean wait) {
        playingstart = false;
        if (wait) {
            try {
                playThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        playThread = null;

    }

    public void stopPlay() {
        stopPlay(true);
    }
}
