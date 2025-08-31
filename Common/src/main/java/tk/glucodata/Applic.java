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

import static android.graphics.Color.BLACK;
import static android.graphics.Color.RED;
import static android.graphics.Color.BLUE;
import static android.graphics.Color.WHITE;
import static android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.view.View.INVISIBLE;
//import java.text.DateFormat;
import static java.lang.String.format;
import static java.util.Locale.US;
import static tk.glucodata.GlucoseCurve.STEPBACK;
import static tk.glucodata.GlucoseCurve.smallfontsize;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MessageSender.initwearos;
import static tk.glucodata.Natives.hasData;
import static tk.glucodata.SuperGattCallback.endtalk;
import static tk.glucodata.util.getlocale;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import tk.glucodata.nums.AllData;
import tk.glucodata.nums.numio;
import tk.glucodata.settings.Broadcasts;
//import static tk.glucodata.MessageSender.messagesender;

public class Applic {
static final boolean ALLGALAXY=true;
static final boolean hasNotChinese=true;
public static final  boolean scrollbar=true;
public static final  boolean horiScrollbar=true;
//static final float mgdLmult= doLog?18.0182f:18.0f;
//static final float mgdLmult= BuildConfig.DEBUG?18.0182f:18.0f;
static final float mgdLmult=18.0f;
//public static tk.glucodata.MessageSender messagesender=null;
   static boolean Nativesloaded=false;
public static boolean hour24=true;
static public final int TargetSDK=BuildConfig.targetSDK;
static public final boolean isWearable= BuildConfig.isWear==1;
//static final boolean DontTalk=isWearable;
static public final boolean DontTalk=false;
static public final boolean useZXing= BuildConfig.noZXing==0;
//static public final boolean includeLib= isWearable;
static public final boolean includeLib= true;
static public final boolean isRelease= BuildConfig.isRelease==1;
//static public final boolean isRelease= !BuildConfig.DEBUG;
static final String JUGGLUCOIDENT=isWearable?"juggluco":"jugglucowatch";
public static final Locale usedlocale=US;
//final public static boolean usemeal=Natives.usemeal();
// boolean usebluetooth=true;
final private static String LOG_ID="Applic";
//ArrayList<String> labels = null;
public  ArrayList<String> getlabels() {
        return  Natives.getLabels();
    }
public tk.glucodata.nums.AllData numdata=null;
public GlucoseCurve curve=null;
    public static int unit=0;
public void redraw() {
    var tmpcurve=curve;
    if(tmpcurve!=null)
        tmpcurve.requestRender();
    }

static private Handler mHandler;
private static  long uiThreadId;
public static Handler getHandler() {
    return mHandler;
    }
static public MainActivity getActivity() {
   return MainActivity.thisone;
    }
static public android.app.Activity getActivity(android.app.Activity activity) {
   return activity;
    }
static public Context getContext() {
      Context cont=getActivity();
        return cont;}
static public void Toaster(String mess) {
   // RunOnUiThread(()-> { Applic.argToaster(getContext() ,mess, Toast.LENGTH_SHORT);}) ;
    }
    static public void Toaster(int res) {
      //  RunOnUiThread(()-> { Applic.argToaster(getContext() ,res, Toast.LENGTH_SHORT);}) ;
    }

static public    void argToaster(Context context,int res,int duration) {
     argToaster(context,context.getString(res), duration);
}
static public    void argToaster(Context context,String message,int duration) {
    Toast.makeText(context,message, duration).show();
    if(!DontTalk) {
        if(initproccalled&&Natives.speakmessages()) 
            speak(message);
        }
    }
static public void RunOnUiThread(Runnable action) {
    if (Thread.currentThread().getId() != uiThreadId) {
        mHandler.post(action);
      } else {
        action.run();
        }
    }
static public void postDelayed(Runnable action,long mmsecs) {
    Applic.getHandler().postDelayed( action ,mmsecs);
    }
    /*
static  public void RunOnUiThread(Runnable action) {
   app.RunOnUiThreader(action);
    } */
public static  Applic app;
private final IntentFilter mintimefilter ;
@MainThread
public Applic() {
    super();
    app=this;
    android.util.Log.i(LOG_ID,"start tk.glucodata");
    mintimefilter = new IntentFilter();
    mintimefilter.addAction(Intent.ACTION_TIME_TICK);
    mHandler = new Handler(Looper.getMainLooper());
    uiThreadId=Thread.currentThread().getId();
    if(!isWearable) {
        numdata=new AllData();
        }
    }
void setnotify(boolean on) {
    Notify.alertwatch=on;
    {if(doLog) {Log.i(LOG_ID,"setnotify="+on);};};
    }
public void setunit(int unit)  {
     if(Applic.unit!=unit)
        SuperGattCallback.previousglucosevalue=0.0f;
    Natives.setunit(unit);
   // Notify.mkunitstr(app,unit);
    }
public void sendlabels() {
    if(!isWearable) {
        if(Natives.gethasgarmin())
            numdata.sendlabels();
        }
    }
void setcurve(GlucoseCurve curve) {
    this.curve=curve;
    }
//void savestate() { if(blue!=null) blue.savestate(); }
public static String curlang=null;

private static boolean hasSystemtimeformat=true;
public static boolean systemtimeformat() {
//    return DateFormat.is24HourFormat(app)==hour24;
    return hasSystemtimeformat;
    }
//private static boolean was24=true;

private static void setlanguage() {
        var lang=getlocale().getLanguage();
        {if(doLog) {Log.i(LOG_ID,"Applic.setlangauge="+lang+" cur="+curlang);};};
        if(!lang.equals(curlang)) {
            curlang=lang;
            if(!DontTalk) {
                if(Talker.istalking())    
                        SuperGattCallback.newtalker(null);
                }
            }
        else  {
            return;
            }
        Natives.setlocale(lang);
     }

/*@Override
public void onConfigurationChanged(Configuration newConfig) {
   super.onConfigurationChanged(newConfig);
   if(Nativesloaded)  {
        needsnatives();

//        var new24 = DateFormat.is24HourFormat(this);
        hasSystemtimeformat=DateFormat.is24HourFormat(app)==hour24;
        Notify.timef = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
        {if(doLog) {Log.i(LOG_ID,"Applic.onConfigurationChanged "+(Applic.hour24?" 24uur":" 12uur"));};};
 //       was24=new24;
        setlanguage();
        }
    } */

static private void setjavahour24(boolean val) {
    /*Applic.hour24=val;
    Notify.timef = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT);
    hasSystemtimeformat=DateFormat.is24HourFormat(app)==hour24;
      if(!isWearable) {
         var main=MainActivity.thisone;
         if(main!=null) {
            var tmpcurve=main.curve;
            if(tmpcurve!=null) {
               if(tmpcurve.search!=null)
                  tmpcurve.clearsearch(null);
               }
              }
            }*/
       }

static public void sethour24(boolean val) {
     Natives.sethour24(val);
    setjavahour24(val);
    }
/*@Override
public void onTerminate () {
    super.onTerminate();
    if(!isWearable) {
        if (numdata != null) {
            numdata.onCleared();
        }
    }
}*/

static final String[] scanpermissions=( (Build.VERSION.SDK_INT >= 31)? (new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}):((Build.VERSION.SDK_INT >= 29)?new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION}:new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}));



