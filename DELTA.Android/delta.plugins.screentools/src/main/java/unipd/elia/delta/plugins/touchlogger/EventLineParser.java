package unipd.elia.delta.plugins.touchlogger;

/**
 * Created by Elia on 24/06/2015.
 */
public class EventLineParser {
    public enum EventType {ABS_MT_TRACKING_ID, ABS_MT_SLOT, SYN_REPORT, ABS_MT_POSITION_X, ABS_MT_POSITION_Y, ABS_MT_TOUCH_MAJOR, ABS_MT_PRESSURE, UNSUPPORTED}

    public EventType eventType;
    private String line;

    public static EventType getEventType(String line){
        if(line.contains("ABS_MT_TRACKING_ID"))
            return EventType.ABS_MT_TRACKING_ID;
        else if(line.contains("ABS_MT_SLOT"))
            return  EventType.ABS_MT_SLOT;
        else if(line.contains("SYN_REPORT"))
            return  EventType.SYN_REPORT;
        else if(line.contains("ABS_MT_POSITION_X"))
            return  EventType.ABS_MT_POSITION_X;
        else if(line.contains("ABS_MT_POSITION_Y"))
            return  EventType.ABS_MT_POSITION_Y;
        else if(line.contains("ABS_MT_TOUCH_MAJOR"))
            return  EventType.ABS_MT_TOUCH_MAJOR;
        else if(line.contains("ABS_MT_PRESSURE"))
            return  EventType.ABS_MT_PRESSURE;
        else return EventType.UNSUPPORTED;
    }


    public static String getTimestamp(String line){
        return line.substring(line.indexOf("[") + 1, line.indexOf("]")).trim();
    }

    public static double getTimestampNumber(String line){
        return Double.parseDouble(getTimestamp(line));
    }


    public static Long getEventValue(String line){
        String[] splitted = line.split("\\s+");
        return Long.decode("0x" + splitted[splitted.length - 1]);
    }

    public static String getEventStringValue(String line){
        String[] splitted = line.split("\\s+");
        return (splitted[splitted.length - 1]);
    }
}
