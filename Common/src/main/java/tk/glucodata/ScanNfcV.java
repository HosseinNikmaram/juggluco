/*      This file is part of Juggluco, an Android app to receive and display         */
/*      glucose values from Freestyle Libre 2 and 3 sensors.                         */
/*                                                                                   */
/*      Copyright (C) 2021 Jaap Korthals Altes <jaapkorthalsaltes@gmail.com>         */
/*                                                                                   */
/*      Juggluco is free software: you can redistribute it and/or modify             */
/*      it under the terms of the GNU General Public License as published            */
/*      by the Free Software Foundation, either version 3 of the License, or         */
/*      (at your option) any later version.                                          */
/*                                                                                   */
/*      Juggluco is distributed in the hope that it will be useful, but              */
/*      WITHOUT ANY WARRANTY; without even the implied warranty of                   */
/*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         */
/*      See the GNU General Public License for more details.                         */
/*                                                                                   */
/*      You should have received a copy of the GNU General Public License            */
/*      along with Juggluco. If not, see <https://www.gnu.org/licenses/>.            */
/*                                                                                   */
/*      Fri Jan 27 15:31:05 CET 2023                                                 */


package tk.glucodata;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.nfc.Tag;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.CalendarContract;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.function.Consumer;

import tk.glucodata.headless.ScanResult;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.View.GONE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Applic.mgdLmult;
import static tk.glucodata.BuildConfig.libreVersion;
import static tk.glucodata.Gen2.getversion;
import static tk.glucodata.Libre3.libre3NFC;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MainActivity.systembarBottom;
import static tk.glucodata.MainActivity.systembarLeft;
import static tk.glucodata.MainActivity.systembarRight;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getlabel;

public class ScanNfcV   {
    private static final String LOG_ID = "ScanNfcV";



static private byte[] newdevice=null;
    @SuppressWarnings("deprecation")
public static void failure(Vibrator vibrator) {
    final long[] vibrationPatternFailure = {0, 500}; // [ms]
        if(android.os.Build.VERSION.SDK_INT < 26)
        vibrator.vibrate(vibrationPatternFailure, -1);
    else
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternFailure, -1));
    }




static            boolean mayEnablestreaming(Tag tag,byte[] uid,byte[] info) {
    if(!Natives.streamingAllowed()) {
        {if(doLog) {Log.d(LOG_ID,"!Natives.streamingAllowed()");};};
        return false;
        }
    if(!AlgNfcV.enableStreaming(tag, info)) {
        {if(doLog) {Log.d(LOG_ID, "Enable streaming failed");};};
        return false;
        } 

    String sensorident = Natives.getserial(uid, info);
    {if(doLog) {Log.d(LOG_ID, "Streaming enabled, resetDevice " + sensorident);};};
    if(SensorBluetooth.resetDevice(sensorident))
        askpermission=true;
    return true;
    }



static     AudioAttributes audioattributes;
static {
        if(android.os.Build.VERSION.SDK_INT >= 21)
         audioattributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM) .build();
    };
static    VibrationAttributes vibrationattributes=android.os.Build.VERSION.SDK_INT <33?null:new VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM) .build();
static void vibrates(Vibrator vibrator,final long[] vibrationPatternstart ,final int[]  amplitude) {
    if( android.os.Build.VERSION.SDK_INT <33) {
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternstart,amplitude, 1),audioattributes);
        }
    else {
        vibrator.vibrate(VibrationEffect.createWaveform(vibrationPatternstart,amplitude, 1),vibrationattributes);
        }
    }
static private   boolean askpermission=false;
    @SuppressWarnings("deprecation")
