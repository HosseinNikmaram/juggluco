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


package tk.glucodata.nums;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener;
import com.garmin.android.connectiq.ConnectIQ.IQConnectType;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationListener;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSendMessageListener;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import static consts.consts.COLORBLACK;
import static consts.consts.GOTGLUCOSE;
import static consts.consts.GOTSTOPALARM;
import static consts.consts.STOPALARM;
import static tk.glucodata.Applic.isRelease;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import androidx.annotation.NonNull;
import tk.glucodata.Applic;
import tk.glucodata.BuildConfig;
import tk.glucodata.Log;
import tk.glucodata.Natives;
import tk.glucodata.Notify;

import static consts.consts.DELETE;
import static consts.consts.DELETED;
import static consts.consts.DIDSETENDNUM;
import static consts.consts.GETENDNUM;
import static consts.consts.GLUCOSE;
import static consts.consts.GOTNUMS;
import static consts.consts.HAVENUMS;
import static consts.consts.MORENUMS;
import static consts.consts.NOMORENUMS;
import static consts.consts.NUMS;
import static consts.consts.PUTLABELS;
import static consts.consts.PUTNUMS;
import static consts.consts.PUTPRECISION;
import static consts.consts.RECEIVEDCUTS;
import static consts.consts.RECEIVEDPRECISION;
import static consts.consts.RECEIVEDUNITS;
import static consts.consts.SENDERROR;
import static consts.consts.SETENDNUM;
import static consts.consts.SHORTCUTS;
import static consts.consts.START;
//import static consts.consts.ASKLOWEST;
import static consts.consts.STOP;
import static consts.consts.STRING;
import static consts.consts.maxstorage;
import static tk.glucodata.Log.doLog;
import static tk.glucodata.nums.numio.didreceivebackup;

//public class AllData extends AndroidViewModel {
public class AllData  {
   String infostr="";
//    private int  lastheart = 0, lastpress = 0;
//    private File numdatfile;

    private final IQlisten IQlistener = new IQlisten();
//    private Object ups[]=null;// = new List<?>[]{Arrays.asList(NUMS, lastnums), Arrays.asList(HEART, lastheart)};
//    private int upiter = 0;
//    NumFragment numfrags[]={null,null};
//    DeviceActivity activity=null;
public    List<IQDevice> devices=null;
   public int devused=0;
//    public static final String IQDEVICE = "IQDevice";
//   static public boolean isReleaseID= BuildConfig.isReleaseID==1;
   // public static final String MY_APP =  isReleaseID?"96cd3c6949334484a2ff571981c41566":"7212fbfb1f574bdc8459c04efd90a604";
//       public static final String MY_APP =  isReleaseID?"fd298af3f2df4ef092593db875bbf5c9":"7212fbfb1f574bdc8459c04efd90a604";
   // public static final String MY_APP = "699c0800c0454f9d8038f671e9ed9332"; //Widget

    private static final String LOG_ID = "Alldata";

    ConnectIQ mConnectIQ;

    private IQApp mMyApp;
    private boolean mAppIsOpen = false;



final    private IQOpenApplicationListener mOpenAppListener = new IQOpenApplicationListener() {
        @Override
        public void onOpenApplicationResponse(IQDevice device, IQApp app, IQOpenApplicationStatus status) {
           appmissing=-1;
           if (status == IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING) {
                Applic.argToaster(getApplication(), "APP_IS_ALREADY_RUNNING", Toast.LENGTH_SHORT);
                mAppIsOpen = true;
                startall() ;
            } else {
                Applic.argToaster(getApplication(), "Open App", Toast.LENGTH_SHORT);
                mAppIsOpen = false;
//      sync();
            }

       }    

    };
public boolean sendtowatch=false;
private List<Object> glucosemess=null;

private static boolean gotgotglucose=false;
public void sendglucose(String ident,long tim,float glu,float rate,int off) {
   List<Object> uit = Arrays.asList(ident,tim,glu,rate,off,Applic.unit==1?1:0);
   if(!sendtowatch||isSending()) 
      glucosemess=uit;
   else  {
      if(gotgotglucose)
         setSending(true);
      realsendmessage(Arrays.asList(GLUCOSE, uit));
      }
    }

void realsendendnum(int base) {
   {if(doLog) {Log.i(LOG_ID, "realsendendnum "+base);};};
   sendends[base]=false;
   int end= numio.getlastnum( base) ;
   realsendmessage(Arrays.asList(SETENDNUM,base,end));
   }

boolean[] sendends={false,false};

