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

public class NXTMessageDecoder {

	public static String decodeGetOutputState(byte[] message) {
		return Util.bytesToHex(message,27);
	}
	
	public static long decodeBlockTachoCount(byte [] message) {
		byte[] btc = {message[19], message[20], message[21],message[22]};		
		
		byte a1 = btc[0];
		byte b1 = btc[1];
		byte c1 = btc[2];
		byte d1 = btc[3];
		
		int ia1 = a1 & 0xFF;
		int ib1 = b1 & 0xFF;
		int ic1 = c1 & 0xFF;
		int id1 = d1 & 0xFF;
		
		long value1 = ia1 + ib1 * 256 + ic1 * 256 * 256 + id1 * 256 * 256 * 256;

		return value1;
	}
	
	public static String decodeReturnMessage(byte[] message) {
		String retval = null;
		int meslen = message[0]+message[1]*256+2;
		
		if(message[3] == 0x06) {
			retval = String.valueOf(decodeBlockTachoCount(message));
			//retval = decodeGetOutputState(message);
		//} else if(message[3] == 0x07) {
		//	retval = Util.bytesToHex(message,24);
		} else {
			retval = Util.FormatBytes(Util.bytesToHex(message,meslen), meslen);			
		}
		//retval = retval.replaceAll("(.{2})(?!$)", "$1-");
		return retval;
	}
}