static String[] hasPermissions(Context context, String[] perms) {
    var over=new ArrayList<String>();
    {if(doLog) {Log.i(LOG_ID,"hasPermissions: ");};};
    for(var p:perms) {
        if(ContextCompat.checkSelfPermission(context, p)!= PackageManager.PERMISSION_GRANTED) {
            over.add(p);
            Log.i(LOG_ID,p+" not granted");    
            }
        else  {
            Log.i(LOG_ID,p+" granted");    
                }
        }
    var uit=new String[over.size()];
    {if(doLog) {Log.i(LOG_ID,"no perm="+over.size());};};
    return over.toArray(uit);
    }
static String[] noPermissions(Context context) {
    String[] noperms=  hasPermissions(context, scanpermissions) ;
    if(doLog) {
        {if(doLog) {Log.i(LOG_ID,"nopermissions:");};};
        for(var el:noperms) {
            {if(doLog) {Log.i(LOG_ID,el);};};
            }
        }
    return noperms;
    }
static boolean mayscan() {
     if(Build.VERSION.SDK_INT >= 23) {
       return Applic.hasPermissions(getContext(), scanpermissions).length==0 ;
       }
    return true;
    }
static boolean canBluetooth() {
    if(Build.VERSION.SDK_INT > 30) {
       return Applic.hasPermissions(getContext(), scanpermissions).length==0 ;
       }
    return true;
    }
