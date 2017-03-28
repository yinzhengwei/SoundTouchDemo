package com.smp.soundtouchandroid;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@SuppressLint("NewApi")
public class MediaCodecAudioEncoder implements AudioEncoder
{
	private MediaCodec codec;
	private MediaFormat format;

	private ByteBuffer[] codecInputBuffers, codecOutputBuffers;

	private static final String TAG = "ENCODE";

	// private long kNumInputBytes;
	private static final long kTimeoutUs = 0;

	private BufferedOutputStream outputStream;
	private boolean doneDequeing;

	private ByteBuffer overflowBuffer;
	private boolean firstSkipped;
	private byte[] chunk;
	private int numBytesSubmitted;
	private int numBytesDequeued;
	private int samplingRate, channels;
	private int samplingRateKey;
	static String testPath;
	static
	{
		String baseDir = Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		testPath = baseDir + "/musicWRITING.aac";
	}

	public MediaCodecAudioEncoder(int samplingRate, int channels)
			throws IOException
	{
		this.samplingRate = samplingRate;
		this.channels = channels;
		samplingRateKey = determineSamplingRateKey(samplingRate);
		codec = MediaCodec.createByCodecName("OMX.google.aac.encoder");
		format = new MediaFormat();
		format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
		format.setInteger(MediaFormat.KEY_AAC_PROFILE,
				MediaCodecInfo.CodecProfileLevel.AACObjectLC); // AAC LC
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplingRate);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channels);
		format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);

		codec.configure(format, null /* surface */, null /* crypto */,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		codec.start();

		codecInputBuffers = codec.getInputBuffers();
		codecOutputBuffers = codec.getOutputBuffers();

		overflowBuffer = ByteBuffer.allocateDirect(8192);
		chunk = new byte[4096];
	}

	private int determineSamplingRateKey(int samplingRate)
	{
		switch (samplingRate)
		{
		case 96000:
			return 0;
		case 88200:
			return 1;
		case 64000:
			return 2;
		case 48000:
			return 3;
		case 44100:
			return 4;
		case 32000:
			return 5;
		case 24000:
			return 6;
		case 22050:
			return 7;
		case 16000:
			return 8;
		case 12000:
			return 9;
		case 11025:
			return 10;
		case 8000:
			return 11;
		case 7350:
			return 12;
		default:
			return 4;
		}
	}

	public void initFileOutput(String fileNameOut) throws IOException
	{
		Log.d("FILE", fileNameOut);
		FileOutputStream fos = new FileOutputStream(fileNameOut);
		outputStream = new BufferedOutputStream(fos);

	}

	@Override
	public int writeChunk(byte[] input, int offsetInBytes, int sizeInBytes)
			throws IOException
	{

		int total = 0;
		if (overflowBuffer.capacity() < sizeInBytes)
			overflowBuffer = ByteBuffer.allocateDirect(sizeInBytes);
		overflowBuffer.clear();
		overflowBuffer.put(input, offsetInBytes, sizeInBytes);
		overflowBuffer.flip();
		while (overflowBuffer.hasRemaining())
		{
			int index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);

			if (index >= 0)
			{
				ByteBuffer buffer = codecInputBuffers[index];
				int chunkSize = Math.min(buffer.capacity(),
						overflowBuffer.remaining());
				if (chunkSize > chunk.length)
					chunk = new byte[chunkSize];
				overflowBuffer.get(chunk, 0, chunkSize);
				buffer.clear();
				buffer.put(chunk);
				codec.queueInputBuffer(index, 0, chunkSize, 0, 0);
				numBytesSubmitted += chunkSize;

				// Log.d(TAG, "queued " + chunkSize + " bytes of input data.");

			}
			writeOutput();
		}
		return total;
	}

	public void finishWriting() throws IOException
	{
		int index;
		do
		{
			index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);
			if (index >= 0)
				codec.queueInputBuffer(index, 0 /* offset */, 0 /* size */,
						0 /* timeUs */, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			writeOutput();
		}
		while (index < 0);
		Log.d(TAG, "queued input EOS.");

		while (!doneDequeing)
			writeOutput();
	}

	private void writeOutput() throws IOException
	{
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		int index = codec
				.dequeueOutputBuffer(info, kTimeoutUs /* timeoutUs */);

		if (index == MediaCodec.INFO_TRY_AGAIN_LATER)
		{
			;
		}
		else if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
		{
			Log.d(TAG, "dequeued output EOS.");
			doneDequeing = true;
		}
		else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
		{
			format = codec.getOutputFormat();
		}
		else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
		{
			codecOutputBuffers = codec.getOutputBuffers();
		}
		else if (index >= 0)
		{
			int outBitsSize = info.size;
			int outPacketSize = outBitsSize + 7; // 7 is ADTS size
			ByteBuffer outBuf = codecOutputBuffers[index];

			outBuf.position(info.offset);
			outBuf.limit(info.offset + outBitsSize);

			byte[] data = new byte[outPacketSize]; // space for ADTS header
													// included
			addADTStoPacket(data, outPacketSize);
			outBuf.get(data, 7, outBitsSize);
			outBuf.position(info.offset);

			if (firstSkipped)
				outputStream.write(data, 0, outPacketSize); 
			
			firstSkipped = true; // beforehand
			numBytesDequeued += info.size;

			outBuf.clear();
			codec.releaseOutputBuffer(index, false /* render */);

			// Log.d(TAG, "dequeued " + info.size +
			// " bytes of output data.");
		}

	}

	private void addADTStoPacket(byte[] packet, int packetLen)
	{
		int profile = 2; // AAC
							// 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
		int freqIdx = samplingRateKey; 
		int chanCfg = channels; 

		// fill in ADTS data
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF9;
		packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
		packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	@Override
	public void close()
	{
		try
		{
			codec.stop();
		}
		catch (IllegalStateException e)
		{
			e.printStackTrace();
		}
		codec.release();
		codec = null;

		try
		{
			outputStream.flush();
			outputStream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
