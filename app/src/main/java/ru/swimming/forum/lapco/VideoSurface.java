package ru.swimming.forum.lapco;

import java.io.IOException;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class VideoSurface extends SurfaceView  implements SurfaceHolder.Callback{
	Context mainActivity;
	Camera camVideo; 
	private String sLogTag="VideoSurface";
	
	public VideoSurface(Context context, Camera mainCamera) {
		super(context);
		// TODO Auto-generated constructor stub
		mainActivity=context;
		camVideo=mainCamera;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.d(sLogTag,"surfaceChanged");

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d(sLogTag,"surfaceCreated");
        if (camVideo != null){
            //Parameters params = camVideo.getParameters();
            //camVideo.setParameters(params);
        	//try {
        	//	
        	//}

			//--------------------------------------------------------------
        }
        else {
            Toast.makeText(mainActivity, "Camera not available!", Toast.LENGTH_LONG).show();
            //finish();
        }

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d(sLogTag,"surfaceDestroyed");

	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
	}
	

}