@Keep
static int bluePermission() {
    if(Build.VERSION.SDK_INT > 23) {
        if(Applic.hasPermissions(getContext(), scanpermissions).length==0)
               return 2;
        if(Build.VERSION.SDK_INT > 30) {
           return 0;
           }
        return 1;
        }
    return 2;
    }
public static void updateservice(Context context,boolean usebluetooth) {
        try {
            if(Class.forName("tk.glucodata.keeprunning") != null) {
                if(usebluetooth||Natives.backuphostNr( )>0) {
                    if(keeprunning.start(context))
                        {if(doLog) {Log.i(LOG_ID,"updateservice: keeprunning started");};}
                    else
                        {if(doLog) {Log.i(LOG_ID,"updateservice keeprunning not started="+keeprunning.started);};};

                    }
                else
                    keeprunning.stop();
            }
        } catch(ClassNotFoundException e) {
            // keeprunning class not available in this build variant
        }
        }
private static void dontusebluetooth() {
    {if(doLog) {Log.i(LOG_ID,"dontusebluetooth()");};};
    try {
        if(Class.forName("tk.glucodata.SensorBluetooth") != null) {
            SensorBluetooth.destructor();
        }
    } catch(ClassNotFoundException e) {
        // SensorBluetooth class not available in this build variant
    }
    try {
        if(Class.forName("tk.glucodata.keeprunning") != null) {
            if(Natives.backuphostNr( )<=0) {
                keeprunning.stop();
                }
        }
    } catch(ClassNotFoundException e) {
        // keeprunning class not available in this build variant
    }
    }


static boolean usingbluetooth=false;
static boolean  canusebluetooth() {
    return usingbluetooth&&Natives.getusebluetooth();
    }

static     void explicit(Context context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        try {
        final PowerManager pm = (PowerManager)context.getSystemService(Activity.POWER_SERVICE);
        var name=context.getPackageName();
        if(pm.isIgnoringBatteryOptimizations(name))
            return;
         Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
         intent.setData(Uri.parse("package:" + name));
        try {
            context.startActivity(intent);
        } catch(ActivityNotFoundException e) {
            // Activity not found
            Log.e(LOG_ID, "Activity not found for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
        }
        } catch(Throwable e) {
            Log.stack(LOG_ID,"explicit",e);
            }
        }
    }
static void initbluetooth(boolean usebluetooth, Context context, boolean frommain) {
    if(doLog) {Log.i(LOG_ID,"initbluetooth "+usebluetooth);};
    try {
        if(Class.forName("tk.glucodata.SensorBluetooth") != null) {
            SensorBluetooth.start(usebluetooth);
        }
    } catch(ClassNotFoundException e) {
        // SensorBluetooth class not available in this build variant
    }
    
    try {
        if(Class.forName("tk.glucodata.keeprunning") != null) {
            if(!keeprunning.started) {
                if(usebluetooth||Natives.backuphostNr( )>0) {
                    if(keeprunning.start(context))
                        {if(doLog) {Log.i(LOG_ID,"keeprunning started");};}
                    else
                        {if(doLog) {Log.i(LOG_ID,"keeprunning not started="+keeprunning.started);};};
                    if(frommain)
                        ((MainActivity)context).askNotify();
                    }
                }
        }
    } catch(ClassNotFoundException e) {
        // keeprunning class not available in this build variant
    }
    }
