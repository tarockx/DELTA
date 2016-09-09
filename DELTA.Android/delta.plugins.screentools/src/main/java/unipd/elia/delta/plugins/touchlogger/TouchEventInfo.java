package unipd.elia.delta.plugins.touchlogger;

/**
 * Created by Elia on 24/06/2015.
 */
public class TouchEventInfo {
    public long X;
    public long Y;
    public long pressure;
    public long size; //size of the fingertip/pentip

    public boolean isDirty;

    public void setX(long X){
        this.X = X;
        isDirty = true;
    }
    public void setY(long Y){
        this.Y = Y;
        isDirty = true;
    }
    public void setPressure(long pressure){
        this.pressure = pressure;
        isDirty = true;
    }
    public void setSize(long size){
        this.size = size;
        isDirty = true;
    }
}
