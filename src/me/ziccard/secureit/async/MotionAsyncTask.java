package me.ziccard.secureit.async;

import java.util.ArrayList;
import java.util.List;

import me.ziccard.secureit.codec.ImageCodec;
import me.ziccard.secureit.motiondetection.IMotionDetector;
import me.ziccard.secureit.motiondetection.LuminanceMotionDetector;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

/**
 * Task doing all image processing in backgrounds, 
 * has a collection of listeners to notify in after having processed
 * the image
 * @author marco
 *
 */
public class MotionAsyncTask extends Thread {
	
	// Input data
	
	private List<MotionListener> listeners = new ArrayList<MotionListener>();
	private int[] lastPic;
	private byte[] rawNewPic;
	private int width;
	private int height;
	private Handler handler;
	
	// Output data
	
	private Bitmap lastBitmap;
	private Bitmap newBitmap;
	private boolean hasChanged;
	
	public interface MotionListener {
		public void onProcess(Bitmap oldBitmap,
				Bitmap newBitmap,
				boolean motionDetected);
	}
	
	public void addListener(MotionListener listener) {
		listeners.add(listener);
	}
	
	public MotionAsyncTask(
			int[] lastPic, 
			byte[] rawNewPic, 
			int width, 
			int height,
			Handler updateHanlder) {
		this.lastPic = lastPic;
		this.rawNewPic = rawNewPic;
		this.width = width;
		this.height = height;
		this.handler = updateHanlder;
		
	}

	@Override
	public void run() {
		int[] newPic = ImageCodec.N21toLuma(
				rawNewPic, 
				width, 
				height);
		if (lastPic == null) {
			newBitmap = ImageCodec.lumaToGreyscale(newPic, width, height);
			lastBitmap = newBitmap;
		} else {
			IMotionDetector detector = new LuminanceMotionDetector();
			List<Integer> changedPixels = 
					detector.detectMotion(lastPic, newPic, width, height);
			hasChanged = false;
	
			if (changedPixels != null) {
				hasChanged = true;
				for (int changedPixel : changedPixels) {
					newPic[changedPixel] = Color.RED;
				}
			}
			
			lastBitmap = ImageCodec.lumaToGreyscale(lastPic, width, height);
			newBitmap = ImageCodec.lumaToGreyscale(newPic, width, height);
		}
		
		Log.i("MotionAsyncTask", "Finished processing, sending results");
		handler.post(new Runnable() {
			
			public void run() {
				for (MotionListener listener : listeners) {
					Log.i("MotionAsyncTask", "Updating back view");
					listener.onProcess(
							lastBitmap,
							newBitmap,
							hasChanged);
				}
				
			}
		});
	}
}