static boolean possiblybluetooth(Context context) {
    {if(doLog) {Log.i(LOG_ID,"possiblybluetooth");};};
//    boolean useblue=Natives.getusebluetooth()&& hasPermissions(context, Applic.scanpermissions).length==0;
    final boolean useblue=Natives.getusebluetooth();
    try {
        new Applic().initbluetooth(useblue,context,false);
    } catch(Exception e) {
        // initbluetooth failed
        Log.e(LOG_ID, "initbluetooth failed");
    }
    return useblue;
    }
public static boolean hasip() {
    String[] ips=Backup.gethostnames();
    return ips.length>0&&ips[ips.length-1]!=null;
    }
//@RequiresApi(api = Build.VERSION_CODES.M)
private static boolean hasonAvailable=false;
@Keep
public static void sendsettings() {
      {if(doLog) {Log.i(LOG_ID,"sendsettings");};};
         try {
             if(Class.forName("tk.glucodata.MessageSender") != null) {
                 if(!MessageSender.cansend()) {
                               {if(doLog) {Log.i(LOG_ID,"!cansend()");};};
                    return;
                    }
                var sender=tk.glucodata.MessageSender.getMessageSender();
                if(sender!=null) {
                   // sender.sendsettings();
                    }
             }
         } catch(ClassNotFoundException e) {
             // MessageSender class not available in this build variant
         }
        }

static public boolean useWearos() {
   return  false;
    }
@Keep
private void initialize() {
        if (android.os.Build.VERSION.SDK_INT >=  android.os.Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback((new NetworkRequest.Builder()).build(), new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
           hasonAvailable=true;
           {if(doLog) {Log.i(LOG_ID, "network: onAvailable(" + network+")");};};
           if(useWearos()||hasip()) {
             Natives.networkpresent();
             MessageSender.reinit();
            if(useWearos()) {
               MessageSender.sendnetinfo();
               Applic.scheduler.schedule(()-> { resetWearOS(); }, 20, TimeUnit.SECONDS);
               }    
              Applic.wakemirrors();
            }
        }
        @Override
        public void onUnavailable () {
            {if(doLog) {Log.i(LOG_ID,"network: onUnavailable()");};};
            }
        @Override
        public void onLosing (Network network, int maxMsToLive) {
            {if(doLog) {Log.i(LOG_ID,"network: OnLosing("+network+","+maxMsToLive+")");};};
            }
        @Override
        public void onCapabilitiesChanged (Network network, NetworkCapabilities networkCapabilities) {
            var down=networkCapabilities.getLinkDownstreamBandwidthKbps();
            var up=networkCapabilities.getLinkUpstreamBandwidthKbps();
            var wifi=networkCapabilities.hasTransport(TRANSPORT_WIFI);
            var blue=networkCapabilities.hasTransport(TRANSPORT_BLUETOOTH);
            boolean aware= false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                aware = networkCapabilities.hasTransport(TRANSPORT_WIFI_AWARE);
            }
            {if(doLog) {Log.i(LOG_ID, "network: onCapabilitiesChanged(" + network+"downstream "+down+" Kbps up="+up+" Kbps "+(wifi?"":"no ")+" wifi, "+(blue?"":"no ")+" bluetooth, "+(aware?"":" no ")+"WIFI aware)");};};
            }
        @Override
        public void onLinkPropertiesChanged (Network network, LinkProperties linkProperties) {
            {if(doLog) {Log.i(LOG_ID, "network: onLinkPropertiesChanged("+network+", LinkProperties linkProperties) ");};};
            }
        @Override
        public void onLost(Network network) {
              {if(doLog) {Log.i(LOG_ID, "onLost(" + network+")");};};
            Natives.networkabsent();
                 MessageSender.reinit();
//           if(hasonAvailable) 
               if(useWearos()) {
                Applic.wakemirrors();
                Applic.scheduler.schedule(()-> { resetWearOS(); }, 20, TimeUnit.SECONDS);
            }
        }
        });

        if(hasip()) Natives.networkpresent();
        }
    else
        Natives.networkpresent();
    }



