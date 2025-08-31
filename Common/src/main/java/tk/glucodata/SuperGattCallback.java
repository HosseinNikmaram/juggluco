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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Build;

import java.util.UUID;

import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
import static android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static java.util.Objects.isNull;
import static tk.glucodata.Applic.DontTalk;
import static tk.glucodata.Applic.getContext;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Applic.mgdLmult;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.Log.showbytes;
import static tk.glucodata.Natives.thresholdchange;
import static tk.glucodata.SensorBluetooth.blueone;

import tk.glucodata.room.GlucoseSaver;

public abstract class SuperGattCallback extends BluetoothGattCallback {
volatile protected    boolean stop=false;
public static boolean doWearInt=false;
public static boolean doGadgetbridge=false;
    private static final String LOG_ID="SuperGattCallback";
    static final private int  use_priority=CONNECTION_PRIORITY_HIGH;
    static  boolean autoconnect=false;
    String mDeviceName=null;

         protected static final UUID mCharacteristicConfigDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    protected static final UUID mCharacteristicUUID_BLELogin = UUID.fromString("0000f001-0000-1000-8000-00805f9b34fb");
    protected static final UUID mCharacteristicUUID_CompositeRawData = UUID.fromString("0000f002-0000-1000-8000-00805f9b34fb");
    public static final UUID LIBRE3_DATA_SERVICE = UUID.fromString("089810cc-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID SIG_SERVICE_DEVICE_INFO = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID LIBRE3_SECURITY_SERVICE = UUID.fromString("0898203a-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_DEBUG_SERVICE = UUID.fromString("08982400-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_BLE_LOGIN = UUID.fromString("0000f001-0000-1000-8000-00805f9b34fb");
    public static final UUID LIBRE3_CHAR_PATCH_CONTROL = UUID.fromString("08981338-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_PATCH_STATUS = UUID.fromString("08981482-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_EVENT_LOG = UUID.fromString("08981bee-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_GLUCOSE_DATA = UUID.fromString("0898177a-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_HISTORIC_DATA = UUID.fromString("0898195a-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_CLINICAL_DATA = UUID.fromString("08981ab8-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_CHAR_FACTORY_DATA = UUID.fromString("08981d24-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_SEC_CHAR_COMMAND_RESPONSE = UUID.fromString("08982198-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_SEC_CHAR_CHALLENGE_DATA = UUID.fromString("089822ce-ef89-11e9-81b4-2a2ae2dbcce4");
    public static final UUID LIBRE3_SEC_CHAR_CERT_DATA = UUID.fromString("089823fa-ef89-11e9-81b4-2a2ae2dbcce4");
    //    private final UUID mCharacteristicUUID_ManufacturerNameString = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
//    private final UUID mCharacteristicUUID_SerialNumberString = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
//    private final UUID mSIGDeviceInfoServiceUUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    final long starttime = System.currentTimeMillis();
   long connectTime=0L;
    String SerialNumber;
    public String mActiveDeviceAddress;
    long dataptr = 0L;
    public BluetoothDevice mActiveBluetoothDevice;
    long foundtime = 0L;
    protected BluetoothGatt mBluetoothGatt;
    boolean superseded=false;
    public final int sensorgen;
    int readrssi=9999;
    protected long sensorstartmsec;

protected    SuperGattCallback(String SerialNumber,long dataptr,int gen) {
    this.SerialNumber = SerialNumber;
    this.dataptr = dataptr;
    mActiveDeviceAddress = Natives.getDeviceAddress(dataptr,true);
    sensorstartmsec=Natives.getSensorStartmsec(dataptr);
    sensorgen=gen;
    {if(doLog) {Log.i(LOG_ID, "new SuperGattCallback " + SerialNumber + " " + ((mActiveDeviceAddress != null) ? mActiveDeviceAddress : "null"));};};
    }
public void disconnect() {
    final var thegatt= mBluetoothGatt;
    if(thegatt!=null) {
        {if(doLog) {Log.i(LOG_ID,"Disconnect");};};
        thegatt.disconnect();
        }
     else  {
        {if(doLog) {Log.i(LOG_ID,"Disconnect mBluetoothGatt==null");};};
      }
    }
public boolean reconnect(long now) {
    final var old=now-showtime+20;
    if(charcha[1]<old&&connectTime<(now-60*1000))  {
        try {
            if(doLog) {Log.i(LOG_ID,"reconnect "+SerialNumber);};
            final var thegatt= mBluetoothGatt;
            if(thegatt!=null) 
                thegatt.disconnect();
            } 
        catch(Throwable th) {
            Log.stack(LOG_ID,"reconnect",th);
            }
        finally {
            return connectDevice(0);
            }
        }
     return true;
    }

void shouldreconnect(long now) {
      final var old=now-showtime+20;
    if(starttime<old&&charcha[0]<old&&connectTime<(now-60*1000))
        reconnect(old);
    }

    long[] constatchange = {0L, 0L};
    int constatstatus = -1;
    String handshake = "";
    long[] wrotepass = {0L, 0L};
    long[] charcha = {0L, 0L};


    static final long thefuture = 0x7FFFFFFFFFFFFFFFL;
    //static  long oldtime = thefuture;

 long showtime = Notify.glucosetimeout;
 static long lastfoundL=0L;
static long lastfound() {
        return lastfoundL;
    }

  private  static long[] nextalarm = new long[10];

    static public void writealarmsuspension(int kind, short wa) {
        short prevsus = Natives.readalarmsuspension(kind);
        Natives.writealarmsuspension(kind, wa);
        int versch = wa - prevsus;
        nextalarm[kind] += versch * 60;
    }


static final int mininterval=55;
static long nexttime=0L; //secs
static notGlucose previousglucose=null;
static float previousglucosevalue=0.0f;

static public void initAlarmTalk() {
    if(!DontTalk) {
        Talker.getvalues();
        if(Talker.shouldtalk())
            newtalker(null);
        }
    }
static Talker talker;
static boolean dotalk=false;
static void newtalker(Context context) {
    if(!DontTalk) {
        if (talker != null)
            talker.destruct();
        talker = new Talker(context);
    }
    }
static void endtalk() {
    if(!DontTalk) {
        dotalk = false;
        if (talker != null) {
            talker.destruct();
            talker = null;
        } 
        }
    }
static private int veryhigh(long tim,notGlucose sglucose,float gl,float rate,int alarm,boolean[] alarmspeak) {
             final boolean alarmtime = tim > nextalarm[6];
             Notify.onenot.veryhighglucose(sglucose,gl, rate,alarmtime);
             if(alarmtime) {
                    nextalarm[8]=nextalarm[6] = nextalarm[1] = tim + Natives.readalarmsuspension(6) * 60;
                    alarm |= 8;
                    if(!DontTalk) {
                        if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
                    }
                   }
              return alarm;
             }
static private int high(long tim,notGlucose    sglucose,float gl,float rate,int alarm,boolean[] alarmspeak) {
        final boolean alarmtime = tim > nextalarm[1];
        Notify.onenot.highglucose(sglucose,gl, rate,alarmtime);
        if (alarmtime) {
            nextalarm[8] = nextalarm[1] = tim + Natives.readalarmsuspension(1) * 60;
            alarm |= 8;
            if(!DontTalk) {
                if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
            }
        }
        return alarm;
       }

static private int verylow(long tim,notGlucose    sglucose,float gl,float rate,int alarm,boolean[] alarmspeak) {
        final boolean alarmtime = tim > nextalarm[5];
        Notify.onenot.verylowglucose(sglucose,gl, rate,alarmtime);
        if(alarmtime) {
            nextalarm[7] =nextalarm[5] =nextalarm[0]= tim + Natives.readalarmsuspension(5) * 60;
            {if(doLog) {Log.i(LOG_ID,"next alarm at "+ nextalarm[5] +" "+bluediag.datestr( nextalarm[5]*1000L ));};};
            alarm |= 8;
            if(!DontTalk) {
                if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
            }
        }
       return alarm;
      }
static private int low(long tim,notGlucose    sglucose,float gl,float rate,int alarm,boolean[] alarmspeak) {
        final boolean alarmtime = tim > nextalarm[0];
        Notify.onenot.lowglucose(sglucose,gl, rate,alarmtime);
        if(alarmtime) {
            nextalarm[7] =nextalarm[0] = tim + Natives.readalarmsuspension(0) * 60;
            {if(doLog) {Log.i(LOG_ID,"next alarm at "+ nextalarm[0] +" "+bluediag.datestr( nextalarm[0]*1000L ));};};
            alarm |= 8;
            if(!DontTalk) {
                if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
            }
        }
       return alarm;
      }
    static void dowithglucose(String SerialNumber, int mgdl, float gl, float rate, int alarm, long timmsec,long sensorstartmsec,long showtime,int sensorgen) {
        if(gl==0.0)
            return;

        GlucoseSaver saver = new GlucoseSaver(getContext());
        saver.saveReading(
                SerialNumber,
                 mgdl,
                gl,
                rate,
                alarm,
                timmsec,
                sensorstartmsec,
                showtime,
                sensorgen
        );
        final long tim = timmsec / 1000L;
        boolean waiting = false;
        var sglucose=new notGlucose(timmsec, String.format(Applic.usedlocale,Notify.pureglucoseformat, gl),  rate,sensorgen);
        previousglucose=sglucose;
        previousglucosevalue=gl;
        final var fview=Floating.floatview;
//        MainActivity.showmessage=null;
        boolean[] alarmspeak={false};
        if(fview!=null) 
            fview.postInvalidate();

        try {

            switch (alarm) {
                case 4: {
                    if(Natives.hasalarmveryhigh())
                        alarm=veryhigh(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                    else {
                        if(Natives.hasalarmhigh())
                            alarm=high(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                         else
                            Notify.onenot.normalglucose(sglucose,gl, rate,false);
                        }
                    break;
                        }
                case 18:  {
                 final boolean alarmtime = tim > nextalarm[8];
                 Notify.onenot.prehighglucose(sglucose,gl, rate,alarmtime);
                 if(alarmtime) {
                        nextalarm[8]= tim + Natives.readalarmsuspension(8) * 60;
                        alarm |= 8;
                        if(!DontTalk) {
                            if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
                        }
                       }
                    break;
                    }
                case 16: 
                      alarm=veryhigh(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                      break;
                case 6: 
                      alarm=high(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                      break;
                case 5: {
                    if(Natives.hasalarmverylow()) {
                       alarm=verylow(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                       }
                    else {
                        if(Natives.hasalarmlow()) {
                           alarm=low(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                           }
                        else
                            Notify.onenot.normalglucose(sglucose,gl, rate,false);
                         }
                     break;
                    }
                case 17: 
                      alarm=verylow(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                      break;
                case 7: 
                      alarm=low(tim,sglucose, gl, rate,alarm,alarmspeak) ;
                      break;
                case 19:  {
                 final boolean alarmtime = tim > nextalarm[7];
                 Notify.onenot.prelowglucose(sglucose,gl, rate,alarmtime);
                 if(alarmtime) {
                        nextalarm[7]= tim + Natives.readalarmsuspension(7) * 60;
                        alarm |= 8;
                        if(!DontTalk) {
                            if((alarmspeak[0]=Natives.speakalarms())) Talker.nexttime = 0L;
                        }
                       }
                    break;
                    }
                case 3:
                    waiting = true;
                default:
                    Notify.onenot.normalglucose(sglucose,gl, rate,waiting);
            }
            ;
        } catch (Throwable e) {
            Log.stack(LOG_ID,SerialNumber, e);
        }
        {if(doLog) {Log.v(LOG_ID, SerialNumber + " "+tim+" glucose=" + gl + " " + rate);};};
        Applic.updatescreen();

        if(!DontTalk) {
            if(dotalk&&!alarmspeak[0])  {
                talker.selspeak(sglucose.value);
                }
            }
        if(isWearable) {
         tk.glucodata.glucosecomplication.GlucoseValue.updateall();
         }


        if(Natives.getJugglucobroadcast())
            JugglucoSend.broadcastglucose(SerialNumber,mgdl,gl,rate,alarm,timmsec);
        if(!isWearable) {
            new Applic().numdata.sendglucose(SerialNumber, tim, gl, thresholdchange(rate), alarm|0x10);
          //  GlucoseWidget.update();
            }
        if(tim>nexttime) {
            nexttime=tim+mininterval;
            if(!isWearable) {
                if(Natives.getlibrelinkused()) XInfuus.sendGlucoseBroadcast(SerialNumber, mgdl, rate, timmsec);
                if(Natives.geteverSensebroadcast()) EverSense.broadcastglucose(mgdl, rate, timmsec);
                //SendNSClient.broadcastglucose(mgdl, rate, timmsec);
                }
            if(Natives.getxbroadcast())
                SendLikexDrip.broadcastglucose(mgdl,rate,timmsec,sensorstartmsec,sensorgen);
            if(!isWearable) {
                if(doWearInt)
                    tk.glucodata.WearInt.sendglucose(mgdl, rate, alarm, timmsec);

                if(doGadgetbridge)
                    Gadgetbridge.sendglucose(sglucose.value,mgdl,gl,rate,timmsec);
                } 
            }

    }
boolean stopHealth=false;
private boolean    dohealth(SuperGattCallback one) {
if(!isWearable) {
        var blue=blueone;
        if(blue==null)
            return true; //false?
       final var gatts=blue.gattcallbacks;
       boolean other=gatts.size()>1;
       if(!other) {
           return true; //TODO stopHealth=false
        }
       if(stopHealth)
           return false;
       for(var el:gatts) {
           if(el!=one)
            el.stopHealth=true;
           }
    return true;
    }
else {
    return false;
    }
    }
protected void handleGlucoseResult(long res,long timmsec) {
        int glumgdl = (int) (res & 0xFFFFFFFFL);
        if(glumgdl != 0) {
            int alarm = (int) ((res >> 48) & 0xFFL);
            {if(doLog) {Log.i(LOG_ID, SerialNumber + " alarm=" + alarm);};};
            float gl = Applic.unit == 1 ? glumgdl / mgdLmult : glumgdl;
            short ratein = (short) ((res >> 32) & 0xFFFFL);
            float rate = ratein / 1000.0f;
            //dowithglucose(SerialNumber, glumgdl, gl, rate, alarm, timmsec,sensorstartmsec,showtime,sensorgen);
            // Headless listener dispatch
            try {
                var lis = tk.glucodata.headless.HeadlessJugglucoManager.glucoseListener;
                if(lis!=null) lis.onGlucose(SerialNumber, glumgdl, gl, rate, alarm, timmsec, sensorstartmsec, sensorgen);
                // Also emit history updates for listeners when new data arrives
            } catch(Throwable ignore) {}
          /*  charcha[0] = timmsec;
            if(!isWearable) {
                if(Natives.gethealthConnect( )) {
                    if(Build.VERSION.SDK_INT >= 28) {
                    if(dohealth(this)) {
                            final long sensorptr = Natives.getsensorptr(dataptr);//TODO: set sensorptr in SuperGattCallback?
                            HealthConnection.Companion.writeAll(sensorptr,SerialNumber);
                            }
                        }
                    }
                }*/
            SensorBluetooth.othersworking(this,timmsec);
        } else {
            {if(doLog) {Log.i(LOG_ID, SerialNumber + " onCharacteristicChanged: Glucose failed");};};
            charcha[1] = timmsec;
        }
    }
public void searchforDeviceAddress() {
    {if(doLog) {Log.i(LOG_ID,SerialNumber+" searchforDeviceAddress()");};};
    //setDeviceAddress(null);
    foundtime=0L;
    mActiveDeviceAddress = null;
    }    
     String getinfo() {
         if(dataptr!=0L)
             return Natives.getsensortext(dataptr);
         return "";
         }
     public long resetdataptr() {
         Natives.freedataptr(dataptr);
         close();
         dataptr = Natives.getdataptr(SerialNumber);
         mActiveDeviceAddress = Natives.getDeviceAddress(dataptr,true);
         return dataptr;
     }


    public void setDevice(BluetoothDevice device) {

        mActiveBluetoothDevice = device;
        if(device!=null) {
            String address=device.getAddress();
            {if(doLog) {Log.i(LOG_ID,SerialNumber+" setDevice("+address+")");};};
            setDeviceAddress(address);
            }
        else  {
            {if(doLog) {Log.i(LOG_ID, SerialNumber +" setDevice(null)");};};
            setDeviceAddress(null);
            }
    }

    public void setDeviceAddress(String address) {
        {if(doLog) {Log.i(LOG_ID, SerialNumber +" "+"setDeviceAddress("+ address+")");};};
        mActiveDeviceAddress = address;
        Natives.setDeviceAddress(dataptr, address);
    }
    void free() {
        stop=true;
        {if(doLog) {Log.i(LOG_ID,"free "+SerialNumber);};};
        close();
        Natives.freedataptr(dataptr);
        dataptr = 0L;
         //sensorbluetooth=null;
    }
    boolean streamingEnabled() {//TODO: libre3?
        return Natives.askstreamingEnabled(dataptr);
        }
    void finishSensor() {
        Natives.finishSensor(dataptr);
        }
    public void close() {
        {if(doLog) {Log.i(LOG_ID,"close "+SerialNumber);};};
        var tmpgatt=mBluetoothGatt ;
        if (tmpgatt != null) {
            try {
                tmpgatt.disconnect();
                tmpgatt.close();
            } catch (Throwable se) {
                var mess = se.getMessage();
                mess = mess == null ? "" : mess;
                String uit = ((Build.VERSION.SDK_INT > 30) ? Applic.getContext().getString(R.string.turn_on_nearby_devices_permission)  : mess) ;
                Applic.Toaster(uit);
                Log.stack(LOG_ID, SerialNumber +" "+ "BluetoothGatt.close()", se);
            }
        finally {    
            mBluetoothGatt = null;
            }
        }
    else {
        {if(doLog) {Log.i(LOG_ID,"mBluetoothGatt==null");};};
        }

    }
    private Runnable getConnectDevice(long delayMillis) {
        var cb = this;
        close();
        if(cb.mActiveDeviceAddress ==null|| cb.mActiveBluetoothDevice == null) {
            {if(doLog) {Log.i(LOG_ID, SerialNumber +" "+"cb.mActiveBluetoothDevice == null");};};
            foundtime = 0L;
            return null;
        }
        return () -> {
            {if(doLog) {Log.i(LOG_ID,"getConnectDevice Runnable "+ SerialNumber);};};
            var device= cb.mActiveBluetoothDevice;
            var sensorbluetooth= blueone;
            if(sensorbluetooth==null) {
                Log.e(LOG_ID, SerialNumber +" "+"sensorbluetooth==null");
                return;
                }
            if(!sensorbluetooth.bluetoothIsEnabled()) {
                Log.e(LOG_ID, SerialNumber +" "+"!sensorbluetooth.bluetoothIsEnabled()");
                return ;
                }
            if(device==null) {
                Log.e(LOG_ID, SerialNumber +" "+"device==null");
                return;
                }
        
            if (cb.mBluetoothGatt != null) {
                {if(doLog) {Log.d(LOG_ID, SerialNumber + " cb.mBluetoothGatt!=null");};};
                return;
                }
            var devname=device.getName();
            if(devname!=null)
                mDeviceName=devname;
                if (doLog) {
                    {if(doLog) {Log.d(LOG_ID, SerialNumber + " Try connection to " + device.getAddress()+ " "+devname+" autoconnect="+autoconnect);};};
                    }
                try {
                    if(isWearable)  {
                        cb.mBluetoothGatt = device.connectGatt(Applic.getContext(), autoconnect, cb, BluetoothDevice.TRANSPORT_LE);
                       cb.setGattOptions(cb.mBluetoothGatt);
                        }
                    else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cb.mBluetoothGatt = device.connectGatt(Applic.getContext(), autoconnect, cb, BluetoothDevice.TRANSPORT_LE);
                        } else {
                            cb.mBluetoothGatt = device.connectGatt(Applic.getContext(), autoconnect, cb);
                            }
                        }

                    setpriority(cb.mBluetoothGatt);
                {if(doLog) {Log.i(LOG_ID,SerialNumber+" after connectGatt");};};
            connectTime= System.currentTimeMillis();
                } catch (SecurityException se) {
                    var mess = se.getMessage();
                    mess = mess == null ? "" : mess;
                    String uit = ((Build.VERSION.SDK_INT > 30) ? Applic.getContext().getString(R.string.turn_on_nearby_devices_permission)  : mess) ;
                    Applic.Toaster(uit);

                    Log.stack(LOG_ID, SerialNumber +" "+ "connectGatt", se);
                } catch (Throwable e) {
                    Log.stack(LOG_ID, SerialNumber +" "+ "connectGatt", e);

                    }
        };
    }

 public boolean connectDevice(long delayMillis) {
    {if(doLog) {Log.i(LOG_ID,"connectDevice("+delayMillis+") "+ SerialNumber);};};
    Runnable connect=getConnectDevice(delayMillis);
    if(connect==null) 
        return false;
    if(delayMillis>0)
        Applic.getHandler().postDelayed(connect, delayMillis);
    else
        Applic.getHandler().post(connect);
    return true;
    }

private boolean used_priority=false;
    @SuppressLint("MissingPermission")
    void setpriority(BluetoothGatt bluegatt) {
        if(bluegatt!=null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(Natives.getpriority()) {
                   bluegatt.requestConnectionPriority(use_priority);
                   {if(doLog) {Log.i(LOG_ID,"requestConnectionPriority HIGH");};};
                   used_priority=true;
               }
                else {
               if(used_priority) {
                  bluegatt.requestConnectionPriority(CONNECTION_PRIORITY_BALANCED);
                  {if(doLog) {Log.i(LOG_ID,"requestConnectionPriority LOW");};};
                  used_priority=false;
                  }
               }
            }
            }
        else {
            Log.e(LOG_ID, SerialNumber +" "+"setpriority BluetoothGatt==null");
            }
    }

    boolean    disablenotification(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
        if(isNull(gatt)) {
            return false;
            }
        if(isNull(ch))
            return false;
        try {
            gatt.setCharacteristicNotification(ch, false);
            BluetoothGattDescriptor descriptor = ch.getDescriptor(mCharacteristicConfigDescriptor);
            if (!descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                Log.e(LOG_ID, SerialNumber + " " + "descriptor.setValue())  failed");
                return false;
            }
            return gatt.writeDescriptor(descriptor);
        }
        catch(Throwable th) {
            Log.stack(LOG_ID,"disablenotification",th);
            return false;
        }
    }

 protected  final boolean enableNotification(BluetoothGatt bluetoothGatt1, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
     return enableGattDescriptor(bluetoothGatt1, bluetoothGattCharacteristic,ENABLE_NOTIFICATION_VALUE);
        }
 protected  final boolean enableIndication(BluetoothGatt bluetoothGatt1, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
     return enableGattDescriptor( bluetoothGatt1,  bluetoothGattCharacteristic,ENABLE_INDICATION_VALUE);
        }
@SuppressLint("MissingPermission")
 protected  final boolean enableGattDescriptor(BluetoothGatt bluetoothGatt1, BluetoothGattCharacteristic bluetoothGattCharacteristic,byte[] type) {
    try {
         BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(mCharacteristicConfigDescriptor);
         if(!descriptor.setValue(type)) {
             Log.e(LOG_ID, SerialNumber +" "+"descriptor.setValue())  failed");
             return false;
             }
         final int originalWriteType = bluetoothGattCharacteristic.getWriteType();
         bluetoothGattCharacteristic.setWriteType( BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
         var success=bluetoothGatt1.writeDescriptor(descriptor);
         bluetoothGattCharacteristic.setWriteType(originalWriteType);
         if(!success) {
             Log.e(LOG_ID, SerialNumber +" "+"bluetoothGatt1.writeDescriptor(descriptor))  failed");
             return success;
             }
          showbytes(LOG_ID+" "+SerialNumber +" "+    "enableNotification ",type);
          if(!bluetoothGatt1.setCharacteristicNotification(bluetoothGattCharacteristic, type[0]!=0)) {
             Log.e(LOG_ID, SerialNumber +" "+"setCharacteristicNotification("+bluetoothGattCharacteristic.getUuid().toString()+",true) failed");
             return false;
             }
         return success;
         }
      catch(Throwable th) {
        Log.stack(LOG_ID,"enableGattDescriptor",th);
        return false;
        }
     }

protected final boolean asknotification(BluetoothGattCharacteristic charac) {
        return enableNotification(mBluetoothGatt, charac);
    }
public boolean matchDeviceName(String deviceName,String address) {
    return false;
    }
public UUID getService()  {
   return null;
   }
public void bonded()  {
   }
public String mygetDeviceName() {
    if(mDeviceName!=null)
       return mDeviceName;
   final var device= mActiveBluetoothDevice;
   if(device!=null) {
      var name=device.getName();
      if(name!=null)
         return name;
      }
    if(mActiveDeviceAddress !=null)
       return mActiveDeviceAddress;
    return "?";
    }

public void setGattOptions(BluetoothGatt gatt) {
        {if(doLog) {Log.i(LOG_ID,"setGattOptions(BluetoothGatt gatt) empty");};};
    }
@Override 
public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
    {if(doLog) {Log.i(LOG_ID,"onPhyUpdate txPhy="+txPhy+" rxPhy="+rxPhy+" status="+status);};};
    }
}
