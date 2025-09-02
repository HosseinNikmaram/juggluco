package tk.glucodata;


import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tk.glucodata.nums.numio;


class MirrorString {
private static final String LOG_ID="MirrorString";
static private JSONArray jsonArray(String[] args) {
    var uit=new JSONArray(); 
    for(var el:args) {
        uit.put(el);
        }
    return uit;
    }
static private String[] fromjsonArray(JSONArray jar) throws JSONException {
     final int len=jar.length();
     String[] uit=new String[len];
     for(int i=0;i<len;++i) {
        uit[i]=jar.getString(i);
        }
    return uit;
    }
    /*
static String makestring(String[] names,int nr,boolean detect,String port,boolean nums,boolean stream,boolean scans,boolean recover,boolean receive,boolean activeonly,boolean passiveonly,String pass,long starttime,String label,boolean testip,boolean hasname) throws JSONException {
        JSONObject json=new JSONObject();
        json.put("names",jsonArray(names));
        json.put("nr",nr);
        json.put("detect",detect);
        json.put("port",port);
        json.put("nums",nums);
        json.put("stream",stream);
        json.put("scans",scans);
        json.put("receive",receive);
        json.put("activeonly",activeonly);
        json.put("passiveonly",passiveonly);
        json.put("pass",pass);
        json.put("starttime",starttime);
        json.put("label",label);
        json.put("testip",testip);
        json.put("hasname",hasname);
        return json.toString()+" MirrorJuggluco";
        } */
    static void fromstring(String jsonstr,MainActivity act) {
        try {
            JSONObject json =new JSONObject(jsonstr);
            String[] names=fromjsonArray(json.getJSONArray("names"));
            int nr=json.getInt("nr");
            boolean detect=json.optBoolean("detect",false);
            String port=json.getString("port");
            boolean nums=json.optBoolean("nums",false);
            boolean stream=json.optBoolean("stream",false);
            boolean scans=json.optBoolean("scans",false);
            boolean receive=json.optBoolean("receive",false);
            boolean activeonly=json.optBoolean("activeonly",false);
            boolean passiveonly=json.optBoolean("passiveonly",false);
            String pass=json.isNull("pass")?null:json.getString("pass");
        //    long starttime=json.getLong("starttime");
           long starttime=0L;
            String label=json.isNull("label")?null:json.getString("label");
            boolean testip=json.optBoolean("testip",false);
            boolean hasname=json.optBoolean("hasname",false);
            Runnable save=()-> {
               int pos=Natives.changebackuphost(-1,names,nr,detect,port, nums,stream,scans,false,receive,activeonly,passiveonly,pass,starttime,label,testip,hasname);
               if(pos<0) {
                      }
               else   {
                  Applic.argToaster(Applic.getContext(),R.string.mirrorscansucces, Toast.LENGTH_SHORT);
                  Applic.wakemirrors();
                  }
                   };
            if(receive) {
                String type=""; 
                if(!nums&&numio.hasNumdata()) {
                    type=act.getString(R.string.amountsname); 
                    }
                if(!scans&&Natives.hasscans()) {
                    final var addstr=act.getString(R.string.scansname); 
                    if(type.length()>0)
                        type+=", "+addstr; 
                    else type=addstr; 
                    }
                if(!stream&&Natives.hasstreamed( )) {
                    final var addstr=act.getString(R.string.streamname); 
                    if(type.length()>0)
                        type+=", "+addstr; 
                    else type=addstr; 
                    }
                if(type.length()>0) {
                        Confirm.ask(act,act.getString(R.string.datapresent)+type,act.getString(R.string.overwrite),save);
                        return;
                       } 
                 }
             save.run();
            ;
            }
         catch(Exception th) {
            Log.stack(LOG_ID,"fromstring",th);
            Applic.argToaster(Applic.getContext(),th.toString(),Toast.LENGTH_SHORT);
            }
        }
public static   void      makeMirror(String jsonstr,MainActivity act) {
        fromstring(jsonstr,act);
        }
}