 void sendendnum(int base) {
   if(didreceivebackup(base))  {
      return;
      }
    if(!isSending()) {
       setSending(true);
       realsendendnum(base);
       }
    else {
        log("sendend later "+base);
         sendends[base]=true;
        sendmessages();
    }
   }
public void setcolor(boolean black) {
   if(!usewatch)
      return;
   sendmessage(Arrays.asList(COLORBLACK,black?1:0));
   }
boolean[] asksendends={false,false};
private void realaskendnum(int base) {
   asksendends[base]=false;
   realsendmessage(Arrays.asList(GETENDNUM,base));
   }
private void askendnum(int base) {
   if(didreceivebackup(base)) 
      return;
    if(!isSending()) {
   setSending(true);
   realaskendnum(base);
       }
    else {
        log("askendend later "+base);
   asksendends[base]=true;
        sendmessages();
    }
   }
   /*
public void resendtowatch() {
   Natives.resendtowatch();
   changedback(1);
   sendendnum(1);
   changedback(0);
   sendendnum(0);
   }*/
void false2(boolean[] ar) {
   ar[0]=ar[1]=false;
   }
void clearsyncgegs() {
   false2(askednums);
   false2(asksendends);
   false2(backchanged);
   false2(sendends);
   mqueue.clear();
   setSending(false);
}


private void setSendlast(int base) {
    long ptr=numio.numptrs[base];
	int last=Natives.getlastNum(ptr); 
    int back=Math.max(0,last-20);
    {if(doLog) {Log.i(LOG_ID,base+" setSendlast last="+last+" back="+back);};};
    Natives.setchangedNum(ptr,back);
    }
private void givebase0nums() {
    Log.i(LOG_ID,"givebase0nums()"); 
    Natives.setshouldsendlabels(true);
    Natives.setsendcuts(true);
    setSendlast(0);
    setSendlast(1);

    final int last = numio.getlastnum(0);
    if(last>0) {
        changedback(0);
        }
    else {
          sendmessage(Arrays.asList(MORENUMS,0, 0));
        }
    sendtowatch=false; //force sync
    startglucose();
    //sync();
    }
public void sync() {
   if(!usewatch)
      return;

   if(Natives.shouldsendlabels()) 
      sendlabels();
   {if(doLog) {Log.i(LOG_ID,"sync");};};

   getnums(0); 
   askendnum(0);
   changedback(1);
   sendendnum(1);
   changedback(0);
   getnums(1); 
   if(Natives.sendcuts()) 
      sendshortcuts(Natives.getShortcuts());
      
   }
   /*
public void sendunits(int unitin) {
   if(!usewatch)
      return;
   int unit=unitin==1?1:0;
   sendmessage(Arrays.asList(GLUNITS,unit));
   }
   */
public boolean startglucose() {
   if(!sendtowatch)  {
      sendtowatch=true;
      wasglucose=false;
      int unit=Natives.getunit();
      Applic.unit=unit;
      sync();
      return true;
      }
   return false;
   }
public void startall() {
   sendmessage(Arrays.asList(START));
   }
boolean wasglucose=false;
public void stopglucose() {
   wasglucose=false;
   sendtowatch=false;
}
public void pushglucose() {
   if(sendtowatch)  {
      wasglucose=true;
      sendtowatch=false;
      }
}
public void backglucose() {
   sendtowatch=wasglucose;
//   setSending(false);
   }
private final static Application getApplication() {
   return Applic.app;
    }

static private void senderror(int type) {
   errorm("Senderror "+type);
   }

static private void errorm(String mess) {
    Log.e(LOG_ID,mess);
    Applic.Toaster(mess);
    }
static final int sendchunk = 50;
private int[] sendgoal = {0,0};
public long receivedmessage=0L;
class IQlisten implements IQApplicationEventListener {
@Override
public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, IQMessageStatus status) {
    if (status != IQMessageStatus.SUCCESS) {
   {if(doLog) {Log.i(LOG_ID,"onMessageReceived failt");};};
   return;
    }
    if (message.size() == 0) {
   {if(doLog) {Log.i(LOG_ID,"onMessageReceived size==0");};};
   return;
    }

    for (Object o : message) {
   if (!(o instanceof List<?>)) {
       Log.e(LOG_ID,"onMessageReceived  no instance of List<?>: " + o.getClass().getName());
       continue;
   }
   receivedmessage=System.currentTimeMillis();

    List<?> li = (List<?>) o;
   int kind=(Integer) li.get(0);
  log("onMessageReceived " + kind);
    switch (li.size()) {
       case 1:
       case 2:
           switch(kind) {
              case GOTSTOPALARM:
              case COLORBLACK:break;
              case RECEIVEDPRECISION:
                Natives.setshouldsendlabels(false);
                break;
              case DIDSETENDNUM:break;
              case GOTGLUCOSE:gotgotglucose=true;break;
              case RECEIVEDCUTS: Natives.setsendcuts(false);break;
/*              case RECEIVEDUNITS: 
                 {if(doLog) {Log.i(LOG_ID,"RECEIVEDUNITS");};};
                 setSending(false);
                 sync();
                 return; */
    //           case RECEIVEDLABELS:Natives.havesendlabels();
              case START: 
                 setSending(false);
                 if(li.size()==1||((Boolean)li.get(1))==false) {
                     if(startglucose())
                         return;
                       }
                 else {
                    givebase0nums();
                    return;
                    }
                 break;
              case STOP: stopglucose();break;
              case STRING: infostr = (String) li.get(1); break;
              case SENDERROR:senderror((Integer) li.get(1));break;
              case STOPALARM: Notify.stopalarmnotsend(false);break;
//              case ASKLOWEST: givebase0nums();break;
              };

              break;

   case 3: {
       int base =(Integer) li.get(1);
      int num=(Integer)li.get(2);
      switch(kind) {
    //          case HAVENUMS:    sendmessage(Arrays.asList(NUMS, base,getlastnum(base))); break;
          case SETENDNUM:{
             numio.setlastnum(base,num);
             };break;
          case DELETE: {
             log("DELETE "+base+" "+num);
             numio.delete(base,num);
             Applic.app.redraw();
             realsendmessage(Arrays.asList(DELETED,base, num)); 
             return;
             }
          case NOMORENUMS: {
             int last=(Integer)li.get(2);
             log("NOMORENUMS "+base+ " "+last);
             numio.updated(base,last);
             Applic.app.redraw();

             }
             ;break;
          };
      };break;
   case 4: {
//   Communications.transmit([GOTNUMS,base,begin,end],
       int base=(Integer)li.get(1);
       int begin = (Integer) li.get(2), end = (Integer) li.get(3);
       switch (kind) {
          case DELETED: 
              log("DELETED "+base+" "+begin+"-"+end); 
              break;
          case GOTNUMS: {
              if(!backchanged[base])
                 Natives.setchangedNumLater(numio.numptrs[base],end);
              final var sgoal=sendgoal[base];
              log("GOTNUMS "+base+ " "+begin+"-"+end+" sendgoal="+sgoal);
              if(sgoal > end) {
                 putnums(base,end, sgoal);
                 return;
                 }
              else {
                 sendgoal[base]=0;
                 if(base==0) {
                    if(Natives.shouldsendlabels())  {
                       setSending(false);
                       sendlabels();
                       return;
                       }
                    }
                 }
          }
          ;
          break;
       }
       }; break;

   case 5: 
       int type = kind;
       int base= (Integer) li.get(1);
       int size = (Integer) li.get(2);
       int  end= (Integer) li.get(3);
   switch (type) {
       case HAVENUMS:    
          log("HAVENUMS "+ base+" "+size+" "+end);
          if((end- size)>numio.getlastnum(base)) {
             realgetnums(base) ;
             return;
             }

       case NUMS: {
          log("NUMS "+ base+" "+size+" "+end);
          List<Object> gegs = (List<Object>) li.get(4);
          log("mess: "+gegs);
           if (size != gegs.size()) {
             errorm("input said " + size + " got " + gegs.size());
             continue;
             }
         int datiter = end - gegs.size(),start=datiter;
          ListIterator<List<Number>> iter = ((List<List<Number>>) li.get(4)).listIterator();
          while (iter.hasNext())
             numio.writeAr(base,datiter++, iter.next());
          
          numio.updatedstartend(base,start,datiter);
          realsendmessage(Arrays.asList(MORENUMS,base, datiter));
          return;
          }
      } ; break;
   default:
       errorm("Don't know messages of size="+  li.size());
       break;
    }
    ;
    }
   nextmessage();
}


}
static final private void log(String str) {
   {if(doLog) {Log.i(LOG_ID,str);};};
   }
   /*
public void allback(int base) {
   if(!usewatch)
      return;   


      
   long ptr=numio.numptrs[base];
   int last =  Natives.getlastNum(ptr);
   int first =  Natives.getfirstNum(ptr);
        sendmessage(putnummer(base,first,last));
   }
*/
private boolean[] backchanged=new boolean[2];
private boolean realchangedback(int base) {
   backchanged[base]=false;
   Object mess=getchangedback(base);
   if(mess!=null) {
      realsendmessage(mess);
      return true;
      }
   return false;
   }
