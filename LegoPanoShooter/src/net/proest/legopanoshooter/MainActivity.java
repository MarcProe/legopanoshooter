package net.proest.legopanoshooter;

/*
 * Copyright 2015 Marcus Proest
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *     
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

	private NXTTalker mNXTTalker; // XXX Move talker completely to controller?
	// private NXTController mNXTController;
	private BluetoothAdapter mBluetoothAdapter;
	private String mDeviceAddress = null;
	// private BluetoothDevice mDevice;
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private final float[] mValuesOrientation = new float[3];
	private Location mLocation = null;
	
	//vertical angle
	private static final float ORIENTATION_FLOOR = 40;
	private static final float ORIENTATION_DOWN = 65;
	private static final float ORIENTATION_HORIZ = 90;
	private static final float ORIENTATION_UP = 120;	
	private static final float ORIENTATION_CEIL = 150;	
	
	//horizontal angles to run
	private static final float ANGLE_H = 18;  	//horiz
	private static final float ANGLE_S = 24; 	//up/down
	private static final float ANGLE_U = 30; 	//floor/ceil

	//turns to make a full turn (ANGLE_X * TURN_X = 360)
	private static final int TURN_H = 20;
	private static final int TURN_S = 15;
	private static final int TURN_U = 12;
	

	private TextView mStateDisplay;
	private TextView mSensorDisplayVert;
	private TextView mSensorDisplayHoriz;
	private Button mConnectButton;
	private Button mSnapSinglePicButton;
	private Button mResetButton;
	private Button mStartButton;
	private TimerTask mTimerTask;

	private boolean mAIsRunning = false;
	private boolean mBIsRunning = false;

	private boolean mIsSnapping = false;

	private float mStartAngle;
	private float mCurrentAngleDiff;
	private float mRunningAngle;
	private float mAngleBoundary;
	private float mVertiMode = ORIENTATION_UP;

	private double mPitch;
	private double mRoll;

	private int[] imageViews = { R.id.imageView1, R.id.imageView2,
			R.id.imageView3, R.id.imageView4, R.id.imageView5, R.id.imageView6 };
	private int currentImageView = 0;
	// private int[] imageNums = {0,5,1,6,2,7,3,8,4};

	private Timer mTimer;

	Camera cameraObject;
	ShowCamera mShowCam;

	private boolean NO_BT = false;

	final SensorEventListener mEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		public void onSensorChanged(SensorEvent event) {
			// Handle the events for which we registered
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ORIENTATION:
				System.arraycopy(event.values, 0, mValuesOrientation, 0, 3);
				break;
			}
			mSensorDisplayHoriz.setText(String.valueOf(Math
					.floor(mValuesOrientation[0])));
			mSensorDisplayVert.setText(String.valueOf(Math
					.floor(mValuesOrientation[1])));

			if (mBIsRunning && mValuesOrientation[1] > mVertiMode - 5
					&& mValuesOrientation[1] < mVertiMode + 5) {
				mNXTTalker.writeRaw(mNXTTalker.setMotorByte(
						NXTTalker.STOP_MOTOR, NXTTalker.MOTOR_B));
				mBIsRunning = false;
			}

			mCurrentAngleDiff = Util.diffDegree(mStartAngle,
					mValuesOrientation[0]);
			mRunningAngle = mValuesOrientation[0];
			if (mAIsRunning && mCurrentAngleDiff > mAngleBoundary) {
				mNXTTalker.writeRaw(mNXTTalker.setMotorByte(
						NXTTalker.STOP_MOTOR, NXTTalker.MOTOR_A));
				Util.sleep(300);
				mNXTTalker.idleMotor(NXTTalker.MOTOR_A);
				mAIsRunning = false;
			}

			// EMERGENCY STOP (Should not happen ;-) )
			if (mBIsRunning && mValuesOrientation[1] < -45
					&& mValuesOrientation[1] > -135) {
				mNXTTalker.idleMotor(NXTTalker.MOTOR_ALL);
				mBIsRunning = false;
				mTimerTask.cancel();
			}

			mPitch = mValuesOrientation[1];
			mRoll = mValuesOrientation[2];

		};
	};

	@SuppressWarnings("unused")
	private int mState = NXTTalker.STATE_NONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!NO_BT) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			if (mBluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth is not available",
						Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}

		mNXTTalker = new NXTTalker(mHandler);

		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		setListeners(mSensorManager, mEventListener);

		mLocationManager = (LocationManager) this
				.getSystemService(LOCATION_SERVICE);
		mLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				makeUseOfNewLocation(location);
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		mRunningAngle = 0;

		mStateDisplay = (TextView) findViewById(R.id.state_display);
		mSensorDisplayVert = (TextView) findViewById(R.id.sensor_display_vert);
		mSensorDisplayHoriz = (TextView) findViewById(R.id.sendor_display_horiz);

		mStateDisplay.setText(String.valueOf(mNXTTalker.getState()));

		mConnectButton = (Button) findViewById(R.id.connect_button);
		mConnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connectPress();
				// mStateDisplay.setText("connect pressed");
			}
		});
		mSnapSinglePicButton = (Button) findViewById(R.id.snap_single_pic_button);
		mSnapSinglePicButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					snapIt(v);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// mStateDisplay.setText("connect pressed");

			}
		});
		mResetButton = (Button) findViewById(R.id.reset_button);
		mResetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}

		});
		mStartButton = (Button) findViewById(R.id.start_button);
		mStartButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mStartButton.setEnabled(false);
					startPress();
					Log.i("NXT", "STOP PRESSED ");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		startCam();

		mTimer = new Timer();
	}

	protected void makeUseOfNewLocation(Location location) {
		mLocation = location;
		mStateDisplay.setText(String.valueOf(location.getAccuracy()));
	}

	public void startCam() {
		// ImageView pic = (ImageView)findViewById(R.id.imageView1);
		cameraObject = isCameraAvailiable();
		Parameters params = cameraObject.getParameters();
		params.setPictureSize(4160, 3120);
		params.setRotation(270);
		params.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
		//params.setFocusMode(Parameters.FOCUS_MODE_EDOF);

		params.setJpegQuality(100);
		if (mLocation != null) {
			params.setGpsAltitude(mLocation.getAltitude());
			params.setGpsLatitude(mLocation.getLatitude());
			params.setGpsLongitude(mLocation.getLongitude());
			params.setGpsProcessingMethod(mLocation.getProvider());
			params.setGpsTimestamp(mLocation.getTime());
		}
		cameraObject.setParameters(params);

		/*
		 * List<Camera.Size> sizes = params.getSupportedPictureSizes(); for (int
		 * i=0;i<sizes.size();i++){ Log.i("PictureSize", "Supported Size: "
		 * +sizes.get(i).width + "height : " + sizes.get(i).height); }
		 */
		cameraObject.setDisplayOrientation(270);
		mShowCam = new ShowCamera(this, cameraObject);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mShowCam);
	}

	public static Camera isCameraAvailiable() {
		Camera object = null;
		try {
			object = Camera.open();
		} catch (Exception e) {
		}
		return object;
	}

	private PictureCallback capturedIt = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			Bitmap obitmap = BitmapFactory
					.decodeByteArray(data, 0, data.length);
			if (obitmap == null) {
				Toast.makeText(getApplicationContext(), "not taken",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "taken",
						Toast.LENGTH_SHORT).show();

				Bitmap bitmap = Util.rotate(obitmap, 270);
				Bitmap thumb = Bitmap.createScaledBitmap(bitmap, 128, 128,
						false);
				ImageView pic = (ImageView) findViewById(imageViews[currentImageView % 6]);
				pic.setImageBitmap(thumb);

				File pictureFileDir = Util.getDir();
				if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

					Log.e("NXT", "Can't create directory to save image.");
					Toast.makeText(getApplicationContext(),
							"Can't create directory to save image.",
							Toast.LENGTH_LONG).show();
					return;

				}// end if pic dir
				String vm = String.valueOf(mVertiMode);
				if(vm.length()==2) {
					vm = "0"+vm;
				}

				String photoFile = "LPS_"
						+ vm
						+ "_"
						+ String.valueOf(currentImageView)
						+ "_"
						+ String.valueOf(Math.round(mPitch * 100.0) / 100.0)
						+ "_"
						+ String.valueOf(Math.round(mRunningAngle * 100.0) / 100.0)
						+ "_" 
						+ String.valueOf(System.currentTimeMillis())
						+ ".jpg";

				String filename = pictureFileDir.getPath() + File.separator
						+ photoFile;
				String exiffilename = pictureFileDir.getPath() + File.separator
						+ "e" + photoFile;

				File pictureFile = new File(filename);
				File exifFile = new File(exiffilename);

				try {
					FileOutputStream fos = new FileOutputStream(pictureFile);
					fos.write(data);
					fos.close();

					Log.i("NXT", "New Image saved");

				} catch (Exception error) {
					Log.e("NXT",
							"File" + filename + "not saved: "
									+ error.getMessage());
					Toast.makeText(getApplicationContext(),
							"Image could not be saved.", Toast.LENGTH_SHORT)
							.show();
					Log.e("NXT", "Image could not be saved.");
				}

				try {
					// Exif.changeExifMetadata(pictureFile, exifFile);
					new Exif(pictureFile, exifFile, mRunningAngle, mPitch,
							mRoll, true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			currentImageView++;
			cameraObject.release();
			mShowCam.getHolder().removeCallback(mShowCam);
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.removeAllViews();
			startCam();
			mIsSnapping = false;
		}
	};

	public void snapIt(View view) throws IOException {
		if (!mShowCam.isTakingPicture()) {
			mShowCam.setTakingPicture(true);
			Util.sleep(500);
			cameraObject.takePicture(null, null, capturedIt);
		}
	}

	private void singleHorizontalAction(final float angle, int turn,
			boolean doFirstTurn) {
		if (turn > 0 || doFirstTurn) {
			mAIsRunning = true;
			mStartAngle = mValuesOrientation[0];
			mAngleBoundary = angle;
			mNXTTalker
					.runMotor(NXTTalker.MOTOR_A, 0, NXTTalker.MOTOR_SPEED_n50);

			while (mAIsRunning) {
				Util.sleep(100);
			}
		}
		try {
			mIsSnapping = true;
			snapIt(null);
			while (mIsSnapping) {
				Util.sleep(100);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Util.sleep(1000);
	}

	private void startPress() throws InterruptedException {

		// -90-15 = -115
		// -90+15 = -75
		mTimerTask = new TimerTask() {
			public void run() {

				// //////////////BLOCK ONE
				byte speed = NXTTalker.MOTOR_SPEED_DEFAULT;

				if (mValuesOrientation[1] < 90 && mValuesOrientation[1] >= -90) {
					speed = NXTTalker.MOTOR_SPEED_nDEFAULT;
				} else if (mValuesOrientation[1] >= 90
						&& mValuesOrientation[1] < -90) {
					speed = NXTTalker.MOTOR_SPEED_DEFAULT;
				}

				mVertiMode = ORIENTATION_HORIZ;
				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				Util.sleep(5000);

				// / ROUND WE GO
				for (int i = 0; i < TURN_H; i++) {
					singleHorizontalAction(ANGLE_H, i, true);
				}

				// //////////END BLOCK ONE

				// ///////////BLOCK TWO
				speed = NXTTalker.MOTOR_SPEED_nDEFAULT;
				mVertiMode = ORIENTATION_UP;

				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				// /ROUND WE GO
				for (int i = 0; i < TURN_S; i++) {
					singleHorizontalAction(ANGLE_S, i, false);
				}

				// /////END BLOCK TWO

				// //////////BLOCK THREE

				speed = NXTTalker.MOTOR_SPEED_DEFAULT;
				mVertiMode = ORIENTATION_DOWN;

				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				// /ROUND WE GO

				for (int i = 0; i < TURN_S; i++) {
					singleHorizontalAction(ANGLE_S, i, false);
				}

				// ///////END BLOCK THREE

				// ///BLOCK FOUR
				speed = NXTTalker.MOTOR_SPEED_nDEFAULT;
				mVertiMode = ORIENTATION_CEIL;

				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				// /ROUND WE GO
				for (int i = 0; i < TURN_U; i++) {
					singleHorizontalAction(ANGLE_U, i, false);
				}

				// //////////END BLOCK FOUR

				// ///BLOCK FIVE
				speed = NXTTalker.MOTOR_SPEED_DEFAULT;
				mVertiMode = ORIENTATION_FLOOR;

				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				// /ROUND WE GO
				for (int i = 0; i < TURN_U; i++) {
					singleHorizontalAction(ANGLE_U, i, false);
				}

				// //////////END BLOCK FIVE

				// ////////RETURN TO NICE

				speed = NXTTalker.MOTOR_SPEED_nDEFAULT;
				mVertiMode = ORIENTATION_DOWN;

				mBIsRunning = true;
				mNXTTalker.runMotor(NXTTalker.MOTOR_B, 0, speed);

				while (mBIsRunning) {
					Util.sleep(100);
					// Log.i("NXT","SLEEEEEP");
				}

				mNXTTalker.idleMotor(NXTTalker.MOTOR_B);

				// /////////READY.

				Log.i("NXT", "DONNNNNNE");

				finish();

			};
		};

		mTimer.schedule(mTimerTask, 500);

	}

	private void connectPress() {
		if (mNXTTalker.getState() == NXTTalker.STATE_NONE) {
			mDeviceAddress = "00:16:53:08:BF:C9";
			BluetoothDevice mDevice = mBluetoothAdapter
					.getRemoteDevice(mDeviceAddress);
			mNXTTalker.connect(mDevice);
		} else if (mNXTTalker.getState() == NXTTalker.STATE_CONNECTED) {
			mNXTTalker.stop();
		}
	}

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NXTTalker.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(NXTTalker.TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			case NXTTalker.MESSAGE_STATE_CHANGE:
				mState = msg.arg1;
				// displayState();
				break;
			}
		}
	};

	public void setListeners(SensorManager sensorManager,
			SensorEventListener mEventListener) {
		sensorManager.registerListener(mEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}
}
