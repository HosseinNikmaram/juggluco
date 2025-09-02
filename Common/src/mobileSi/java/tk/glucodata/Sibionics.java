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
/*      Thu Apr 04 20:10:13 CEST 2024                                                 */


package tk.glucodata;


import static tk.glucodata.Applic.Toaster;
import static tk.glucodata.Applic.isWearable;
import static tk.glucodata.Applic.useZXing;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MainActivity.REQUEST_BARCODE;
import static tk.glucodata.MainActivity.REQUEST_BARCODE_SIB2;
import static tk.glucodata.ZXing.scanZXingAlg;
import static tk.glucodata.util.getbutton;

import android.widget.RadioGroup;
import android.widget.Toast;

//import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
//import com.google.mlkit.vision.barcode.common.Barcode;
//import com.google.mlkit.vision.barcode.common.Barcode;
//import com.google.mlkit.vision.barcode.common.Barcode;

//import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
//import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class Sibionics {
private static final String LOG_ID="Sibionics";


private static void     wrongtag() {
    Toaster("Wrong QR code") ;
    }




/*
E007-0M0063KNUJ0....
      LT2309GEPD   
802JPPLT2309GEPD

GEPD802J
31108 GEPD802J PP7
Sensorname
sensorgegs
int len=sensorgegs.size();
startpos=len-19

longserialnumber 


(01) 06972831640165
(11) 231209
(17) 241208
(10) LT41 231108 C

(21) 231108 GEPD802J PP76

0106972831640165112312091724120810LT41231108C21231108GEPD802JPP76 
^]0106972831641483112411201726051910LT46241155C^]21P22411J6EP
0106972831640165112312091724120810LT41231108C21231108 GEPD 802J PP76 
*/
//LT2309GEPD
/*
Sibionics2:
*/

private static void selectType(String name,long dataptr,MainActivity act) {
    int subtype=Natives.getSiSubtype(dataptr);
    /*
    if(subtype==3) {
        Natives.freedataptr(dataptr);
        return;
        } */

    var group=new RadioGroup(act);
    int id=0;

    group.check(subtype);
   var ok=getbutton(act, R.string.ok);


   MainActivity.setonback(() -> {
      int type=group.getCheckedRadioButtonId();
      Log.i(LOG_ID,"getCheckedRadioButtonId()="+type);
      if(type>=0) {
          Natives.setSiSubtype(dataptr,type);
          if(type==3) {
                Confirm.ask2(act,act.getString(R.string.scantranstitle) ,act.getString( R.string.scantransmessage) , 
                ()-> {
                        scanner(act,REQUEST_BARCODE_SIB2,dataptr);
                        },
                () -> {
                    Natives.finishSensor(dataptr);
                    Natives.freedataptr(dataptr);
                    SensorBluetooth.sensorEnded(name);
                    }

                );

            }
           else {
              }
            }

      });
   ok.setOnClickListener(v-> {
        MainActivity.doonback();
        });

    }

static long wasdataptr=0L;
static void connectSensor(final String scantag,MainActivity act,int request,long dataptr2)  {
     if(!isWearable) {
        switch(request) {
            case REQUEST_BARCODE:{
                if(scantag.endsWith("MirrorJuggluco")) {
                    MirrorString.makeMirror(scantag,act);
                    return;
                    }
                 else {
                    String name=Natives.addSIscangetName(scantag);
                    if(name!=null)  {
                       MainActivity.tocalendarapp=true;
                       var dataptr= Natives.getdataptr(name);
                       int type=Natives.getLibreVersion(dataptr);
                       {if(doLog) {Log.i(LOG_ID,"type="+type);};};
                       if(type== 0x10) {
                            selectType(name,dataptr,act);
                            }
                       else
                            Natives.freedataptr(dataptr);
                       var res=SensorBluetooth.updateDevices();
                      // SuperGattCallback.glucosealarms.setLossAlarm();
                       Applic.wakemirrors();
                       if(res) {
                            act.finepermission(); 
                            }
                          else
                            act.systemlocation();
                       return;
                       }
                    }
                 }break;
               case REQUEST_BARCODE_SIB2: {
                    if(Natives.siTransmitterScan(dataptr2,scantag)) {
                        Natives.freedataptr(dataptr2);
                        return;
                        }
                    else {
                        transmitterScanCancelled(dataptr2);
                        }

                    }
                }
                }
          wrongtag(); 
         }

static void transmitterScanCancelled(long dataptr2) {
            if(dataptr2!=0L) {
                Natives.finishSensor(dataptr2);
                var serial=Natives.getSensorName(dataptr2);
                Natives.freedataptr(dataptr2);
                if(serial!=null)  {
                    Log.i(LOG_ID,"transmitterScanCancelled "+serial);
                    SensorBluetooth.sensorEnded(serial);
                    }
                 }
            else {
                Log.i(LOG_ID,"transmitterScanCancelled dataptr==0");
                }
           }



public static void scanner(MainActivity act,int type,long dataptr) {
    if (!isWearable) {
        if(!Natives.getGoogleScan())
            scanZXingAlg(act,type,dataptr);
        else {
            try {
                scanGoogle(act,type,dataptr);
                } catch (Throwable th) {
                    Log.stack(LOG_ID, "scanGoogle", th);
                    scanZXingAlg(act,type,dataptr);
                    }
           }
      }
}
public static void scan(MainActivity act,int type) {
    scanner(act,type,0L);
    }
private static void scanGoogle(MainActivity act,int type,long dataptr) {
     if(!isWearable) {
        if(doLog) {Log.i(LOG_ID, "before scan");};
        final var options =  new com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder().setBarcodeFormats( com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX, com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build();
        final var scanner =  com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(act, options);
        scanner.startScan().addOnSuccessListener(
           barcode -> {
               var rawValue = barcode.getRawValue();
               var message="Scanned: "+rawValue;
               if(doLog) {Log.i(LOG_ID,message);};
               connectSensor(rawValue,act,type,dataptr);
               })
           .addOnCanceledListener(
               () -> {
                    var message="Scan cancelled";
                    if(doLog) {Log.i(LOG_ID,message);};
                    Toast.makeText(act, message, Toast.LENGTH_LONG).show();
                    transmitterScanCancelled(dataptr);
                     // Task canceled
                   })
       .addOnFailureListener(
           e -> {
            var message=e.getMessage();
            if(doLog) {Log.i(LOG_ID,message);};
            Toast.makeText(act, message, Toast.LENGTH_SHORT).show();  
            if(useZXing) {
                Toast.makeText(act, "Move to zXing", Toast.LENGTH_SHORT).show();
                scanZXingAlg(act,type,dataptr);
                }
        
         // Task failed with an exception
           });

   }
    }

/*
static void testsibionics() {
if(doLog) {
  String tag="^]0106972831640820112312221724122110LT48231127G^]212311271NTK237GAA21";
  var name=Natives.addSIscangetName(tag);
  long dataptr=Natives.getdataptr(name);
  var si=new SiGattCallback(name, dataptr);
 si.testchanged();
  }
};*/
}