public void changedback(int base) {
   if(!usewatch)
      return;

    if(!isSending()) {
      {if(doLog) {Log.i(LOG_ID,"changedback "+base);};};
      setSending(true);
      if(!realchangedback(base)) {
         nextmessage();
         }
      }
   else {
      {if(doLog) {Log.i(LOG_ID,"backchanged "+base+" = true");};};
      backchanged[base]=true;
       } 
}
private Object  getchangedback(int base) {
   long ptr=numio.numptrs[base];
   int last =  Natives.getlastNum(ptr);
   int changed=  Natives.getchangedNum(ptr);
   int one=  Natives.getonechangeNum(ptr);
log(base+" changedback: last="+last+" changed="+changed+" one="+one);
   if(changed<last) {
      if(one>0&&one<changed)
         return putnummer(base,one,last);
      else
         return putnummer(base,changed,last);
      }
   else {
      if(one>0&&one<last)
         return putnummer(base,one,one+1);
      }
   return null;
   }

private  Object putnummer(int base,int beg, int end) {
    log("putnummer("+base+","+beg+","+end+")");
    int len = end - beg;
    if (len > maxstorage) {
       beg = end - maxstorage;
       }
    return     getputnums(base,beg, end);
    }

private void putnums(int base,int beg, int end) {
   Object mess=getputnums(base,beg,end);
   if(mess!=null)
      realsendmessage(mess);
   else
      nextmessage();
   }
