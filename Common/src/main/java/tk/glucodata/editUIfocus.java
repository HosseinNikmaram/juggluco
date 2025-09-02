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

import android.view.View;


public class editUIfocus implements View.OnFocusChangeListener {
//public static Editable edit;
//final private String LOG_ID="editUIfocus";
    @Override

    public void onFocusChange(View v, boolean hasFocus) {
	MainActivity act= (MainActivity) v.getContext();
	/*
	DisplayMetrics metrics=act.getResources().getDisplayMetrics();
	int width=GlucoseCurve.getwidth();	
	*/
	if(hasFocus) {
//		{if(doLog) {Log.i(LOG_ID,"focus1 width="+metrics.widthPixels+" "+width);};};
//		v.requestLayout();
		act.showui=true;
	        act.showSystemUI();
//		{if(doLog) {Log.i(LOG_ID,"focus2 width="+metrics.widthPixels+" "+width);};};
	}
	
	else {
//		{if(doLog) {Log.i(LOG_ID,"nofucos1 width="+metrics.  widthPixels+" "+width);};};
		act.showui=false;
	       act.hideSystemUI();
//		{if(doLog) {Log.i(LOG_ID,"nofucos2 width="+ metrics.  widthPixels+" "+width);};};
	   }
    }
}