public static Vibrator getvibrator(Context context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        return ((VibratorManager)context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
    }
    else
        return (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

    }
public static void startvibration(Vibrator vibrator) {
        if(android.os.Build.VERSION.SDK_INT < 26)
            vibrator.vibrate( new long[]  {0, 100, 10,50,50} , 1);
        else {
        final long[] vibrationPatternstart = {0, 70, 50,50,50,50,50};
        final int[] amplitude={0,  255,150,0,255,50,0};
        vibrates(vibrator,vibrationPatternstart,amplitude); 
            }
    }

    static private int[] libre3scan(Vibrator vibrator,Tag tag) {
        int value=0;
        int ret = 0x100000;
        if(android.os.Build.VERSION.SDK_INT >= 26) {
            long streamptr;
            streamptr=libre3NFC(tag);
            vibrator.cancel();
            if(streamptr==2L) {
                {if(doLog) {Log.i(LOG_ID,"streamptr==2");};};
                ret= 0xFD;
            }
            else {
                if(libreVersion == 3) {
                    if(streamptr>=0L&&streamptr<7L) {
                        switch((int)(streamptr&0xFFFFFFF)) {
                            case 1: {
                                {if(doLog) {Log.i(LOG_ID,"streamptr==1");};};
                                ret=0xFB;
                            };break;
                            case 5: {
                                {if(doLog) {Log.i(LOG_ID,"terminated");};};
                                ret=13;
                                break;
                            }
                            case 6: {
                                {if(doLog) {Log.i(LOG_ID,"ended");};};
                                ret=4;
                                break;
                            }
                            default: {
                                if(streamptr>=0L&&streamptr<4L) {
                                    {if(doLog) {Log.i(LOG_ID,"p<streamptr<4");};};
                                    ret=0xFA;
                                }
                            }
                        }
                    }
                    else {
                        var name = Natives.getSensorName(streamptr);
                        if(name==null){
                            {if(doLog) {Log.i(LOG_ID,"name==null");};};
                            ret=0xFA;
                            Natives.freedataptr(streamptr);
                        }
                        else{
                            {if(doLog) {Log.i(LOG_ID,"scanned "+name);};};
                            if(SensorBluetooth.resetDeviceOrFree(streamptr, name))
                                askpermission = true;
                            ret = 0xFC;
                            value=1;
                            askcalendar=true;
                            //curve.render.badscan =calendar(main, ret, name);
                        }
                    }
                }
                else {
                    {if(doLog) {Log.i(LOG_ID,"libreVersion!=3");};};
                    ret = 0xFE;
                }
            }
            if(ret!=0xFC)
                failure(vibrator);
        }
        else  {
            {if(doLog) {Log.i(LOG_ID,"No Libre 3 Android <8");};};
            ret=0xF9;
        }
        return new int[]{ret,value};
    }

    /**
     * Callback interface for NFC scan results
     */
    private static Consumer<ScanResult> scanResultCallback;
    
    /**
     * Set the scan result callback
     * @param callback The callback to receive scan results
     */
    public static void setScanResultCallback(Consumer<ScanResult> callback) {
        scanResultCallback = callback;
    }
    
    /**
     * Remove the scan result callback
     */
    public static void removeScanResultCallback() {
        scanResultCallback = null;
    }

    /**
     * Create a human-readable message based on the scan result
     * @param returnCode The return code from the scan
     * @param glucoseValue The glucose value
     * @param serialNumber The sensor serial number
     * @return Human-readable message
     */
    private static String createScanMessage(int returnCode, int glucoseValue, String serialNumber) {
        int baseCode = returnCode & 0xFF;
        
        switch (baseCode) {
            case 0:
                if (glucoseValue > 0) {
                    return String.format("Glucose reading: %d mg/dL", glucoseValue);
                } else {
                    return "Scan successful, no glucose reading available";
                }
            case 3:
                return "Sensor needs activation";
            case 4:
                return "Sensor has ended";
            case 5:
                return "New sensor detected";
            case 7:
                return "New sensor detected (V2)";
            case 8:
                return "Streaming enabled successfully";
            case 9:
                return "Streaming already enabled";
            case 0x85:
            case 0x87:
                return "Streaming enabled (V2)";
            case 17:
                return "Failed to read tag information";
            case 18:
                return "Failed to read tag data";
            case 19:
                return "Unknown error occurred";
            default:
                return String.format("Unknown result code: %d", baseCode);
        }
    }
    
    /**
     * Convert byte array to hexadecimal string
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    static public synchronized void scan(MainActivity activity,Tag tag) {
        askpermission=false;
        MainActivity main= activity;
        if(!isWearable) {

        }
        var vibrator=getvibrator(main);
        startvibration(vibrator);
        {
            if(!Natives.gethaslibrary()) {
                vibrator.cancel();
                failure(vibrator);
                return;
            }
            int value=0;
            int ret = 0x100000;
            try {
                byte[] uid=tag.getId();
                if(doLog) {
                    String sensid="";
                    for(var e:uid) {
                        sensid=String.format("%02X",(0xFF&e))+sensid;
                    }
                    {if(doLog) {Log.i(LOG_ID,"TAG::sensid="+sensid);};};
                }
/*
        if(uid.length==8&&uid[6]!=7) {
               int[] uit= libre3scan(curve,main,vibrator,tag);
               ret=uit[0];
               value=uit[1];
              }
        else  */
            {
            var isLibre3=uid.length==8&&uid[6]!=7;
            // Try more times for non-Libre3 sensors to reduce transient failures
            byte[] info = AlgNfcV.nfcinfotimes(tag,(isLibre3||doLog)?1:15);
            if(info==null||info.length!=6) {
                    if(isLibre3) {
                           int[] uit= libre3scan(vibrator,tag);
                           ret=uit[0];
                           value=uit[1];
                           
                           // Create ScanResult for Libre3 and notify listener
                           String serialNumber = "Libre3_" + bytesToHex(uid);
                           String message = createScanMessage(ret, value, serialNumber);
                           boolean success = value > 0 || (ret & 0xFF) == 0;
                           ScanResult result = new ScanResult(success, value, ret, serialNumber, message);
                           
                           if(scanResultCallback != null) {
                               scanResultCallback.accept(result);
                           }
                           return;
                          }
                    else {
                        ret=17;
                        {if(doLog) {Log.i(LOG_ID,"Read Tag Info Error");};};
                        vibrator.cancel();
                        
                        // Notify listener of tag info error
                        if(scanResultCallback != null) {
                            scanResultCallback.accept(new ScanResult(false, 0, ret, "", "Failed to read tag info"));
                        }
                        return;
                        }
                    }
                else  {
                    byte[] data;
                    if((data = AlgNfcV.readNfcTag(tag,uid,info)) != null) {
                        {if(doLog) {Log.d(LOG_ID,"Read Tag");};};
                        /*showbytes("uid",uid);
                        showbytes("info",info);
                        showbytes("data",data); */
                        int uit = Natives.nfcdata(uid, info, data);
                        value = uit & 0xFFFF;
                        Log.format("glucose=%.1f\n",(float)value/mgdLmult);
                        ret = uit >> 16;
                        
                        // Get serial number for ScanResult
                        String serialNumber = Natives.getserial(uid, info);
                        if (serialNumber == null) {
                            serialNumber = "";
                        }
                        
                        if(newdevice!=null&& Arrays.equals(newdevice,uid)&& Applic.canusebluetooth() ) {
                            if(value!=0|| (ret&0xFF)==5||(ret&0xFF)==7) {
                                if(SensorBluetooth.resetDevice(serialNumber))
                                    askpermission=true;
                                newdevice=null;
                                }
                            }
                        {if(doLog) {Log.d(LOG_ID,"Badscan "+ret);};};
                        vibrator.cancel();
                        
                        // Create ScanResult and notify listener
                        String message = createScanMessage(ret, value, serialNumber);
                        boolean success = value > 0 || (ret & 0xFF) == 0 || (ret & 0xFF) == 8 || (ret & 0xFF) == 9;
                        ScanResult result = new ScanResult(success, value, ret, serialNumber, message);
                        
                        if(scanResultCallback != null) {
                            scanResultCallback.accept(result);
                        }
                        switch(ret&0xFF) {
                            case 8: {
                                mayEnablestreaming(tag,uid,info);
                                ret=0;
                                break;
                                }
                            case 9: {
                                String sensorident = Natives.getserial(uid, info);
                                {if(doLog) {Log.d(LOG_ID, "Streaming enabled, resetDevice " + sensorident);};};
                                if(SensorBluetooth.resetDevice(sensorident))
                                    askpermission=true;
                                }
                                ret=0;
                                break;
                            case 4: 
                                 SensorBluetooth.sensorEnded(serialNumber); 
                                 break;
                            case 3: {
                                if (value == 0) {
                                
                                    boolean actsuccess = AlgNfcV.activate(tag, info, uid);
                                    if(actsuccess) {
                                        final long[] needsactivationthrill = {20, 10, 40, 5,   2, 15,  35, 7, 12}; // [ms]
                                        if(android.os.Build.VERSION.SDK_INT < 26) {
                                            vibrator.vibrate(needsactivationthrill, -1);
                                            }
                                        else{
                                            final int[] needsactivationthrillamp = {0,  255, 0, 255, 0, 255, 0, 255, 0}; // 
                                            vibrator.vibrate(VibrationEffect.createWaveform(needsactivationthrill,needsactivationthrillamp, -1));
                                            }
                                        newdevice = uid;
                                    } else {
                                        failure(vibrator);
                                    }
                                    main.runOnUiThread(() -> {
                                        main.activateresult(actsuccess);

                                    });

                                    ret=0;
                                }
                                ;
                            } ;break;
                                case 0x85: mayEnablestreaming(tag,uid,info); 
                                            ret&=~0x80;

                                case 5: {
                                    final long[] newsensorwait = {50, 300, 100, 10};
                                    if(android.os.Build.VERSION.SDK_INT < 26) 
                                        vibrator.vibrate(newsensorwait, -1);
                                    else
                                        vibrator.vibrate(VibrationEffect.createWaveform(newsensorwait, -1));
                                };
                                break;
                                case 0x87:  mayEnablestreaming(tag,uid,info); 
                                            ret&=~0x80;
                                case 7:
                                    final long[] newsensorVib =  {50, 150,50,50,12,8,15,73};
                                    if(android.os.Build.VERSION.SDK_INT < 26) 
                                        vibrator.vibrate(newsensorVib, -1);
                                    else
                                        vibrator.vibrate(VibrationEffect.createWaveform(newsensorVib, -1));
            //                        ret=0;
                                    break;
                        };
                        }

                   else  {
                    ret=18;
                    vibrator.cancel();
                    {if(doLog) {Log.i(LOG_ID,"Read Tag Data Error");};};
                    
                    // Notify listener of tag data error
                    if(scanResultCallback != null) {
                        scanResultCallback.accept(new ScanResult(false, 0, ret, "", "Failed to read tag data"));
                    }
                    
                    if(getversion(info)==2&&!Natives.switchgen2()) {
                            Natives.closedynlib();
                            Applic.RunOnUiThread(() -> { getlibrary.openlibrary(main);    });
                           }

                   }
                 }
                 }
         }
        catch( Throwable  error) {
            ret=19;
               vibrator.cancel();
            String mess=error.getMessage();
            if(mess==null)
                mess="unknown error";
                   Log.stack(LOG_ID,mess,error);

            failure(vibrator);

            }
    if(value==0) {
        failure(vibrator);
        }
            }
  if(activity.waitnfc) {
      activity.waitnfc = false;
      activity.setnfc();
        }
    }


