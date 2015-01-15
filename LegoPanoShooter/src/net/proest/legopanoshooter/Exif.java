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
import java.io.FileWriter;
import java.io.IOException;


import android.util.Log;

import rcs34.android.libs.ExifDriver.ExifDriver;
import rcs34.android.libs.ExifDriver.Values.UndefinedValueAccessException;
import rcs34.android.libs.ExifDriver.Values.ValueByteArray;
import rcs34.android.libs.ExifDriver.Values.ValueNumber;
import rcs34.android.libs.ExifDriver.Values.ValueRationals;

public class Exif {
	
	public static final int TAG_GPS_GPSROLL = 0xd001;	//rational64s
	public static final int TAG_GPS_GPSPITCH = 0xd000;	//rational64s
		
	private ExifDriver mDriver;
	private static final double CROP_FACTOR = 7.3840377697083439399118131300214;
	private static final String logfile = Util.getDir().getPath()
			+ File.separator + "exiflog.txt";

	public Exif(File src, File dst, double direction, double pitch,
			double roll, boolean delsrc) throws 
			IOException {
		this.mDriver = rcs34.android.libs.ExifDriver.ExifDriver.getInstance(src
				.getAbsolutePath());
		this.set35mmFocalLengthEquivalent();
		try {
			this.setGPS(direction, pitch, roll);
		} catch (UndefinedValueAccessException e) {
			throw new IOException();
			//e.printStackTrace();
		}
		this.mDriver.save(dst.getAbsolutePath());

		FileWriter f = new FileWriter(new File(logfile), true);
		f.write(src.getName() + "\t" + String.valueOf(direction) + "\t"
				+ String.valueOf(pitch) + "\t" + String.valueOf(roll) + "\r\n");
		f.close();

		if (delsrc) {
			src.delete();
		}
	}

	private void setGPS(double dirVal, double pitch, double roll)
			throws UndefinedValueAccessException {

		ValueRationals direction = new ValueRationals(
				ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		int dirIVal = (int) Math.floor(dirVal * 100);
		int v1[] = { dirIVal, 100 };
		int v2[][] = { v1 };
		direction.setRationals(v2);
		this.mDriver.getIfdGps().put(ExifDriver.TAG_GPS_SLMG_DIRECTION,
				direction);

		ValueByteArray dirref = new ValueByteArray(
				ExifDriver.FORMAT_ASCII_STRINGS);
		dirref.setBytes("T".getBytes());
		this.mDriver.getIfdGps().put(ExifDriver.TAG_GPS_SLMG_DIRECTION_REF,
				dirref);

		/*ValueRationals vrpitch = new ValueRationals(
				ExifDriver.FORMAT_SIGNED_RATIONAL);
		int ivalPitch = (int) Math.floor(pitch * 100);
		int vp1[] = { ivalPitch, 100 };
		int vp2[][] = { vp1 };
		direction.setRationals(vp2);
		this.mDriver.getIfdGps().put(ExifDriver.TAG_GPS_GPSPITCH, vrpitch);

		ValueRationals vrroll = new ValueRationals(
				ExifDriver.FORMAT_SIGNED_RATIONAL);
		int ivalRoll = (int) Math.floor(roll * 100);
		int vr1[] = { ivalRoll, 100 };
		int vr2[][] = { vr1 };
		direction.setRationals(vr2);
		this.mDriver.getIfdGps().put(ExifDriver.TAG_GPS_GPSROLL, vrroll);*/

	}

	private void set35mmFocalLengthEquivalent() {
		double focalLength;

		ValueRationals r = (ValueRationals) this.mDriver.getIfdExif().get(
				ExifDriver.TAG_FOCAL_LENGTH);

		int[][] ra = r.getRationals();

		focalLength = (double) ra[0][0] / (double) ra[0][1];
		Log.i("EXIF", String.valueOf(ra.length));
		Log.i("EXIF", String.valueOf(ra[0].length));
		Log.i("EXIF", String.valueOf(focalLength));

		int focalLength35mm = (int) Math.round(CROP_FACTOR * focalLength);

		Log.i("EXIF", String.valueOf(focalLength35mm));

		ValueNumber vrFocalLength35mm = new ValueNumber(
				ExifDriver.FORMAT_UNSIGNED_SHORT);

		int[] v1 = { focalLength35mm };

		vrFocalLength35mm.setIntegers(v1);

		this.mDriver.getIfdExif().put(ExifDriver.TAG_FOCAL_LENGTH_35MM_FILM,
				vrFocalLength35mm);

	}

}