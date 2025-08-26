
algtype(getAlgorithmContextFromNative) vers(getAlgorithmContextFromNative);
algtype(initAlgorithmContext) vers(initAlgorithmContext);
algtype(processAlgorithmContext) vers(processAlgorithmContext);
algtype(getAlgorithmVersion) vers(getAlgorithmVersion);
algtype(releaseAlgorithmContext) vers(releaseAlgorithmContext);


static bool vers(getJNIfunctions)() {
    static    std::string_view alglib=jniAlglib;
    void *handle=openlib(alglib);
    if(!handle) {
        LOGGER("dlopen %s failed: %s\n",alglib.data(),dlerror());
        return false;
        }
{    constexpr const char str[]=algjavastr(getAlgorithmContextFromNative);
    vers(getAlgorithmContextFromNative)= (algtype(getAlgorithmContextFromNative)) dlsym(handle,str);
     if(!vers(getAlgorithmContextFromNative)) {
         LOGGER("dlsym %s failed: %s\n",str,dlerror());
        return false;
         }
      }
{    constexpr const char str[]=algjavastr(getAlgorithmVersion);
    vers(getAlgorithmVersion)= (algtype(getAlgorithmVersion)) dlsym(handle,str);
     if(!vers(getAlgorithmVersion)) {
         LOGGER("dlsym %s failed: %s\n",str,dlerror());
        return false;
         }
}
{    constexpr const char str[]=algjavastr(initAlgorithmContext);
    vers(initAlgorithmContext)= (algtype(initAlgorithmContext)) dlsym(handle,str);
     if(!vers(initAlgorithmContext)) {
         LOGGER("dlsym %s failed: %s\n",str,dlerror());
        return false;
         }
      }
{    constexpr const char str[]=algjavastr(processAlgorithmContext);
    vers(processAlgorithmContext)= (algtype(processAlgorithmContext)) dlsym(handle,str);
     if(!vers(processAlgorithmContext)) {
         LOGGER("dlsym %s failed: %s\n",str,dlerror());
        return false;
         }
}
{    constexpr const char str[]=algjavastr(releaseAlgorithmContext);
    vers(releaseAlgorithmContext)= (algtype(releaseAlgorithmContext)) dlsym(handle,str);
     if(!vers(releaseAlgorithmContext)) {
         LOGGER("dlsym %s failed: %s\n",str,dlerror());
        return false;
         }
}

    typedef   jint (*OnLoadtype)(JavaVM* vm, void* reserved) ;
    constexpr const char onloadname[]="JNI_OnLoad";
     OnLoadtype OnLoad= (OnLoadtype)dlsym(handle, onloadname);
     if(!OnLoad) {
        LOGGER("dlsym %s failed\n",onloadname);
        return false;
        }
    LOGSTRING("found OnLoad\n");

    OnLoad(getnewvm(),nullptr);
    LOGAR("after OnLoad");
    return true;
    }

double SiContext::vers(process)(int index,double value, double temp) {
     const auto res= vers(processAlgorithmContext)(subenv,nullptr,reinterpret_cast<jobject>(algcontext),index,value,temp,0.0,targetlow,targethigh);
     LOGGER("processAlgorithmContext%d(%p,%d,%f,%f,%f,%f,%f)=%f\n",vers(0),algcontext,index,value,temp,0.0,targetlow,targethigh,res);
     return res;
    };

getjson_t vers(getjson);
setjson_t vers(setjson);
#ifdef TEST
AlgorithmContext *vers(initAlgorithm)(const char *shortname) {
    char *version = (char *)vers(getAlgorithmVersion)(subenv,nullptr);
    LOGGER("getAlgorithmVersion()=%s\n",version);
    jobject jalg= vers(getAlgorithmContextFromNative)(subenv,nullptr);
    version = (char *)vers(getAlgorithmVersion)(subenv,nullptr);
    LOGGER("getAlgorithmVersion()=%s  algcontext=%p\n",version,jalg);
    int res = vers(initAlgorithmContext)(subenv,nullptr,jalg, 0, reinterpret_cast<jstring>((char *)shortname));
    LOGGER("initAlgorithmContext%d(...%s)=%d\n",vers(0),shortname,res);
    if(res != 1 ) {
        LOGGER("initAlgorithmContext(algcontext,0,%s)==%d\n",shortname,res);
        return nullptr;
    }
    auto algcontext=reinterpret_cast<AlgorithmContext *>(jalg);
    // loadjson(sens, sens->vers(statefile).data(),algcontext,setjson); 
     return algcontext;
     }
#else
AlgorithmContext *vers(initAlgorithm)(SensorGlucoseData *sens, setjson_t setjson) {
    jobject jalg= vers(getAlgorithmContextFromNative)(subenv,nullptr);
    char *shortname=sens->getinfo()->siBlueToothNum;
    int res = vers(initAlgorithmContext)(subenv,nullptr,jalg, 0, reinterpret_cast<jstring>(shortname));
    LOGGER("initAlgorithmContext%d(...%s)=%d\n",vers(0),shortname,res);
    if(res != 1) {
        const char *shortname;
        if(sens->notchinese()) {
            const int sub=sens->siSubtype();
            if(sub==1) {
                 shortname="YMWD016F";
                 }
            else {
                if(sub==3) {
                    shortname="0316015A";
                    }
                else {
                    shortname="YEZ1450H";
                    }
                  }
            }
        else  {
            shortname="GEPD802J";
            }
        res = vers(initAlgorithmContext)(subenv,nullptr,jalg, 0, reinterpret_cast<jstring>(const_cast<char *>(shortname)));
       LOGGER("initAlgorithmContext%d(...%s)=%d\n",vers(0),shortname,res);
        if(res!=1) {
            LOGGER("initAlgorithmContext(algcontext,0,%s)==%d\n",shortname,res);
            return nullptr;
            }
       }
    auto algcontext=reinterpret_cast<AlgorithmContext *>(jalg);
//     loadjson(sens, sens->vers(statefile).data(),algcontext,setjson); 
     loadjson(sens, sens->statefile.data(),algcontext,setjson); 
     return algcontext;
     }
#endif
static bool vers(getNativefunctions)() {
    std::string_view alglib=algLibName;
    void *handle=openlib(alglib);
    if(!handle) {
        LOGGER("dlopen %s failed: %s\n",alglib.data(),dlerror());
        return false;
        }
     const char *getjsonstr=jsonname(get,Ev);
    vers(getjson)= (getjson_t)dlsym(handle,getjsonstr);
     if(!vers(getjson)) {
         LOGGER("dlsym %s failed\n",getjsonstr);
        return false;
         }
     const char *setjsonstr=jsonname(set,EPc);
    vers(setjson)= (setjson_t)dlsym(handle,setjsonstr);
     if(!vers(setjson)) {
         LOGGER("dlsym %s failed\n",setjsonstr);
        return false;
         }
     LOGAR("found Nativefunctions");
     return true;
    }