static private void newsensor(Activity act,String text,String name) {
    if(!isWearable) {
        XInfuus.sendSensorActivateBroadcast(act, name, Natives.laststarttime());
    }

    var metrics= act.getResources().getDisplayMetrics();
    int width= metrics.widthPixels;
    int pad=width/30;
    {if(doLog) {Log.i(LOG_ID,"newsensor "+name);};};
    act.runOnUiThread(() -> {
       TextView tv=getlabel(act,text);
       if(!isWearable)
               tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,Applic.largefontsize);

        CheckBox calBox = new CheckBox(act);
        calBox.setPadding(0,pad,0,pad);
        calBox.setChecked(true);
        calBox.setText(R.string.addsensorenddate);
        final var endtime=        Natives.sensorends()*1000L;
        final boolean  stillused=endtime>System.currentTimeMillis();
        if(!stillused)
            calBox.setVisibility(GONE);

    });
    }
static boolean askcalendar=true;
static int calendar(Activity act,int ret,String name) {
    if(askcalendar)  {
        int waitmin=(ret&0xff)==5?ret>>8:0;
        String mess=(waitmin>0) ?
(act.getString(R.string.sensor)+" "+name+act.getString(R.string.ready_in)+waitmin+" "+act.getString(R.string.minutes)) :act.getString(R.string.ready_for_use);
        newsensor(act,mess,name);
        return 0;
        }
    else
        return ret;
    }
private static void insertcalendar(Activity act,String name,long endtime) {
/*
    long endtime=Natives.sensorends()*1000L;
    if(endtime<= System.currentTimeMillis())
        return; */

    try {
        Intent intent = new Intent(Intent.ACTION_INSERT)
        .putExtra(CalendarContract.Events.TITLE, act.getString(R.string.enddatesensor)+name)
        .putExtra(CalendarContract.Events.DESCRIPTION, act.getString(R.string.sensor)+" "+name+act.getString(R.string.endstime) )
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, endtime)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endtime+1000L);
        {if(doLog) {Log.i(LOG_ID,"start calendar app");};};
        act.startActivity(intent);
        askcalendar=false;
        } 
    catch(Throwable error) {
            String mess=error.getMessage();
            if(mess==null) mess="Exception";
            Log.stack(LOG_ID,mess,error);
            }
    }
}