private Object getputnums(int base,int begin, int end) {
   int last = numio.getlastnum(base);
   {if(doLog) {Log.i(LOG_ID,"getputnums base="+base+" begin="+begin+" end="+end+" last="+last);};};
   if(last < end) {
       end = last;
       if(begin>=end)
          return null;
       }
    int len = end - begin;
    if(len > sendchunk) {
       sendgoal[base] = end;
       end = begin + sendchunk;
       {if(doLog) {Log.i(LOG_ID,"getputnums len="+len+" > "+sendchunk+" sendgoal="+sendgoal[base]);};};
       }
    List<List<Number>> ar = new ArrayList<>();
    try  {
       for(int pos = begin; pos < end; pos++) {
           var el=   numio.readAr(base,pos);
           ar.add(el);
           }
    } catch (Exception e) {
       Log.stack(LOG_ID,"getsendnums ",e);
       return null;
        }
    return Arrays.asList(PUTNUMS, base,begin,end,ar);
    }
private final int[][] dodelete ={{-1,-1},{-1,-1}};


private void senddeleteone(int base) {
   realsendmessage(Arrays.asList(DELETE,base,dodelete[base][0],dodelete[base][1]));
   dodelete[base][0]=dodelete[base][1];
   }
private boolean deletelater(int base) {
   int last=numio.getlastnum(base);
   if(last>dodelete[base][0])
      dodelete[base][0]=last;
   if(dodelete[base][0]<dodelete[base][1]) {
      senddeleteone(base);
      return true;
      }
   return false;
   }
