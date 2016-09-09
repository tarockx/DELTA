package unipd.elia.delta.plugins.touchlogger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Elia on 07/07/2015.
 */
public class SmartTouchEventInfo {
    public List<Long> X = new LinkedList<>();
    public List<Long> X_ts = new LinkedList<>();
    public List<Long> Y = new LinkedList<>();
    public List<Long> Y_ts = new LinkedList<>();
    public List<Long> pressure = new LinkedList<>();
    public List<Long> pressure_ts = new LinkedList<>();
    public List<Long> size = new LinkedList<>();
    public List<Long> size_ts = new LinkedList<>();

    public long event_start_ts;
    public long event_end_ts;

    public SmartTouchEventInfo(){
        event_start_ts = System.currentTimeMillis();
    }

    public void fingerUp(){
        event_end_ts = System.currentTimeMillis();
    }

    public void setY(long y){
        Y.add(y);
        Y_ts.add(System.currentTimeMillis());
    }

    public void setX(long x){
        X.add(x);
        X_ts.add(System.currentTimeMillis());
    }

    public void setSize(long s){
        size.add(s);
        size_ts.add(System.currentTimeMillis());
    }

    public void setPressure(long p){
        pressure.add(p);
        pressure_ts.add(System.currentTimeMillis());
    }
}