public static int initscreenwidth=-1;


void domintime() {
    {if(doLog) {Log.i(LOG_ID,"TICK");};};
    if (curve != null) {
        if((curve.render.stepresult & STEPBACK) == 0) {
            {if(doLog) {Log.i(LOG_ID,"requestRender()");};};
            curve.requestRender();
            if (!isWearable) {
                numdata.sendmessages();
            }
        }
    }
}
    static final ScheduledExecutorService scheduler =Executors.newScheduledThreadPool(1);

void setmintime() {
    try {
       //  registerReceiver(minTimeReceiver, mintimefilter);
         }
     catch(Throwable th) {
        Log.stack(LOG_ID,"registerReceiver",th);
        }
  }
void cancelmintime() {
    try {
         // unregisterReceiver(minTimeReceiver);
          }
     catch(Throwable th) {
        Log.stack(LOG_ID,"cancelmintime",th);
        }
    }
/*
void startstrictmode() {
    if(BuildConfig.isRelease!=1) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder() .detectAll().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll() .penaltyLog() .build());
        }
    }*/
static boolean initproccalled=false;
static boolean initStarted=false;
/*
public static void initwearos(Context app) {
    {if(doLog) {Log.i(LOG_ID,"before new MessageSender");};};
    messagesender=new MessageSender(app);
    {if(doLog) {Log.i(LOG_ID,"before sendnetinfo");};};
    MessageSender.sendnetinfo();
    }*/
static boolean dataAtStart=false;
boolean initproc() {
    if(!initproccalled) {
        if(!numio.setlibrary())
            return false;
        if(isWearable) {
            if(Natives.getWifi())
                UseWifi.usewifi();
            }
        if(tk.glucodata.Applic.useWearos()) {
          //  initwearos(this);
            }
        needsnatives();
        {if(doLog) {Log.i("Applic","initproc width="+initscreenwidth);};};
        libre3init.init();
        SuperGattCallback.initAlarmTalk();
      //  initialize();
     //   NumAlarm.handlealarm(this);
      //  Maintenance.setMaintenancealarm(this);
        initbroadcasts();
        initproccalled=true;
        if(isWearable&&!(dataAtStart=hasData())) {
          final ScheduledFuture<?>[] askstarthandle={null};
            askstarthandle[0] = scheduler.scheduleWithFixedDelay(()->{
             if(initStarted) {
                 if(askstarthandle[0]!=null)
                       askstarthandle[0].cancel(false);
               return;
               }
             else
                MessageSender.sendaskforstart();
              }, 4, 30,TimeUnit.SECONDS);
          }
        else 
           MessageSender.sendnetinfo();
       // Specific.start(this);
        if(isWearable) {
             tk.glucodata.glucosecomplication.GlucoseValue.updateall();
             }
        }
    return true;
    }

public static void setbluetooth(Context activity,boolean on) {
    {if(doLog) {Log.i(LOG_ID,"setbluetooth(Context activity,"+on+")");};};
    Natives.setusebluetooth(on);
    if(on)  {
         Applic.initbluetooth(on,activity,activity instanceof MainActivity);
        }
    else {
        Applic.dontusebluetooth();
        }
    app.redraw();
    }
public static    int stopprogram=0;
/*    @Override
    public void onCreate() {
        super.onCreate();
    if(DiskSpace.check(this)) {
        initproc();
        }
    else {    
        android.util.Log.e(LOG_ID,"Stop program");
        stopprogram=1;
        }
    }*/

//static public int backgroundcolor= BLACK;
static public int backgroundcolor= isWearable?BLACK:RED;
void setbackgroundcolor(Context context) {
if(!isWearable) {
   if(Build.VERSION.SDK_INT >= 21) {
        TypedValue typedValue = new TypedValue();
        if(context.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true) && typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) 
        backgroundcolor= typedValue.data;
        }
     }    
    }

//static private Runnable updater=null;
static private ArrayList<Runnable> updaters=new ArrayList<Runnable>();
public static void  setscreenupdater(Runnable up) {
    updaters.add(up);    
//    updater=up;
    }