public void deletelast(int base,int pos,int end ) {
 if(!usewatch)
      return;
  {if(doLog) {Log.i(LOG_ID,"delete "+base+" "+pos+"-"+end);};};
  dodelete[base][0]=pos;
  if(end>dodelete[base][1]) 
       dodelete[base][1]=end;
  if(!isSending()) {
       setSending(true);
       senddeleteone(base);
       }
}
/*
void addmessages(final Object[] upin) {
   if(upiter>0) {
      final Object[] tmp =ups;
      final int total=upin.length+upiter;
       ups= new Object[total];
        System.arraycopy(ups, 0, upin, 0, upin.length);
        System.arraycopy(ups, upin.length, tmp, 0, upiter);
      }
   else {
      ups=upin;
      }
   upiter=ups.length;
   }

    void nextmessage() {
        if (upiter > 0)
            sendmessage(ups[--upiter]);
    }
*/
private Queue<Object> mqueue= new LinkedList<Object>();

private boolean sendmessage(Object obj) {
    if(!isSending()) {
   setSending(true);
        realsendmessage(obj);
   return true;
       }
    else {
        log("sendmessage add to queue");
        mqueue.add(obj);
        sendmessages();
   return false;
    }
   }
   
public void sendmessages() {
      if(!usewatch)
         return;
   if(!isSending()) {
           nextmessage() ;
      }
   }
/*
void addmessages(final Object[] upin) {
   Collections.addAll(mqueue, upin);
   sendmessages();   
   }
   */
private boolean todelete(int base) {
   return dodelete[base][0]<dodelete[base][1] ;
   }
public boolean waiting() {
   return(glucosemess!=null||!mqueue.isEmpty()||todelete(0)||todelete(1)||backchanged[0]||backchanged[1]||askednums[0]||askednums[1]);
   }
boolean sendoldglucose() {
   if(sendtowatch&&glucosemess!=null) {
      {if(doLog) {Log.i(LOG_ID,"sendoldglucose");};};
            long nu   =System.currentTimeMillis()/1000;
      long glutime=(long)glucosemess.get(1);
      List<Object> tmp=glucosemess;
      glucosemess=null;
      if((nu-glutime)<60) {
         if(!realsendmessage(Arrays.asList(GLUCOSE, tmp))) {
            if(glucosemess==null)
               glucosemess= tmp;
            }
         return true;
         }
      }
   return false;
   }
public void nextmessage() {
   {if(doLog) {Log.i(LOG_ID,"nextmessage");};};
   setSending(true);
   if(sendoldglucose())
      return;
   Object obj=mqueue.poll();
   if(obj!=null) {
      {if(doLog) {Log.i(LOG_ID,"from queue");};};
      realsendmessage(obj);
      return;
      }

   else  {
      
      if(deletelater(0))
         return;
      if(deletelater(1))
         return;
      if(backchanged[1]&& realchangedback(1))
         return;

      if(askednums[0]) { 
         realgetnums(0);
         return;
         }
      if(asksendends[0])  {
         realaskendnum(0);
         return;
         }
      if(sendends[1]) {
         realsendendnum(1);
         return;
         }

      if(backchanged[0]&& realchangedback(0))
         return;
      if(askednums[1]) { 
         realgetnums(1);
         return;
         }
      if(sendends[0]) {
         realsendendnum(0); 
         return;
         }
      if(asksendends[1]) {
         realaskendnum(1); //Never happens
         return;
         }
//      nexttime=0L;
      setSending(false);
      }
    }


   /*
public void sync() {
   if(!usewatch)
      return;

   if(Natives.shouldsendlabels()) 
      sendlabels();
   {if(doLog) {Log.i(LOG_ID,"sync");};};

   getnums(0); 
   askendnum(0);
   changedback(1);

   sendendnum(1);

   changedback(0);
   getnums(1); 
   if(Natives.sendcuts()) 
      sendshortcuts(Natives.getShortcuts());
      
   } */


