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

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressWarnings("deprecation")
public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback {

	private SurfaceHolder holdMe;
	private Camera theCamera;
	
	private boolean takingPicture = true;

	public ShowCamera(Context context,Camera camera) {
		super(context);
		theCamera = camera;
		holdMe = getHolder();
		holdMe.addCallback(this);
		takingPicture = false;
	}

	public void restartPreview(){
		try   {
			theCamera.setPreviewDisplay(holdMe);
			theCamera.startPreview(); 
		} catch (IOException e) {
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try   {
			theCamera.setPreviewDisplay(holder);
			theCamera.startPreview(); 
		} catch (IOException e) {
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
	}

	public boolean isTakingPicture() {
		return takingPicture;
	}

	public void setTakingPicture(boolean takingPicture) {
		this.takingPicture = takingPicture;
	}

}