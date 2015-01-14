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

/*
 * This File ist derived from:
 * 
 * https://code.google.com/p/nxt-remote-control/source/browse/trunk/src/org/jfedor/nxtremotecontrol/NXTTalker.java
 * 
 * Original License:
 *  
 * Copyright (c) 2010 Jacek Fedorynski
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
 */

/*
 * This file is derived from:
 * 
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package net.proest.legopanoshooter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NXTTalker {

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;      
       
	public static final int MESSAGE_STATE_CHANGE = 2;
    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_READ = 3;
    
    public static final String TOAST = "toast";
    public static final String READ = "read";
    
    public static final byte MOTOR_A = 0x00;
    public static final byte MOTOR_B = 0x01;
    public static final byte MOTOR_C = 0x02;
    public static final byte MOTOR_ALL = (byte)0xff;
    
    public static final byte MOTOR_SPEED_100 = (byte)0x64;
    public static final byte MOTOR_SPEED_n100 = (byte)0x9C;
    public static final byte MOTOR_SPEED_75 = (byte)0x4B;
    public static final byte MOTOR_SPEED_n75 = (byte)0xB5;
    public static final byte MOTOR_SPEED_50 = (byte)0x32;
    public static final byte MOTOR_SPEED_n50 = (byte)0xCE;
    public static final byte MOTOR_SPEED_25 = (byte)0x19;
    public static final byte MOTOR_SPEED_n25 = (byte)0xE7;
    
    public static final byte MOTOR_SPEED_DEFAULT = MOTOR_SPEED_50;
    public static final byte MOTOR_SPEED_nDEFAULT = MOTOR_SPEED_n50;
    
    public static final byte[] RUN_MOTOR   = {0x0C, 0x00, (byte)0x80, 0x04, MOTOR_A, MOTOR_SPEED_DEFAULT, 0x03, 0x00, 0x00, 0x20, (byte)0x68, 0x01, 0x00, 0x00};
    public static final byte[] STOP_MOTOR  = {0x0C, 0x00, (byte)0x80, 0x04, MOTOR_A, MOTOR_SPEED_DEFAULT, 0x07, 0x01, 0x00, 0x00,       0x00, 0x00, 0x00, 0x00};
    public static final byte[] IDLE_MOTOR  = {0x0C, 0x00, (byte)0x80, 0x04, MOTOR_A, MOTOR_SPEED_DEFAULT, 0x00, 0x00, 0x00, 0x00,       0x00, 0x00, 0x00, 0x00};
    
    public static final byte[] RESET_MOTOR = {0x04, 0x00, (byte)0x80, 0x0A, MOTOR_A, 0x01 };
    public static final byte[] READ_MOTOR =  {0x03, 0x00, (byte)0x00, 0x06, MOTOR_A};    
    
    //public static final int CALIBRATE_RUNTIME = 1000;
    
	public static final byte[] SET_COMPASS = {0x05, 0x00, (byte)0x00, 0x05, 0x00, 0x0A, (byte)0x00 };
	public static final byte[] READ_COMPASS = {0x03, 0x00, 0x00, 0x07, 0x00 };
    
	public static final byte[] LS_GET_STATUS =  {0x03, 0x00, (byte)0x00, (byte)0x0E, 0x00};  
	public static final byte[] LS_WRITE_COMPASS_CAL1 = {0x00, 0x0F, 0x00, (byte)0x00, (byte)0x00, 0x3C };
	    
    private int mState;   
    
    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    
    private byte[] mLastMessage = {};
    
    
    public byte[] getLastMessage() {
		return mLastMessage;
	}

	private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    
    public NXTTalker(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null) {
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        } else {
            //XXX
        }
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } else {
            //XXX
        }
    }

    private void sendRead(byte[] text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MESSAGE_READ);
            Bundle bundle = new Bundle();
            bundle.putString(READ, NXTMessageDecoder.decodeReturnMessage(text));
            msg.setData(bundle);
            mHandler.sendMessage(msg);                                  
        } else {
            //XXX
        }
    }
    
    public synchronized void connect(BluetoothDevice device) {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        toast("Connected to " + device.getName());
        
        setState(STATE_CONNECTED);
    }
    
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
        toast("Connection failed");
    }
    
    private void connectionLost() {
        setState(STATE_NONE);
        toast("Connection lost");
    }
    
    public void writeRaw(byte[] data) {
    	Log.i("NXT","Write Raw");    	
    	write(data);
    }      
    
    public void idleMotor(byte motor) {
    	write(setMotorByte(IDLE_MOTOR, motor));
    }
    
    public void runMotor(byte motor, int deg) {
    	runMotor(motor, deg, MOTOR_SPEED_DEFAULT);
    }
    
    public void runMotor(byte motor, int deg, byte speed) {
    	byte[] run_command = RUN_MOTOR;    	
    	run_command = setSpeedByte(run_command, speed);    	    	    
    	run_command = setMotorAndDegreeByte(run_command, motor, deg);
    	
    	Util.sleep(200);    	
    	write(run_command);    	
    	Util.sleep(200);    	    	
    }    
       
    private byte[] setMotorAndDegreeByte(byte[] command, byte motor, int deg) {
    	byte[] retval = command;
    	retval = setMotorByte(retval, motor);
    	retval = setDegreeByte(retval, deg);
    	return retval;
    }
    
    private byte[] setSpeedByte(byte[] command, byte speed) {
    	byte[] retval = command;
    	retval[5] = speed;
    	return retval;
    }
    
    public byte[] setMotorByte(byte[] command, byte motor) {
    	byte[] retval = command;
    	retval[4] = motor;
    	return retval;
    }
    
    private byte[] setDegreeByte(byte[] command, int deg) {
    	byte[] degr = Util.degree2byte(deg);
    	byte[] retval = command;
    	retval[10] = degr[0];
    	retval[11] = degr[1];
    	retval[12] = degr[2];
    	retval[13] = degr[3];
    	
    	return retval;
    }            
    
    private void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }       
        
        r.write(out);
    }    
    
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }
        
        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    // This is a workaround that reportedly helps on some older devices like HTC Desire, where using
                    // the standard createRfcommSocketToServiceRecord() method always causes connect() to fail.
                    Method method = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                    mmSocket = (BluetoothSocket) method.invoke(mmDevice, Integer.valueOf(1));
                    mmSocket.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    connectionFailed();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return;
                }
            }
            
            synchronized (NXTTalker.this) {
                mConnectThread = null;
            }
            
            connected(mmSocket, mmDevice);
        }
        
        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
                
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;            
        }
        
        public void run() {
            byte[] buffer = new byte[1024];
            //int lbytes;
            //lbytes = 0;
            
            while (true) {
            	//Util.sleep(50);
                try {
                    //lbytes = mmInStream.read(buffer);
                    mmInStream.read(buffer);
                    //toast(Integer.toString(bytes) + " bytes read from device");
                    //mReadBuffer.addLast(buffer);  
                    sendRead(buffer);
                    Log.i("DECODE",NXTMessageDecoder.decodeReturnMessage(buffer));
                    mLastMessage = buffer;

                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }
        
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                // XXX
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }       
    }
    

}
