
#pragma once
#include <vector>
#include <time.h>
#include "SensorGlucoseData.hpp"
#include "nums/numdata.hpp"

  static constexpr const int glucosetype=0x40000000;//std::numeric_limits<int>::max();
  static constexpr const int nosearchtype=0x20000000;//std::numeric_limits<int>::max();
    static constexpr const int historysearchtype=2|glucosetype;
    static constexpr const int scansearchtype=1|glucosetype;
    static constexpr const int streamsearchtype=4|glucosetype;

struct  Searchgegs {
    int type;float under;float above;int frommin; int tomin;
    uint32_t count;
    float amount;
    std::vector<int> ingredients;
    bool operator()  (const Num *num) const {
        if(num->type>count)
            return false;
        if((type<0||num->type==type)) {
            float val=num->value;
            if(val>=under&&val<=above&&righttime(num->time)) {
                extern int carbotype;
                if(type==carbotype&&ingredients.size()>0) {
                    return (num->mealptr>0)&& meals->datameal()->matchmeals(ingredients,amount,num->mealptr);
                    }
                return  true;
                }
            }
        return false;
        }



    bool operator()  (const Num &num) const {
        return operator()(&num);
        }

    bool righttime(time_t tim)const {
        if(frommin<0&&tomin<0)
            return true;
        struct tm stm;
        localtime_r(&tim,&stm);
        int minutes=60*stm.tm_hour+ mktmmin(&stm);
        if(frommin<=tomin||tomin<0) {    
            if(minutes>=frommin&&(tomin<0||tomin>=minutes) ){
                return  true;
                }

            return false;
            }
        if(minutes<tomin||minutes>frommin)
            return true;
        return false;
        }
    bool operator() (const ScanData *g) const {
        if(!g||!g->valid())
            return false;
        uint32_t glu=g->g*10.0;
        if(g->t&&glu&&glu>=under&&glu<=above&&righttime(g->t)) {
            return  true;
            }
        return false;
        }
    bool operator() (const Glucose *g) const {
        if(!g||!g->valid())
            return false;
        uint32_t glu=g->getsputnik();

        if(glu>=under&&glu<=above&&righttime(g->gettime())) {
            return  true;
            }
        return false;
        }
};
extern Searchgegs searchdata;
