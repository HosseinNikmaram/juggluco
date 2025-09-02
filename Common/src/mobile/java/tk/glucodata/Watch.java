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
/*      Fri Jan 27 15:32:11 CET 2023                                                 */


package tk.glucodata;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static tk.glucodata.Applic.mgdLmult;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.MessageSender.initwearos;
import static tk.glucodata.util.getbutton;
import static tk.glucodata.util.getcheckbox;

import android.view.View;
import android.view.ViewGroup;

class Watch {
static private final boolean TestBridge=BuildConfig.DEBUG;
static private float glucose=80f;
static private float trend=-5.2f;
static private final  String LOG_ID="Watch";
static public void show(MainActivity context) {

	       var watchdrip=getcheckbox(context,"Watchdrip", SuperGattCallback.doWearInt);
		watchdrip.setOnCheckedChangeListener(
			(buttonView,  isChecked) ->  {
				Natives.setwatchdrip(isChecked);
				tk.glucodata.watchdrip.set(isChecked);
				});
       var gadget=getcheckbox(context,"GadgetBridge", SuperGattCallback.doGadgetbridge);
		gadget.setOnCheckedChangeListener(
			(buttonView,  isChecked) ->  {
				Natives.setgadgetbridge(isChecked);
				SuperGattCallback.doGadgetbridge=isChecked;
				});
	var test=TestBridge?getbutton(context,"Test"):null;
	View[] mibandrow;
	if(TestBridge) {
		test.setOnClickListener(v-> {
			int mgdl;
			trend += 0.6f;
			if (trend > 5f)
				trend = -5f;
			if(Applic.unit==1) {
				glucose += 0.6f;
				if (glucose > 28f)
					glucose = 2.2f;

			mgdl=(int)Math.round(glucose*mgdLmult);
			}
			else {
				glucose += 13f;
				if (glucose > 500f)
					glucose = 40f;
				mgdl=(int)glucose;

			}
				
				Gadgetbridge.sendglucose(""+glucose,mgdl,glucose,trend, System.currentTimeMillis());
				});

		mibandrow=new View[]{watchdrip,gadget,test};
		}
	else  {
		mibandrow=new View[]{watchdrip,gadget};
		}
	var usexdripserver=Natives.getusexdripwebserver();
	var server=getcheckbox(context,R.string.webserver,usexdripserver);
	server.setOnCheckedChangeListener( (buttonView,  isChecked) -> Natives.setusexdripwebserver(isChecked));
	var serverconfig=getbutton(context,R.string.config);
	var usegarmin=Natives.getusegarmin();
	var kerfstok=getcheckbox(context,"Kerfstok",usegarmin); 
	var status=getbutton(context,R.string.status);
	status.setVisibility(usegarmin?VISIBLE:INVISIBLE);
	kerfstok.setOnCheckedChangeListener(
			 (buttonView,  isChecked) -> {
			 if(isChecked&&!usegarmin)
			//	Applic.getContext().numdata.reinit(context);
			status.setVisibility(isChecked?VISIBLE:INVISIBLE);
			 });
	var useWearos=Applic.useWearos();
	if(useWearos) {
		var sender=tk.glucodata.MessageSender.getMessageSender();
	        if(sender!=null) sender.finddevices();
 		Natives.networkpresent();
		}

	var wearbox=getcheckbox( context, "WearOS", useWearos);
	var wearossettings=getbutton(context,R.string.config);
	wearossettings.setVisibility(useWearos?VISIBLE:INVISIBLE);
	wearbox.setOnCheckedChangeListener(
			 (buttonView,  isChecked) -> {
	//Wearos.setuseWearos(isChecked);
			 if(isChecked) {
			 	if(!useWearos) {
					initwearos(Applic.getContext());
					}
				else  {
					var sender=tk.glucodata.MessageSender.getMessageSender();
					if(sender!=null) sender.finddevices();
					}
				Natives.networkpresent();
				}
			wearossettings.setVisibility(isChecked?VISIBLE:INVISIBLE);
			 });


	}
	

}