public IQMessageStatus sendstatus=null;
static private final long waittime=1000L;

public long statustime=0L,sendtime=0L;
private volatile long nexttime=0L;
public boolean realsendmessage(Object message) {
     if(mConnectIQ==null) {
        {if(doLog) {Log.d(LOG_ID,"realsendmessage mConnectIQ==null");};};
        return false;
      }
   if(devices==null) {
        {if(doLog) {Log.d(LOG_ID,"realsendmessage devices==null");};};
      return false;
      }
   {if(doLog) {Log.i(LOG_ID,"realsendmessage "+message);};};
         sendtime=System.currentTimeMillis();
//TODO: remove
//if(!isRelease) nexttime=sendtime+waittime;

      try {
          mConnectIQ.sendMessage(devices.get(devused), mMyApp, message, new IQSendMessageListener() {
         @Override
         public void onMessageStatus(IQDevice device, IQApp app, IQMessageStatus status) {
             String uit= "mConnectIQ.sendMessage:" + message + " " + status.name();
             log(uit);
             sendstatus=status;
             statustime=sendtime;
   //          if(status!= IQMessageStatus.SUCCESS) nexttime=0L;
             }
         });
         return true;
      } catch (InvalidStateException e) {
            Applic.Toaster( "ConnectIQ is not in a valid state");
      } catch (ServiceUnavailableException e) {
            Applic.Toaster( "ConnectIQ service is unavailable.   Is Garmin Connect Mobile installed and running?");
      }
          catch(       Throwable  error) {
         String mess="";
         if(error!=null)  {
            mess=error.getMessage();
            if(mess==null)
               mess="";
            }
            
             Log.stack(LOG_ID,mess,error);

         Applic.app.Toaster(mess);

             return false;
         }
      return false;
    }
public void sendshortcuts(ArrayList<ArrayList<Object>> shortcuts) {
   if(!usewatch)
      return;

    sendmessage(Arrays.asList(SHORTCUTS,shortcuts ));
   }
public void sendlabels() {
   if(!usewatch)
      return;
   List<String> labs= Applic.app.getlabels();
   int len=   labs.size()-1;
   if(len>0)
       sendmessage(Arrays.asList(PUTLABELS, labs.subList(0,len)));
   ArrayList<Float> precs   =new ArrayList<> ();
   precs.ensureCapacity(len);
   for(int i=0;i<len;i++)
      precs.add(Natives.getPrecision(i));
    sendmessage(Arrays.asList(PUTPRECISION, precs));
    }
/*
int getlastnum(int base) {
   long ptr=numio.numptrs[base];
   return Natives.getlastNum(ptr); 
   }
int getlastpollednum(int base) {
   long ptr=numio.numptrs[base];
   return Natives.getlastpolledNum(ptr); 
   }
   */
void setSending(boolean val) {
   if(val)
      nexttime=Long.MAX_VALUE;
   else
       nexttime=0L;
   }
boolean isSending() {   
      return System.currentTimeMillis()<nexttime;
    }
boolean[] askednums=new boolean[2];
private void getnums(int base) {
   if(!usewatch)
      return;
     if(didreceivebackup(base))
      return;
   if(isSending())  {
      {if(doLog) {Log.i(LOG_ID,"asknums "+base);};};
      askednums[base]=true;
      }
   else {
      setSending(true);
      realgetnums(base);
      }
    }

void realgetnums(int base) {
   {if(doLog) {Log.i(LOG_ID,"realgetnums "+base);};};
   askednums[base]=false;
   realsendmessage(Arrays.asList(NUMS, base, numio.getlastpollednum(base)));
   }