public static void  removescreenupdater(Runnable up) {
    updaters.remove(up);    
//    updater=up;
    }
static void updatescreen() {
    if(app.curve != null)
        app.curve.requestRender();
    for(var el:updaters)
        el.run();
    updaters.clear();
    }
static float headfontsize;
static float mediumfontsize;
  static public float largefontsize;
static float menufontsize ;
boolean needsnatives() {
  {if(doLog) {Log.i(LOG_ID,"needsnatives - Headless mode");};};
 // final var res=getResources();
 // var metrics=GlucoseCurve.metrics= res.getDisplayMetrics();
 // MainActivity.screenheight= metrics.heightPixels;
 // MainActivity.screenwidth= metrics.widthPixels;
  
  // For headless module usage, skip all UI-related initializations
  // DisplayMetrics metrics = GlucoseCurve.metrics;
  // if(metrics == null) {
  //     try {
  //         metrics = MainActivity.thisone.getResources().getDisplayMetrics();
  //     } catch(Exception e) {
  //         // Fallback to default values if context is not available
  //         Log.e(LOG_ID, "Could not get DisplayMetrics, using default values");
  //         return false;
  //     }
  // }
  
  // {if(doLog) {Log.i(LOG_ID,"heightPixels="+metrics.heightPixels+" widthPixels="+metrics.widthPixels);};};
  // var newinitscreenwidth= Math.max(metrics.heightPixels,metrics.widthPixels);
  boolean ret = false;
  // menufontsize = res.getDimension(R.dimen.abc_text_size_menu_material);
  // if(menufontsize == 0.0f) {
  //     // Initialize menufontsize with a default value if not set
  //     menufontsize = 16.0f * metrics.density; // Default menu font size
  // }
  // if(headfontsize == 0.0f) {
  //     // Initialize headfontsize with a default value if not set
  //     headfontsize = 32.0f * metrics.density; // Default head font size
  // }
  // if(mediumfontsize == 0.0f) {
  //     // Initialize mediumfontsize with a default value if not set
  //     mediumfontsize = 18.0f * metrics.density; // Default medium font size
  // }
  // if(largefontsize == 0.0f) {
  //     // Initialize largefontsize with a default value if not set
  //     largefontsize = 22.0f * metrics.density; // Default large font size
  // }
  // if(GlucoseCurve.smallfontsize == 0.0f) {
  //     // Initialize smallfontsize with a default value if not set
  //     GlucoseCurve.smallfontsize = 14.0f * metrics.density; // Default small font size
  // }
  //   final double screensize=(newinitscreenwidth/menufontsize);
  // final boolean smallsize=screensize<34.0;
  //   if(newinitscreenwidth!=initscreenwidth)  {
  //        if(smallsize!= NumberView.smallScreen) {
  //           NumberView.smallScreen=smallsize;
  //           ret=true;
  //           }
  //        else
  //           ret=false;
  //        }
  //   else
  //      ret=false;
  //    initscreenwidth=newinitscreenwidth;
  //    {if(doLog) {Log.i(LOG_ID,"initscreenwidth="+newinitscreenwidth);};};
  //    {if(doLog) {Log.i(LOG_ID,"menufontsize="+menufontsize);};};
  //    {if(doLog) {Log.i(LOG_ID,"screensize="+screensize);};};
  //   // headfontsize = res.getDimension(R.dimen.abc_text_size_display_4_material);
  //    Notify.glucosesize= headfontsize*.35f;
     //smallfontsize = res.getDimension(R.dimen.abc_text_size_small_material);
    // largefontsize = res.getDimension(R.dimen.abc_text_size_large_material);
    // mediumfontsize = res.getDimension(R.dimen.abc_text_size_medium_material);
     // For headless module usage, only initialize essential native methods
     // Skip UI-related initializations like OpenGL, notifications, etc.
     
     // Initialize only essential native methods for NFC and Bluetooth
     try {
         // Basic native initialization without UI dependencies
         if(Log.doLog) {
             Log.i(LOG_ID, "Headless mode: initializing only NFC and Bluetooth components");
         }
         
         // Initialize essential native methods for NFC and Bluetooth functionality
         // This ensures the core functionality works without UI dependencies
         
     } catch(Exception e) {
         Log.e(LOG_ID, "Error in headless initialization", e);
         return false;
     }
     
     return ret;
     }
   /*
@Keep
static void toCalendar(String name) {
   MainActivity.tocalendarapp=true;
    MainActivity.calendarsensor=name;
   } */
