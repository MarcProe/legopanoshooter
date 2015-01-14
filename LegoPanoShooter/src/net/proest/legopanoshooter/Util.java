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

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

public class Util {
	final static String FormatBytes(String bytes, int len) {
		return bytes.substring(0,len*3-1);
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int n) {
		if(bytes.length > 0) {
			//Log.i("NXT2",String.valueOf(bytes.length));

			char[] hexChars = new char[bytes.length * 2];
			for ( int j = 0; j < n; j++ ) {
				//Log.i("NXT1",String.valueOf(j));
				int v = bytes[j] & 0xFF;
				hexChars[j * 2] = hexArray[v >>> 4];
				hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			}
			return new String(hexChars);
		} else return "empty message!";
	}

	public static String bytesToHex(byte[] bytes) {
		return bytesToHex(bytes, bytes.length);
	}
	
    public static void sleep(long ms) {
    	try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}   
    }
    
    public static byte[] degree2byte(int deg){
    	byte[] ret = new byte[4];
    	
    	ret[0] = (byte)(Math.floor((double)deg) 			% 256);		// 256^0 ;-)
    	ret[1] = (byte)(Math.floor((double)deg / 256) 		% 256);		// 256 ^1
    	ret[2] = (byte)(Math.floor((double)deg / 65536) 	% 256);		// 256^2
    	ret[3] = (byte)(Math.floor((double)deg / 16777216) 	% 256); 	// 256^3    	    	
    	    	
		return ret;
    	
    }
    
    public static File getDir() {
		File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		Log.i("NXT",sdDir.getAbsolutePath());
		return new File(sdDir, "LegoPanoShooter");
	}// end get dir
    
    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }
    
    public static float diffDegree(float a, float b) {
    	float d = Math.abs(a - b) % 360;
    	float r = d > 180 ? 360 - d : d;
    	return r;
    }
}