//void getnums() { addmessages(new Object[] { Arrays.asList(NUMS, 1, getlastnum(1)),Arrays.asList(NUMS, 0, getlastnum(0)) }); }


/*
    public enum IQDeviceStatus {
        NOT_PAIRED,
        NOT_CONNECTED,
        CONNECTED,
        UNKNOWN
    }

*/
public void stopalarm() {
   {if(doLog) {Log.i(LOG_ID,"send stopalarm");};};
    sendmessage(Arrays.asList(STOPALARM ));
    }
void      testapppresent() {
   sendmessage(Arrays.asList(START));
   }
    private IQDeviceEventListener mDeviceEventListener = new IQDeviceEventListener() {
       @Override
       public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
          {if(doLog) {Log.i(LOG_ID,"onDeviceStatusChanged("+device.toString()+","+status.toString()+")");};};
      if(status==IQDeviceStatus.CONNECTED) {
         setSending(false);
         if(wasglucose)
            backglucose();
         else {
            if(sendtime==0L) testapppresent();
            }
         }
      else {
         pushglucose();
         }
      for(IQDevice local:devices) {
          if (local.getDeviceIdentifier() == device.getDeviceIdentifier()) {
         synchronized(local)  {
            local.setStatus(status);
            }
         return;
          }
      }
       }


    };
public void loadDevices(Context context) {
   {if(doLog) {Log.i(LOG_ID,"loadDevices");};};
   // Avoid calling into ConnectIQ before the SDK is ready
   if(mConnectIQ==null || !sdkready()) {
      return;
   }
        // Retrieve the list of known devices
   try {
       long wasident=numio.getident();
       devices = mConnectIQ.getKnownDevices();
       if(devices!=null&&devices.size()>0) {
          for(int i=0;i<devices.size();i++) {
             IQDevice device= devices.get(i);
             long ident=device.getDeviceIdentifier();
             if(wasident==-1L||wasident==ident) {
                if(wasident==-1L)
                   numio.setident(ident);
                {if(doLog) {Log.d(LOG_ID,"registerForDeviceEvents");};};
                              mConnectIQ.registerForDeviceEvents(device, mDeviceEventListener);
                devused=i;
                break;
                }

             }
    /*                for (IQDevice device : devices) {
                        mConnectIQ.registerForDeviceEvents(device, mDeviceEventListener);
                    } */
          register(context);
          }
       else {
           {if(doLog) {Log.v(LOG_ID, "LoadDevices: devices==null");};};
           }
        } 
   catch (InvalidStateException e) {
      Log.stack(LOG_ID,"LoadDevices:  InvalidStateException ",e);
            // This generally means you forgot to call initialize(), but since
            // we are in the callback for initialize(), this should never happen
        } 
    catch (ServiceUnavailableException e) {
      Log.stack(LOG_ID,"LoadDevices: ServiceUnavailableException ", e);
            // This will happen if for some reason your app was not able to connect
            // to the ConnectIQ service running within Garmin Connect Mobile.  This
            // could be because Garmin Connect Mobile is not installed or needs to
            // be upgraded.
        }
    }