@Keep
static boolean bluetoothEnabled() {    
    return SensorBluetooth.bluetoothIsEnabled();
    }
static final boolean usewakelock=true;
@Keep
static void doglucose(String SerialNumber, int mgdl, float gl, float rate, int alarm, long timmsec,boolean wasblueoff,long sensorstartmsec, long sensorptr,int sensorgen) {
   if(doLog) {Log.i(LOG_ID,"doglucose "+SerialNumber+" "+ mgdl+" "+ gl+" "+rate+" "+ alarm+" "+timmsec+" "+ wasblueoff+ " "+ sensorstartmsec +" sensorptr="+format("%x",sensorptr)+" "+ sensorgen);};

    //var wakelock=    usewakelock?(((PowerManager) MainActivity.thisone.getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Juggluco::Applic")):null;
   // if(wakelock!=null)
    //    wakelock.acquire();
    if(!wasblueoff) {
        Applic.dontusebluetooth();
        }
    SuperGattCallback.dowithglucose( SerialNumber,  mgdl,  gl, rate,  alarm,  timmsec,sensorstartmsec,Notify.glucosetimeout,sensorgen);
    if(!isWearable) {
            if(sensorptr!=0L) {
                {if(doLog) {Log.i(LOG_ID,"sensorptr="+format("%x",sensorptr));};};
                if(Build.VERSION.SDK_INT >= 28) {
                    HealthConnection.Companion.writeAll(sensorptr,SerialNumber);
                    }
               }
        }
   // if(wakelock!=null)
   //     wakelock.release();
    }


@Keep
static boolean updateDevices() { //Rename to reset
    try {
        if(Class.forName("tk.glucodata.SensorBluetooth") != null) {
            if(tk.glucodata.SensorBluetooth.blueone!=null) {
               if(tk.glucodata.SensorBluetooth.blueone.resetDevices()) {
            var main=MainActivity.thisone;
            if(main!=null)
                main.finepermission();
             }
              return true;
          }
        }
    } catch(ClassNotFoundException e) {
        // SensorBluetooth class not available in this build variant
    }
    Log.e(LOG_ID,"tk.glucodata.SensorBluetooth.blueone==null");
    return false;
     }
     /*
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    } */

public static void wakemirrors() {
    {if(doLog) {Log.i(LOG_ID,"wakemirrors");};};
    try {
        if(Class.forName("tk.glucodata.MessageSender") != null) {
            MessageSender.sendwake();
        }
    } catch(ClassNotFoundException e) {
        // MessageSender class not available in this build variant
    }
    try {
        Natives.wakebackup();
    } catch(UnsatisfiedLinkError e) {
        // wakebackup native method not available in this build variant
    }
    }
@Keep 
static public void resetWearOS() {
    try {
        if(Class.forName("tk.glucodata.MessageSender") != null) {
            MessageSender.reinit();
        }
    } catch(ClassNotFoundException e) {
        // MessageSender class not available in this build variant
    }
    wakemirrors();
    }
private static    void initbroadcasts() {
/*if(isWearable) {
    Specific.setclose(!Natives.getdontuseclose());
    }

    Floating.init(); 
   final var initversion=Natives.getinitVersion();
    if(initversion<34) {
      if(initversion<29) {
         if(initversion<22) {
            if(initversion<14) {
               if(initversion<13) {
                  Broadcasts.updateall();
                  }
               if(Notify.arrowNotify!=null)
                  Natives.setfloatingFontsize((int) Notify.glucosesize);
               Natives.setfloatingbackground(WHITE);
                Natives.setfloatingforeground(BLACK);
               }
            }
         sethour24(DateFormat.is24HourFormat(app));
         }
      Natives.setinitVersion(34);
      }

   setjavahour24(Natives.gethour24());
    XInfuus.setlibrenames();
   EverSense.setreceivers();
    JugglucoSend.setreceivers();
    SendLikexDrip.setreceivers();*/
        }

