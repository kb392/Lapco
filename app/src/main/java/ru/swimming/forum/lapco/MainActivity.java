package ru.swimming.forum.lapco;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ru.swimming.forum.lapco.util.SystemUiHider;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	TextView txtResult;
	Button myButton;
	Button btnStop;
	int iStatus = 0;
	long iStartTime = 0;
	long iStopTime = 0;
	long iSaveTime; //переносится в StopTime или нет
	long iPrevTime;
	long iTimeInMilliseconds;
	long iStartVideoTime; //время от начала записи видеофайля. используется в субтитрах
	private Handler h = new Handler();
	TableLayout tt;
	TableRow tr;
	int iLoopCount = 0;
	private static final String strLogTag = "myLogs";
	// private static final String strLogTagPref = "settings";
	private static final int iCellHPadding = 10;
	private static final int iCellVPadding = 2;
	Spinner spDistance;
	Spinner spSwimStyle;
	int iDistance;
	int iCustomDistance; // произвольная дистанция. переносится в основную при
							// выборе соотв. опции
	int iLapLen;

	long aLoopStop[];
	String strFileName; // Имя файла для записи, задаётся при старте
	String strVideoFileName; //Имя видеофайла, может не совпадать с именем strFileName
	String strVideoAutoFileName; //Автоимя видеофайла, образуется из FileName
	boolean bResulFileSaved = true; // Изначально считаем, что файл записывать
									// не надо
	boolean bCvsSepStr; // флаг добавления строки с обозначением разделителя в
						// cvs файл

	// Video
	public Camera camVideo = null;
	int iCamera = 0;
	public SurfaceView surfaceView;
	public SurfaceHolder surfaceHolder;
	public VideoSurface mVideoSurface;
	public MediaRecorder mediaRecorder = new MediaRecorder();
	boolean bVideoStopWaiting=false;    //программа в состоянии ожидания конца видео.
	long iSubTitPause=2000; //время показа субтитров, милисек
	
	int iColorOverVideo = Color.GREEN;
	int iColorOverVideo2 = Color.RED;
	int iColorTextNormal;
	int iColorButtonNormal;
	//private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	//private SystemUiHider mSystemUiHider;
	String[] aNames=new String[]{"Anya7","Berrimor","Volga13","evgeny"};
	List<String> listNames;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// try {
		// Log.d(null,"onCreate");
		
		//============================================= ========================
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);		
		//=====================================================================
		
		setContentView(R.layout.activity_main);
		
		// go non-full screen 
		//WindowManager.LayoutParams attrs = this.getWindow().getAttributes(); 
		//attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		//this.getWindow().setAttributes(attrs); 
		
		//getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		
		
		myButton = (Button) findViewById(R.id.myButton);
		myButton.setText(R.string.start);
		btnStop = (Button) findViewById(R.id.btnStop);
		if (btnStop == null)
			Log.e(null, "no STOP button");

		// myButton.setEnabled(false);
		txtResult = (TextView) findViewById(R.id.txtResult);
		// registerForContextMenu(txtResult);
		tt = (TableLayout) findViewById(R.id.myTable);
		tr = (TableRow) findViewById(R.id.tableRow1);
		spDistance = (Spinner) findViewById(R.id.spDistance);
		spSwimStyle= (Spinner) findViewById(R.id.spSwimtyle);
		// spPoolLen = (Spinner) findViewById(R.id.spPoolLen);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

		iColorTextNormal = txtResult.getTextColors().getDefaultColor();
		iColorButtonNormal = myButton.getTextColors().getDefaultColor();
		
		AutoCompleteTextView txtName = (AutoCompleteTextView) findViewById(R.id.txtName);
	    //ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line, aNames);
	    //txtName.setAdapter(adapter);
		txtName.setAdapter(new ArrayAdapter<String>(this,
	    		android.R.layout.simple_dropdown_item_1line, aNames));
		
		//для режима fullscreen
		//mSystemUiHider = SystemUiHider.getInstance(this, surfaceView,				HIDER_FLAGS);
		//mSystemUiHider = SystemUiHider.getInstance(this, findViewById(R.id.top_layout),				HIDER_FLAGS);
		//mSystemUiHider.setup();
		
		SettingsButtonAdd();


		ReadState();
		// ArrayAdapter<CharSequence>
		// adapter=ArrayAdapter.createFromResource(this,
		// R.array.distances_array,
		// android.R.layout.simple_spinner_dropdown_item);
		// } catch (Exception e) {
		// Log.e(null, "Activity create error: " + e.getMessage());
		// e.printStackTrace();
		// }
	}

	protected void onDestroy() {
		super.onDestroy();
		WriteState();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mymenu, menu);
		return true;
	}
	
	@SuppressLint("NewApi")
	protected void SettingsButtonAdd() {
		boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();
		if(!hasMenu){
			try {
				getWindow().addFlags(WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null));
          		}
			catch (NoSuchFieldException e) {
				// Ignore since this field won't exist in most versions of Android
        		}
			catch (IllegalAccessException e) {
				Log.w("Optionmenus", "Could not access FLAG_NEEDS_MENU_KEY in addLegacyOverflowButton()", e);
        		}
    		}
		//ImageButton btn = (ImageButton)findViewById(R.id.btnMenu);
		//btn.setImageResource(android.R.drawable.ic_menu_moreoverflow_normal_holo_light);

	}

	
	//нихера не работает
	/* public void StatusBar(int iVisibility) {
		View titleView = getWindow().findViewById(android.R.id.title);
	    if (titleView != null) {
	    	ViewParent parent = titleView.getParent();
	        if (parent != null && (parent instanceof View)) {
	        	View parentView = (View)parent;
	        	iVisibility=(iVisibility>0?View.GONE:View.INVISIBLE);
	         	parentView.setVisibility(iVisibility);
	        	}
	    	}
	} */
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void StatusBar(int iVisibility) {
		try {
			if (iVisibility==0)
				getActionBar().hide();
			else
				getActionBar().show();
		}
		catch(Exception e){}
			
	}

	public static void lockScreenOrientation(Activity activity)
	{   
	    WindowManager windowManager =  (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);   
	    Configuration configuration = activity.getResources().getConfiguration();   
	    int rotation = windowManager.getDefaultDisplay().getRotation(); 

	    // Search for the natural position of the device    
	    if(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&  
	       (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) ||  
	       configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&   
	       (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270))   
	    {   
	        // Natural position is Landscape    
	        switch (rotation)   
	        {   
	            case Surface.ROTATION_0:    
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);    
	                break;      
	            case Surface.ROTATION_90:   
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT); 
	            break;      
	            case Surface.ROTATION_180: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE); 
	                break;          
	            case Surface.ROTATION_270: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
	                break;
	        }
	    }
	    else
	    {
	        // Natural position is Portrait
	        switch (rotation) 
	        {
	            case Surface.ROTATION_0: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
	            break;   
	            case Surface.ROTATION_90: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
	            break;   
	            case Surface.ROTATION_180: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT); 
	                break;          
	            case Surface.ROTATION_270: 
	                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE); 
	                break;
	        }
	    }
	}

	public static void unlockScreenOrientation(Activity activity)
	{
	    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	public String getAppFolder(String strAppDefFolder) {
		
		switch (readIntPref("dstfolder_type")){
		case 0:
			break;
		case 1:
			String strExtFolder=Environment.getExternalStorageDirectory().getAbsolutePath()+"/" +this.getString(R.string.app_folder_name);
			File dir = new File(strExtFolder);
			if(dir.exists()==false) dir.mkdirs();
			return strExtFolder;
		case 2:
			break;
		case 3:
			break;
		}
		
		return strAppDefFolder;
	}
	
	private String getVideoFileName() {
		String strFolder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
		
		strFolder=getAppFolder(strFolder);
		Log.d("folder",strFolder);
		if (iStatus == 1 && strFileName != "")
			return strFolder + "/" + strFileName + ".mp4";
		return strFolder + "/" + GetRFileName() + ".mp4";

	}

	private void ColorVideoOn(int iMode) {
		int iColorText;
		int iColorButton1;
		int iColorButton2;

		switch (iMode) {
		case 0:
			iColorText = iColorOverVideo;
			iColorButton1 = iColorOverVideo;
			iColorButton2 = iColorOverVideo2;
			break;
		case 1:
			iColorText = iColorTextNormal;
			iColorButton1 = iColorButtonNormal;
			iColorButton2 = iColorButtonNormal;
			break;
		default:
			return;

		}

		myButton.setTextColor(iColorButton1);
		txtResult.setTextColor(iColorText);
		btnStop.setTextColor(iColorButton2);

		for (int iRow = 0; iRow < tt.getChildCount(); iRow++) {
			TableRow row = (TableRow) tt.getChildAt(iRow);
			for (int iCol = 0; iCol < row.getChildCount(); iCol++) {
				TextView tv = (TextView) row.getChildAt(iCol);
				tv.setTextColor(iColorText);
			}
		}
	}

	private void fullscreenOn() {
		/*
		if (readBoolPref("fullscreen")) {
			//mSystemUiHider.hide();
			//requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	            WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		*/
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		StatusBar(0);
	}
	
	private void fullscreenOff() {
		/*
		if (readBoolPref("fullscreen")) {
			//mSystemUiHider.show();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);                 
            
		}
		*/
		//((View) findViewById(android.R.id.title).getParent()).setVisibility(View.VISIBLE);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		StatusBar(1);
	}
	
	public void LongClickName(View v){
		
	}
	
	public void onVideoButton(View v){
		try {
			if (checkCameraHardware(this)) {
				if (camVideo == null) {
					VideoStart();
				}
				else {
					VideoStop();
				}
			}
		}
		catch(Exception e){}
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void VideoStart() {
		if (checkCameraHardware(this)) {
			try {
				if (camVideo != null) {
					Log.d(strLogTag, "Camera allready opened");
					return;
				}
				Log.d(strLogTag, "Opening cam " + iCamera);
				camVideo = Camera.open(iCamera);
				int iAngle = VideoSetCameraOrientation();
				Log.d(strLogTag, "Angle " + iAngle);
				
				
				fullscreenOn();
				//requestWindowFeature(Window.FEATURE_NO_TITLE);
				//HideTitle();
		        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		        //    WindowManager.LayoutParams.FLAG_FULLSCREEN);
				
				surfaceView.setVisibility(View.VISIBLE);
				ColorVideoOn(0);

				Camera.Parameters paramVideo = camVideo.getParameters();
				/* */
				if (Build.VERSION.SDK_INT >= 11) {
					//List<Camera.Size>  lstCamSizes = paramVideo.getSupportedVideoSizes();
					List<Camera.Size>  lstCamSizes = paramVideo.getSupportedPreviewSizes();
					
					for (int i=0;i<lstCamSizes.size();i++){
					    Log.i("VideoSize", "Supported Size: " +lstCamSizes.get(i).width + "x" + lstCamSizes.get(i).height);
					}
					Camera.Size sizePreview = lstCamSizes.get(readIntPref("videosize"));
					paramVideo.setPreviewSize(sizePreview.width, sizePreview.height);
				}
				else
				    Log.i("VideoSize", "Old SDK");
				/* */    
				paramVideo
						.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				camVideo.setParameters(paramVideo);

				Log.d(strLogTag, "new VideoSurface");
				mVideoSurface = new VideoSurface(this, camVideo);

				surfaceHolder = surfaceView.getHolder();
				surfaceHolder.addCallback(mVideoSurface);
				surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				
				//--------------------------------------------------------------
				//-- из рецепта по наложению текста
				//--------------------------------------------------------------
				/* */
	    		try {
					camVideo.setPreviewDisplay(surfaceHolder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e("v", "error in setPreviewDisplay " + e.getMessage());
					e.printStackTrace();
				}
	    		camVideo.setPreviewCallback(new Camera.PreviewCallback() {
					
					@Override
					public void onPreviewFrame(byte[] data, Camera camera) {
						// TODO Auto-generated method stub
						//invalidate();
						Log.d("v", "inval");
					}
				 
	    		});
	    		/* */
				//--------------------------------------------------------------
				
				

				// 2. Connect Preview – Подготавливаем объект класса SurfaceView
				// к отображению поля зрения камеры методом
				// Camera.setPreviewDisplay().
				// 3. Start Preview – Вызываем Camera.startPreview(), чтобы
				// начать передавать изображение с камеры на экран
				// 4. Start Recording Video – Следующие шаги должны быть
				// выполнены именно в указанном порядке:
				// 4.1. Unlock the Camera – Выхвать метод Camera.unlock().
				camVideo.unlock();
				
				Log.d(strLogTag, "new MediaRecorder");
				mediaRecorder = new MediaRecorder();
				mediaRecorder.setCamera(camVideo);

				// Log.d(strLogTag, "Preview Display");
				// mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
				// Установить источник аудиоинформации - константу
				// MediaRecorder.AudioSource.CAMCORDER.
				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				// Определить источник видеоинформации - константу
				// MediaRecorder.VideoSource.CAMERA.
				mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				// Установить формат выходного видео и его кодировку. Для
				// Android API Level 8 и выше, используйте метод
				// MediaRecorder.setProfile , получив экземпляр профиля в
				// помощью метода CamcorderProfile.get().
				if (Build.VERSION.SDK_INT >= 8) {
					Log.d(strLogTag, "SDK is new");
					mediaRecorder.setProfile(CamcorderProfile
							.get(CamcorderProfile.QUALITY_HIGH));
					/*
					 * int QUALITY_1080P Quality level corresponding to the
					 * 1080p (1920 x 1080) resolution. int QUALITY_480P Quality
					 * level corresponding to the 480p (720 x 480) resolution.
					 * int QUALITY_720P Quality level corresponding to the 720p
					 * (1280 x 720) resolution. int QUALITY_CIF Quality level
					 * corresponding to the cif (352 x 288) resolution. int
					 * QUALITY_HIGH Quality level corresponding to the highest
					 * available resolution. int QUALITY_LOW Quality level
					 * corresponding to the lowest available resolution. int
					 * QUALITY_QCIF Quality level corresponding to the qcif (176
					 * x 144) resolution. int QUALITY_QVGA Quality level
					 * corresponding to the QVGA (320x240) resolution. int
					 * QUALITY_TIME_LAPSE_1080P Time lapse quality level
					 * corresponding to the 1080p (1920 x 1088) resolution. int
					 * QUALITY_TIME_LAPSE_480P Time lapse quality level
					 * corresponding to the 480p (720 x 480) resolution. int
					 * QUALITY_TIME_LAPSE_720P Time lapse quality level
					 * corresponding to the 720p (1280 x 720) resolution. int
					 * QUALITY_TIME_LAPSE_CIF Time lapse quality level
					 * corresponding to the cif (352 x 288) resolution. int
					 * QUALITY_TIME_LAPSE_HIGH Time lapse quality level
					 * corresponding to the highest available resolution. int
					 * QUALITY_TIME_LAPSE_LOW Time lapse quality level
					 * corresponding to the lowest available resolution. int
					 * QUALITY_TIME_LAPSE_QCIF Time lapse quality level
					 * corresponding to the qcif (176 x 144) resolution. int
					 * QUALITY_TIME_LAPSE_QVGA Time lapse quality level
					 * corresponding to the QVGA (320 x 240) resolution.
					 */

				} else {
					Log.d(strLogTag, "SDK is old");
					// Для Android 2.2 и ниже, придется устанавливать все
					// параметры вручную:
					// 1. setOutputFormat() – Формат видео. Используйте
					// установленный по умолчанию или константу
					// MediaRecorder.OutputFormat.MPEG_4.
					mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
					// 2. setAudioEncoder() – Кодировка аудио.. Используйте
					// установленную по умолчанию или
					// MediaRecorder.AudioEncoder.AMR_NB.
					mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					// 3. setVideoEncoder() – Кодировка видо. Используйте
					// установленную по умолчанию или
					// MediaRecorder.VideoEncoder.MPEG_4_SP.
					mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
				}
				// setOutputFile() – Установите путь в выходному файл с помощью
				// метода getOutputMediaFile(MEDIA_TYPE_VIDEO).toString()
				strVideoFileName = getVideoFileName();
				Log.d(strLogTag, "to " + strVideoFileName);
				mediaRecorder.setOutputFile(strVideoFileName);
				// 6. setPreviewDisplay() – Укажите SurfaceView для рекордера
				mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface()); //после добавления в камеру
				// 3. Prepare MediaRecorder – Подготовить MediaRecorder к записи
				// с помощью MediaRecorder.prepare().
				mediaRecorder.setOrientationHint(iAngle); // наверное, здесь
				mediaRecorder.prepare();
				// 4. Start MediaRecorder – Начать запись с помощью метода
				// MediaRecorder.start().
				Log.d(strLogTag, "Start recording");
				mediaRecorder.start();
				// запрет автоматического поворота экрана
				//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
				lockScreenOrientation(this);
				iStartVideoTime = SystemClock.uptimeMillis();

			} catch (Exception e) {
				Log.i(null, "Can not init cam " + e.getMessage());
			}
		} else
			Toast.makeText(this, "No cam", Toast.LENGTH_LONG).show();
	}

	private void VideoStop() {
		try {
			surfaceView.setVisibility(View.INVISIBLE);
			ColorVideoOn(1);
			if (camVideo != null) {
				mediaRecorder.stop();
				mediaRecorder.release();
				camVideo.stopPreview();
				camVideo.release();
				camVideo = null;
				}
			//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			unlockScreenOrientation(this);
			if (strVideoAutoFileName!="" && strVideoFileName!=strVideoAutoFileName) {
			//	rename();
				File f=new File(strVideoFileName);
				f.renameTo(new File(strVideoAutoFileName));
				strVideoAutoFileName="";
				}
			
		} catch (Exception e) {
			Log.i(null, "Can not stop cam " + e.getMessage());
		}
		bVideoStopWaiting=false;
		fullscreenOff();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case R.id.menu_test_fullscreen_on:
			fullscreenOn();
			break;
		case R.id.menu_test_fullscreen_off:
			fullscreenOff();
			break;
		case R.id.menu_save_csv:
			if (iLoopCount > 0 && aLoopStop.length > 0 && iStatus == 0) {
				ResultSaveExternalTsv(",");
			}
			break;
		case R.id.menu_save_tsv:
			if (iLoopCount > 0 && aLoopStop.length > 0 && iStatus == 0) {
				ResultSaveExternalTsv("\t");
			}
			break;
		case R.id.menu_save_ods:
			if (iLoopCount > 0 && aLoopStop.length > 0 && iStatus == 0) {
				ResultSaveExternalOds();
			}
			break;
		case R.id.menu_save_bb:
			if (iLoopCount > 0 && aLoopStop.length > 0 && iStatus == 0) {
				ResultSaveExternalText("",1);
			}
			break;
		case R.id.menu_save_cleare:
			if (iLoopCount > 0 && aLoopStop.length > 0 && iStatus == 0) {
				ClearTable();
				aLoopStop=null;
				iLoopCount=0;
			}
			break;
		case R.id.menu_video_start:
			Log.d(strLogTag, "Start video menu");
			VideoStart();
			break;
		case R.id.menu_video_stop:
			Log.d(strLogTag, "Stop video menu");
			VideoStop();
			break;
		case R.id.menu_pref:
			Log.d(strLogTag, "Preferences");
			Intent intent = new Intent();
			intent.setClass(this, PrefActivity.class);
			startActivity(intent);
			break;
		case R.id.menu_exit:
			WriteState();
			finish();
			System.exit(0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			StartOrLap();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void WriteTableRow(int iLoopNum, long iTimeDist, long iTimeLoop) {

		Log.d(strLogTag, "New TableRow " + Integer.toString(iLoopNum) + ", "
				+ Long.toString(iTimeDist) + ", " + Long.toString(iTimeLoop));
		TableRow trNew = new TableRow(this);
		trNew.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));

		// Круг
		TextView txtCellLap = new TextView(this);
		txtCellLap.setPadding(iCellHPadding, iCellVPadding, iCellHPadding,
				iCellVPadding);
		txtCellLap.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);

		// txtCellLap.setTextAlignment(TEXT_ALIGNMENT_TEXT_END);
		// txtCellLap.setLayoutParams(
		// new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT ,
		// LayoutParams.WRAP_CONTENT));

		if (camVideo != null && mVideoSurface.getVisibility() == View.VISIBLE)
			txtCellLap.setTextColor(iColorOverVideo);
		trNew.addView(txtCellLap);
		txtCellLap.setText(String.valueOf(iLoopNum));

		// Дистанция
		TextView txtCellDone = new TextView(this);
		txtCellDone.setPadding(iCellHPadding, iCellVPadding, iCellHPadding,
				iCellVPadding);
		txtCellDone.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
		long iDone = iLapLen * iLoopNum;
		if (iDone > iDistance)
			iDone = iDistance;
		txtCellDone.setText(String.valueOf(iDone));
		// txtCellDone.setLayoutParams(new LinearLayout.LayoutParams(0
		// ,LayoutParams.WRAP_CONTENT,10.0f));
		// android:layout_weight
		if (camVideo != null && mVideoSurface.getVisibility() == View.VISIBLE)
			txtCellDone.setTextColor(iColorOverVideo);
		trNew.addView(txtCellDone);

		// Время с начала
		TextView txtCell2 = new TextView(this);
		txtCell2.setPadding(iCellHPadding, iCellVPadding, iCellHPadding,
				iCellVPadding);
		txtCell2.setText(GetFormatedTime(iTimeDist, 1));
		// txtCell2.setLayoutParams(
		// new LinearLayout.LayoutParams(0 ,
		// LayoutParams.WRAP_CONTENT,0.4f));
		if (camVideo != null && mVideoSurface.getVisibility() == View.VISIBLE)
			txtCell2.setTextColor(iColorOverVideo);
		trNew.addView(txtCell2);

		// Время круга
		TextView txtCell3 = new TextView(this);
		txtCell3.setPadding(iCellHPadding, iCellVPadding, iCellHPadding,
				iCellVPadding);
		txtCell3.setText(GetFormatedTime(iTimeLoop, 1));
		// txtCell3.setLayoutParams(
		// new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT ,
		// LayoutParams.WRAP_CONTENT,0.4f));
		if (camVideo != null && mVideoSurface.getVisibility() == View.VISIBLE)
			txtCell3.setTextColor(iColorOverVideo);
		trNew.addView(txtCell3);

		tt.addView(trNew, 0);
		// trNew.bringToFront();
		// tt.bringToFront();

		this.bResulFileSaved = false;
	}

	private void NextLoop() {
		try {

			//защита от случайного нажатия 
			long iMinLap=readIntPref("minlapsec")*1000;
			//Log.d(strLogTag, "now=" + Long.toString(iSaveTime)+" min="+Long.toString(iMinLap)+" prev="+Long.toString(iPrevStopTime)+" lap="+Long.toString((iSaveTime-iPrevStopTime)));
			if ((iSaveTime-iStartTime-iPrevTime)>iMinLap) {
				iStopTime = iSaveTime - iStartTime;
							
				if (iLoopCount < aLoopStop.length) {
					aLoopStop[iLoopCount] = iStopTime;
					Log.d(strLogTag, "mem Time[" + iLoopCount + "]=" + iStopTime);
					iLoopCount++;
	
					WriteTableRow(iLoopCount, iStopTime, iStopTime - iPrevTime);
	
					iPrevTime = iStopTime;
					if (iLapLen * iLoopCount >= iDistance)
						StopTimer();
				} else {
					Log.d(strLogTag, "OverLap: " + iLoopCount);
					iLoopCount++;
					StopTimer();
			}

			}
		} catch (Exception e) {
			Log.e(null, "Next loop error " + e.getMessage());
			e.printStackTrace();
		}

	}
	
	private void ResultSave() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		if(prefs.getBoolean("autosave_ods", false)) ResultSaveExternalOds();
		if(prefs.getBoolean("autosave_bb",  false)) ResultSaveExternalText("",1);
		if(prefs.getBoolean("autosave_html",false)) ResultSaveExternalText("",2);
		if(prefs.getBoolean("autosave_sub", false)) ResultSaveExternalText("",3);
	
	}

	private void StopTimer() {

		Log.d(strLogTag, "Stop Time=" + iStopTime);
		txtResult.setText(GetFormatedTime(iStopTime, 1));
		h.removeCallbacks(updateTimerThread);
		myButton.setText(R.string.start);

		iStatus = 0;
		btnStop.setEnabled(false);
		spDistance.setEnabled(true);
		spSwimStyle.setEnabled(true);
		// spPoolLen.setEnabled(true);
		ResultSaveTsv("");
		ResultSave();
		VideoAutoStop();

	}
	
	private int s2i(String s){
		try{return Integer.parseInt(s);}
		catch (Exception e) {return 0;}
	}
	
	private int readIntPref(String strPrefName) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		try {
			String strVal=prefs.getString(strPrefName, "0");
			Log.d(strLogTag, strPrefName+"=" + strVal);
			return Integer.parseInt(strVal);
		}
		catch (Exception e) {return 0;}
	}

	private boolean readBoolPref(String strPrefName) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		try {
			return prefs.getBoolean(strPrefName, false);
		}
		catch (Exception e) {return false;}
	}

	/*
	 * private int GetPoolLenPosition() { switch (iPoolLen) { case 25: return 0;
	 * case 50: return 1; } return 0; }
	 */
	private int GetDistacePosition() {
		// херовый код, надо преписать на поиск реальных значений spDistance
		switch (iDistance) {
		case 50:
			return 0;
		case 100:
			return 1;
		case 200:
			return 2;
		case 400:
			return 3;
		case 800:
			return 4;
		case 1500:
			return 5;
		default:
			return 6;
		}
	}

	private void WriteState() {
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		Editor prefed = prefs.edit();
		// prefed.putLong(key, value)
		Log.d(strLogTag, "write StartTime=" + iStartTime);
		prefed.putLong("StartTime", iStartTime);
		prefed.putLong("StopTime", iStopTime);
		prefed.putLong("PrevTime", iPrevTime);
		prefed.putInt("LoopCount", iLoopCount);
		Log.d(strLogTag, "write Distance=" + iDistance);
		prefed.putInt("Distance", iDistance);
		// Log.d(strLogTag, "write PoolLen=" + iPoolLen);
		prefed.putInt("LapLen", iLapLen);
		Log.d(strLogTag, "write Status=" + iStatus);
		prefed.putInt("Status", iStatus);
		if (iLoopCount > 0) {
			String sParamName;
			for (int i = 0; i < iLoopCount && i < aLoopStop.length; i++) {
				sParamName = "LoopStopTime" + String.format("%04d", i);
				Log.d(strLogTag, "write " + sParamName + "=" + aLoopStop[i]);
				prefed.putLong(sParamName, aLoopStop[i]);
			}
		}
		prefed.putString("FileName", strFileName);

		prefed.commit();

	}

	private void AllocResultArray() {
		int iSize;
		iSize = iDistance / iLapLen;
		if (iDistance % iLapLen>0) iSize ++;
		if (iSize == 0)
			iSize = 1;
		Log.d(strLogTag, "alloc array " + Integer.toString(iDistance) + "/"
				+ Integer.toString(iLapLen) + "=" + Integer.toString(iSize));
		aLoopStop = new long[iSize];
	}

	private void readDistancePref() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// iLapLen = prefs.getInt("laplen", 25);
		iLapLen = s2i(prefs.getString("laplen", "50"));
		Log.d(strLogTag, "Set LapLen=" + Integer.toString(iLapLen));
		iCustomDistance =s2i(prefs.getString("custdist", "100"));

	}

	private void ReadState() {
		Log.d(strLogTag, "Read settings");
		try {

			SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
			iStatus = prefs.getInt("Status", 0);
			Log.d(strLogTag, "read Status=" + iStatus);

			iStartTime = prefs.getLong("StartTime", 0);
			iStopTime = prefs.getLong("StopTime", 0);
			iPrevTime = prefs.getLong("PrevTime", 0);
			iLoopCount = prefs.getInt("LoopCount", 0);
			iDistance = prefs.getInt("Distance", 1500);
			// if (iDistance ==0) iDistance = custom
			Log.d(strLogTag, "read Distance=" + iDistance);
			// readDistancePref();
			iLapLen = prefs.getInt("LapLen", 25);
			Log.d(strLogTag, "read LapLen=" + iLapLen);
			strFileName = prefs.getString("FileName", "");

			// spPoolLen.setSelection(GetPoolLenPosition());
			spDistance.setSelection(GetDistacePosition());

			if (iStatus == 0) {
				txtResult.setText("00:00:00");
				myButton.setText(R.string.start);
				btnStop.setEnabled(false);
				spDistance.setEnabled(true);
				spSwimStyle.setEnabled(true);
				// spPoolLen.setEnabled(true);
			} else {
				// txtResult.setText(R.string.stop);
				myButton.setText(R.string.stop);
				btnStop.setEnabled(true);
				spDistance.setEnabled(false);
				spSwimStyle.setEnabled(false);
				// spPoolLen.setEnabled(false);
				h.post(updateTimerThread);
			}
			if (iStatus == 1 || iLoopCount > 0) {
				AllocResultArray();
			}
			if (iLoopCount > 0) {
				// getStringSet not available on 2.2
				Log.d(strLogTag, "reading laps");
				String sParamName;
				for (int i = 0; i < iLoopCount && i < aLoopStop.length; i++) {
					sParamName = "LoopStopTime" + String.format("%04d", i);
					aLoopStop[i] = prefs.getLong(sParamName, 0);
					Log.d(strLogTag, "read " + sParamName + "=" + aLoopStop[i]);
					WriteTableRow(i + 1, aLoopStop[i], ((i == 0) ? aLoopStop[i]
							: (aLoopStop[i] - aLoopStop[i - 1])));
				}

			}
		} catch (Exception e) {
			Log.e(null, "Reading settings error: " + e.getLocalizedMessage());
			e.printStackTrace();
		}

	}

	/*
	 * private void StartTimer() { aLoopStop=new long[iDistance/(iPoolLen*2)]; }
	 */
	private boolean getAutoVideo() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return prefs.getBoolean("video", false);

	}

	private void VideoAutoStart() {
		if (getAutoVideo()) {
			VideoStart();
			strVideoAutoFileName =getVideoFileName();
			}
	}

	private void VideoAutoStop() {
		if (getAutoVideo()) {
			int iDelay=0;
			//TODO
			//videostopdelay
			iDelay=readIntPref("videostopdelay");
			if (iDelay>0) {
				bVideoStopWaiting=true;
				Handler hVideoStop=new Handler();
				hVideoStop.postDelayed(new Runnable() {
					public void run() {
						VideoStop();
					}
				}, iDelay*1000);
				
			}
			else
				VideoStop();
		}
	}

	private void StartOrLap() {
		// действия при нажати на кнопку
		if (iStatus == 0) {
			// Старт
			if (bVideoStopWaiting) return;
			Log.d(strLogTag, "Start");
			iStartTime = SystemClock.uptimeMillis();
			iStopTime = 0;
			iLoopCount = 0;
			iPrevTime = 0;
			myButton.setText(R.string.stop);
			h.post(updateTimerThread);
			iStatus = 1;
			spDistance.setEnabled(false);
			spSwimStyle.setEnabled(false);
			btnStop.setEnabled(true);
			
			readDistancePref();
			if(spDistance.getSelectedItem()
					.toString().equals(this.getString(R.string.custom))) {
				Log.d(strLogTag, "custom Distance " + spDistance.getSelectedItem().toString());
				iDistance=iCustomDistance;
			}
			else {
				Log.d(strLogTag, "standart Distance " + spDistance.getSelectedItem().toString());
				iDistance = s2i(spDistance.getSelectedItem().toString());
			}
			Log.d(strLogTag, "Distance=" + Integer.toString(iDistance));
			Log.d(strLogTag, "Clear result cache");
			AllocResultArray();
			ClearTable();
			strFileName = GetRFileName();
			VideoAutoStart();
		} else if (iStatus == 1) {
			// следующий круг
			iSaveTime=SystemClock.uptimeMillis(); //первым делом запоминаем время
			NextLoop();
		}

	}

	public void onButtonClick(View v) {
		// действия при нажати на кнопку
		if (iStatus == 0) {
			StartOrLap();
		} else if (iStatus == 1) {
			// Стоп
			iSaveTime = SystemClock.uptimeMillis();
			NextLoop();
		}

	}

	public void onResultClick(View v) {
		if (iStatus > 0) {
			iSaveTime = SystemClock.uptimeMillis();
			NextLoop();
		}
	}

	public void onStopButtonClick(View v) {
		if (iStatus == 1) {
			// Стоп
			iSaveTime = SystemClock.uptimeMillis();
			NextLoop();
			StopTimer();
		}
	}

	// преобразует время из инта в строку
	// iFormatCode:
	// 0 для вывода в текстовый файл с последующим импортом. с часами
	// 1 для вывода на экран, только минуты
	// 2 для вывода в .ods PT00H00M08.508S
	// 3 для вывода в csv файл с запятыми
	public String GetFormatedTime(long iTime, int iFormatCode) {
		int secs = (int) (iTime / 1000);
		int mins = secs / 60;
		int hours = mins / 60;
		secs = secs % 60;
		int iMS = (int) ((iTime % 1000) / 10);

		String strFormatedTime = "";
		switch (iFormatCode) {
		case 0:
			strFormatedTime = String.format("%02d", hours) + ":"
					+ String.format("%02d", mins) + ":"
					+ String.format("%02d", secs) + ","
					+ String.format("%02d", iMS);
			break;
		case 1:
			strFormatedTime = String.format("%02d", mins + hours * 60) + ":"
					+ String.format("%02d", secs) + ","
					+ String.format("%02d", iMS);
			break;
		case 2: // PT00H00M08.508S
			strFormatedTime = String.format("PT%02dH%02dM%02d.%02d0S", hours,
					mins, secs, iMS);
			break;
		case 3:
			strFormatedTime = String.format("%02d", hours) + ":"
					+ String.format("%02d", mins) + ":"
					+ String.format("%02d", secs) + "."
					+ String.format("%02d", iMS);
			break;

		}
		return strFormatedTime;

	}

	private void ClearTable() {
		tt.removeAllViews();
	}

	private String GetRFileName() {
		String strPrefix;
		DateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");
		Date d = new Date();
		if(spSwimStyle.getSelectedItemId()+1== getResources().getStringArray(R.array.swimming_style).length){ 
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			strPrefix=prefs.getString("custstyle", "swimm");
		}
		else
			strPrefix=spSwimStyle.getSelectedItem().toString();
		return  strPrefix+"-"+ Integer.toString(iDistance) + "-" + df.format(d);
	}

	private String getTsvString(int i, String strFieldSeparator) {
		return getTsvString(i, strFieldSeparator,0);
	}
	
	private String getTsvString(int i, String strFieldSeparator, int iType) {
	switch (iType) {
	case 0:
		return getLapResultString(i, strFieldSeparator, "", "\r\n") ;
	case 1:
		return getLapResultString(i, "[/td][td]", "[tr][td]", "[/td][/tr]") ;
	case 2:
		return getLapResultString(i, "</td><td>", "<tr><td>", "</td></tr>") ;
	case 3:
		return getLapResultSubTitString(i);
	}
	return "";
	}
	
	private String getLapResultString(int i, String strFieldSeparator, String strPrefix, String strSuffix) {
		int iFormatCode = 0;
		if (strFieldSeparator.equals(","))
			iFormatCode = 3;
		try {
			if (i >= 0 && i < aLoopStop.length) {
				long iLen = iLapLen * (i + 1);
				if (iLen > iDistance)
					iLen = iDistance;

				return  strPrefix
						+ Integer.toString(i + 1)
						+ strFieldSeparator
						+ Long.toString(iLen)
						+ strFieldSeparator
						+ GetFormatedTime(aLoopStop[i], iFormatCode)
						+ strFieldSeparator
						+ GetFormatedTime(((i == 0) ? aLoopStop[i]
								: (aLoopStop[i] - aLoopStop[i - 1])),
								iFormatCode) // +
												// "\t"
						// + Long.toString(aLoopStop[i])
						+ strSuffix;
			} else
				return "";
		} catch (Exception e) {
			Log.e(null, "getTsvString error " + e.getMessage());
		}
		return "";

	}

	private String getLapResultSubTitString(int i) {
		int iFormatCode = 0;
		try {
			if (i >= 0 && i < aLoopStop.length) {
				long iLen = iLapLen * (i + 1);
				if (iLen > iDistance)
					iLen = iDistance;

				return  GetFormatedTime(iStartTime - iStartVideoTime+aLoopStop[i] ,3)
						+","+GetFormatedTime(iStartTime - iStartVideoTime+aLoopStop[i]+iSubTitPause ,3)
						+ "\r\n"
						//+Long.toString(iStartTime)+"-"+Long.toString(iStartVideoTime)+"+"+Long.toString(aLoopStop[i])+ "\r\n"
						+  getResources().getString(R.string.lap)+": " +Integer.toString(i + 1)
						+ ". "  + getResources().getString(R.string.distance) +": "+Long.toString(iLen)
						+ ". "  + getResources().getString(R.string.distance_time) +": "+ GetFormatedTime(aLoopStop[i], 1)
						+ ". "  + getResources().getString(R.string.lap_time) +": "+GetFormatedTime(((i == 0) ? aLoopStop[i]
								: (aLoopStop[i] - aLoopStop[i - 1])),
								iFormatCode) 
						+ "\r\n\r\n";
			} else
				return "";
		} catch (Exception e) {
			Log.e(null, "getTsvString error " + e.getMessage());
		}
		return "";

	}
	
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// камера присутствует
			return true;
		} else {
			// камера отсутствует
			return false;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public int VideoSetCameraOrientation() {

		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();

		android.hardware.Camera.getCameraInfo(iCamera, info);

		int rotation = this.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int iRotationAngle;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			iRotationAngle = (info.orientation + degrees) % 360;
			iRotationAngle = (360 - iRotationAngle) % 360; // compensate the
															// mirror
		} else { // back-facing
			iRotationAngle = (info.orientation - degrees + 360) % 360;
		}
		camVideo.setDisplayOrientation(iRotationAngle);
		return iRotationAngle;
		// mediaRecorder.setOrientationHint(iRotationAngle); ещё рано,
		// возвращаем угол и ставим позднее

	}

	private boolean getVideoPref() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Boolean blVideo = prefs.getBoolean("video", false);
		Log.d(strLogTag, "video=" + Boolean.toString(blVideo));
		if (blVideo && !checkCameraHardware(this)) {
			Log.d(strLogTag, "No cam in this device");

			Editor prefed = prefs.edit();
			prefed.putBoolean("video", false);
			prefed.commit();
			return false;
		}
		return blVideo;
	}
	

	private void ResultSaveTsv(String strFieldSeparator) {
		// SharedPreferences prefs=getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		Log.d(strLogTag, "Field separator [" + strFieldSeparator + "]");

		if (strFieldSeparator.equals(""))
			strFieldSeparator = prefs.getString("autosave_type", "");
		if (strFieldSeparator.equals("{tab}"))
			strFieldSeparator = "\t"; // c &#x9; херня какая-то, на пробел
										// заменяет

		//iStartTime = prefs.getLong("StartTime", 0); //это зачем?
		bCvsSepStr = prefs.getBoolean("csvsepstr", false);
		Boolean blAutoSave = prefs.getBoolean("autosavetsv", false);
		Log.d(strLogTag, "AutoSave=" + Boolean.toString(blAutoSave));
		if (blAutoSave)
			bResulFileSaved = ResultSaveExternalTsv(strFieldSeparator);
		else
			bResulFileSaved = ResultSaveLocalTsv("\t");

	}

	private String getFileExt(String strFieldSeparator) {
		/*
		 * switch (strFieldSeparator) { case "\t": return "tsv"; case ",":
		 * return "csv"; }
		 */
		if (strFieldSeparator.equals("\t"))
			return "tsv";
		if (strFieldSeparator.equals(","))
			return "csv";
		if (strFieldSeparator.equals(";"))
			return "csv";
		if (strFieldSeparator.equals("[/td][td]"))
			return "bb";
		if (strFieldSeparator.equals("</td><td>"))
			return "html";
		
		return "txt";

	}
	
	private String getFileExt(int iType, String strFieldSeparator) {
		if (iType==0) return getFileExt(strFieldSeparator);
		return getResources().getStringArray(R.array.file_ext)[iType];
		}
	
	private boolean ResultSaveLocalTsv(String strFieldSeparator) {
		try {
			// catches IOException below

			String strTxtFileName = strFileName + "."
					+ getFileExt(strFieldSeparator);

			Log.d(strLogTag, "opening " + strTxtFileName + " in "
					+ getFilesDir());

			FileOutputStream fOut = openFileOutput(strTxtFileName,
					Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);

			String str = "";
			if (SepStringReqired(strFieldSeparator)) {
				osw.write("sep=;\n");
			}
			for (int i = 0; i < iLoopCount; i++) {
				str = getTsvString(i, strFieldSeparator);
				Log.d(strLogTag, "write to file " + str);
				osw.write(str);
			}

			osw.flush();
			osw.close();
			fOut.close();
			Log.d(strLogTag, "file closed");
			Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
			return true;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			Log.e(strLogTag, "tsv write error", ioe);
			return false;
		}
	}

	private boolean SepStringReqired(String strFieldSeparator) {
		if (strFieldSeparator.equals(";"))
			return true;
		return false;

	}

	//iType
	// 0 tsv
	// 1 bb
	// 2 html
	// 3 sub
	private boolean ResultSaveExternalText(String strFieldSeparator, int iType) {
		try {
			// catches IOException below

			if (!Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				Log.d(strLogTag,
						"SD is not available "
								+ Environment.getExternalStorageState());
				return false;
			}
			

			// File sdPath = Environment.getExternalStorageDirectory();
			String strTxtFileName = strFileName + "."
					+ getFileExt(iType, strFieldSeparator);
			Log.d(strLogTag, "opening " + strTxtFileName + " in "
					+ getExternalFilesDir(null));
			
			File f = new File(getAppFolder(getExternalFilesDir(null).getAbsolutePath()), strTxtFileName);
			//File f = new File(getExternalFilesDir(null), strTxtFileName);
			OutputStream os = new FileOutputStream(f);

			String str;
			if ((iType==0) && SepStringReqired(strFieldSeparator)) {
				os.write(new String("sep=;\r\n").getBytes());
			}
			if (iType==1) {
				os.write(new String("[table]").getBytes());
			} else if(iType==2){
				os.write(new String("<table>").getBytes());
			} else if(iType==3){
				os.write(new String("[SUBTITLE]\r\n").getBytes());
			} 
			
			for (int i = 0; i < iLoopCount; i++) {
				str = getTsvString(i, strFieldSeparator, iType);
				Log.d(strLogTag, "write to file " + str);
				os.write(str.getBytes());
			}
			if (iType==1) {
				os.write(new String("[/table]").getBytes());
			} else if(iType==2){
				os.write(new String("</table>").getBytes());
			}
			os.flush();
			os.close();
			Log.d(strLogTag, "file closed");
			Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
			return true;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			Log.e(strLogTag, "text table write error", ioe);
			return false;
		}
	}

	private boolean ResultSaveExternalTsv(String strFieldSeparator) {
		return ResultSaveExternalText(strFieldSeparator,0);
	}

		
	
	private void ResultSaveExternalOds() {
		OpenWriter openWriter = new OpenWriter(this);
		openWriter.WriteResult();
	}

	public String getFileName() {
		return strFileName;
	}

	private Runnable updateTimerThread = new Runnable() {

		public void run() {

			iTimeInMilliseconds = SystemClock.uptimeMillis() - iStartTime;

			// long iUpdatedTime = timeSwapBuff + iTimeInMilliseconds;
			java.lang.String strFormatedTime = GetFormatedTime(
					iTimeInMilliseconds, 1);

			txtResult.setText(strFormatedTime);
			myButton.setText(strFormatedTime);
			h.postDelayed(this, 10);
		}
	};

}