public boolean      usewatch=false;
public static int appmissing=0;
void register(Context context) {

   if(devices!=null&&devices.size()>0) {
       {if(doLog) {Log.v(LOG_ID, "register: devices.size()>0");};};
            try {

                mConnectIQ.getApplicationInfo(Natives.getgarminid(), devices.get(devused), new IQApplicationInfoListener() {

                    @Override
                    public void onApplicationInfoReceived(IQApp app) {
         appmissing=-1;
         {if(doLog) {Log.i(LOG_ID,"onApplicationInfoReceived");};};
                        try {
                            Applic.argToaster(getApplication(), "Opening  Kerfstok...", Toast.LENGTH_SHORT);
                            mConnectIQ.openApplication(devices.get(devused), app, mOpenAppListener);
                        } catch (Exception ex) {
            String mess=ex!=null?ex.getMessage():null;
            if(mess==null)
               mess="Exception during Opening app";
            Log.stack(LOG_ID,mess,ex);
                        }
                    }

                    @Override
                    public void onApplicationNotInstalled(String applicationId) {
                        //Kerfstok not installed on watch
         appmissing=1;
         {if(doLog) {Log.i(LOG_ID,"onApplicationNotInstalled");};};
         /*
         try {
                            Applic.argToaster(context, "Garmin watch app Kerfstok missing", Toast.LENGTH_SHORT);
            if(context instanceof Activity) {
               Activity act=(Activity)context;
            AlertDialog.Builder dialog = new AlertDialog.Builder(act);
            dialog.setTitle(R.string.kerfstok_missing_title);
            dialog.setMessage(R.string.kerfstok_missing_message);
            dialog.setPositiveButton(android.R.string.ok, null);
                           //TODO open https://apps.garmin.com/en-US/apps/128af1a3-1ce7-447f-8714-05c04043cfc5
            dialog.create().show();
            }
            }
         catch(Throwable ex) {

            String mess=ex!=null?ex.getMessage():null;
            if(mess==null)
               mess="Exception during missing app dialog";
            Log.stack(LOG_ID,mess,ex);
         }
         */
                    }

                });
            } catch (InvalidStateException e1) {
      Log.e(LOG_ID,"register:  InvalidStateException "+ e1.getMessage());

            } catch (ServiceUnavailableException e1) {
      Log.e(LOG_ID,"register: ServiceUnavailableException "+ e1.getMessage());
            }
            // Let's register to receive messages from our Applic.app on the device.
            try {
               {if(doLog) {Log.d(LOG_ID,"registerForAppEvents");};};
               mConnectIQ.registerForAppEvents(devices.get(devused), mMyApp, IQlistener);
               usewatch=true;
            } catch (InvalidStateException e) {
                Applic.argToaster(getApplication(), "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT);
            }
        }
   }

void unregister() {
   usewatch=false;
   if(mConnectIQ!=null) {
      try {
      if(devices!=null&&devices.size()>0) {
            mConnectIQ.unregisterAllForEvents();
           {if(doLog) {Log.d(LOG_ID,"unregisterAllForEvents");};};
         if (mMyApp != null) {
            {if(doLog) {Log.d(LOG_ID,"unregisterForApplicationEvents");};};
             mConnectIQ.unregisterForApplicationEvents(devices.get(devused), mMyApp);
         }
         }

          mConnectIQ.shutdown(getApplication());
      } catch (InvalidStateException e) {
      }
      }
   }

public void   onCleared() {
        try {
      numio.close();
        } catch (Exception ex) {
        }
   finally {
      unregister();
      }
   }
/*
void StartKerfstok() {
            try {
       if(devices!=null&&devices.size()>0)
      mConnectIQ.openApplication(devices.get(devused), mMyApp, mOpenAppListener);

            } catch (Exception ex) {
            }
//   return mAppIsOpen;
   }

 */
public boolean sdkready() {
   return mListener!=null&& mListener.sdkready();
   }   
MyConnectIQListener mListener=null; 
public void initIQ(Context context) {
try {
      mMyApp = new IQApp(Natives.getgarminid());
     mConnectIQ = ConnectIQ.getInstance(getApplication(), IQConnectType.WIRELESS);
{if(doLog) {Log.v(LOG_ID,"initIQ");};};
    mListener = new MyConnectIQListener(context);
    mConnectIQ.initialize(getApplication(), false, mListener);
    }
    catch(Throwable error) {
   String mess="";
   if(error!=null)  {
      mess=error.getMessage();
      if(mess==null)
         mess="";
      }
      
       Log.e(LOG_ID,"initIQ: "+mess);
       }

}
public void stop() {
     unregister();
     devices=null;
     mListener=null; 
     }

public void reinit(Context context) {
   stop();
   clearsyncgegs();
     /*
       mMyApp = new IQApp(Natives.getgarminid());
     mConnectIQ = ConnectIQ.getInstance(getApplication(), IQConnectType.WIRELESS);
    if(devices==null)
     */
        initIQ(context);
   
     }
}