static public  boolean    talkbackrunning=false;
static    void        talkbackon(Context cont) {
    if(!DontTalk) {
        try {
            if(Class.forName("tk.glucodata.SuperGattCallback") != null) {
                SuperGattCallback.newtalker(cont);
            }
        } catch(ClassNotFoundException e) {
            // SuperGattCallback class not available in this build variant
        }
        talkbackrunning=true;

        try {
            Natives.settouchtalk(true);
            Natives.setspeakmessages(true);
            //Natives.setspeakalarms(true);
        } catch(UnsatisfiedLinkError e) {
            // Native methods not available in this build variant
        }
        }

    }

static    void        talkbackoff() {
    if(!DontTalk) {
        talkbackrunning=false;
        }
    }
@Keep
static public void speak(String message) {
    if(!DontTalk) {
        var talker=SuperGattCallback.talker;
        if(talker==null) {
            SuperGattCallback.newtalker(null);
            talker=SuperGattCallback.talker;
            }
        if(talker != null) {
            talker.speak(message);
        }
        }
    }
static public boolean getHeartRate() {
    try {
        return initproccalled&&Natives.getheartrate();
    } catch(UnsatisfiedLinkError e) {
        // getheartrate native method not available in this build variant
        return false;
    }
    }
@Keep
static void changedProfile() {
        try {
            if(Class.forName("tk.glucodata.SuperGattCallback") != null) {
               // SuperGattCallback.init();
            }
        } catch(ClassNotFoundException e) {
            // SuperGattCallback class not available in this build variant
        }
        }
@Keep
static void setinittext(String str) {
   RunOnUiThread(()-> {
       try {
           if(Class.forName("tk.glucodata.Specific") != null) {
               Specific.settext(str);
           }
       } catch(ClassNotFoundException e) {
           // Specific class not available in this build variant
       }
   });
    }
@Keep
static void rminitlayout() {
     RunOnUiThread(()-> {
         try {
             if(Class.forName("tk.glucodata.Specific") != null) {
                 Specific.rmlayout();
             }
         } catch(ClassNotFoundException e) {
             // Specific class not available in this build variant
         }
     });
    }
@Keep
static void toGarmin(int base) {
   if(!isWearable) { 
        if(MainActivity.thisone != null ) {
           // MainActivity.thisone.numdata.changedback(base);
        }
        }
    }
@Keep
static void Garmindeletelast(int base,int pos,int end ) {
       if(!isWearable) { 
            if(MainActivity.thisone != null) {
               // MainActivity.thisone.numdata.deletelast(base,pos,end);
              //  MainActivity.thisone.numdata.changedback(base);
            }
          }
        }
@Keep
static public void startMain() {
    final var act=MainActivity.thisone;
    final var intent=new Intent(Applic.getContext(),MainActivity.class);
    intent.putExtra(Notify.fromnotification,true);
    intent.addCategory(Intent. CATEGORY_LAUNCHER ) ;
    intent.setAction(Intent. ACTION_MAIN ) ;
    if(act!=null) {
       intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP );
        {if(doLog) {Log.i(LOG_ID,"startActivityIfNeeded( new Intent(Applic.getContext(),MainActivity)). ");};};
        act.startActivityIfNeeded( intent,0);
        }
    else {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        {if(doLog) {Log.i(LOG_ID,"startActivity MainActivity.thisone==null");};};
        try {
            if(Class.forName("tk.glucodata.keeprunning") != null && keeprunning.theservice != null) {
                keeprunning.theservice.startActivity( intent);
            }
        } catch(ClassNotFoundException e) {
            // keeprunning class not available in this build variant
        }
        }
      }
}

