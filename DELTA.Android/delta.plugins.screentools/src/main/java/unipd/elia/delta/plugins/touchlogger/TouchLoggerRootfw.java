package unipd.elia.delta.plugins.touchlogger;

import android.content.Context;

import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.ShellStreamer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 18/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Touch Screen logger (rootfw variant)",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Records all the user's touches on the screen.",
        DeveloperDescription = "This plugin records all the user's interactions with the device's touchscreen (taps, swipes, etc.) " +
                "including coordinates, timings and pressure (if available). " +
                "NOTE: due to security measures introduced in Android 4.2+ this plugin requires a rooted device.",
        RequiresRoot = true)
public class TouchLoggerRootfw implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    ShellStreamer myShellStreamer;
    String pluginName;

    String deviceToLog;

    //current data on logging events
    //Map<Long, Long> slot2trackingID;
    Map<Long, TouchEventInfo> slot2currentEventInfo;
    Map<Long, SmartTouchEventInfo> slot2currentSmartEventInfo;
    Long currentSlot;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        slot2currentEventInfo = new HashMap<>();
        slot2currentSmartEventInfo = new HashMap<>();
        //slot2trackingID = new HashMap<>();

        //assume first slot is 0 (i hope this is correct...)
        currentSlot = -1l;
        //slot2currentEventInfo.put(currentSlot, new TouchEventInfo());

        Shell rootShell = new Shell(true);
        Shell.Result obtainedSu = rootShell.execute("id");
        if(obtainedSu == null || !obtainedSu.wasSuccessful() || !obtainedSu.getLine().contains("uid=0")){
            //retry
            obtainedSu = rootShell.execute("id");
            if(obtainedSu == null || !obtainedSu.wasSuccessful() || !obtainedSu.getLine().contains("uid=0"))
                throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
        }


        //fails if we don't have root access
        if(!rootShell.isConnected())
            throw new PluginFailedToInitializeException(this, "Could not connect with the root shell");

        //acquire information about input devices and find the device's TouchScreen
        Shell.Result getEventStatsResult = rootShell.execute("getevent -lp");
        if(getEventStatsResult == null || !getEventStatsResult.wasSuccessful())
            throw new PluginFailedToInitializeException(this, "Could not acquire statistics about input devices: getevent command failed");


        String chosenDevice = null;
        try {
            for (String line : getEventStatsResult.getArray()) {
                if (line.contains("add device")) {
                    String currentDevice = line.split(":")[1].trim();
                    Shell.Result getDeviceStatsResult = rootShell.execute("getevent -lp " + currentDevice);
                    if (getDeviceStatsResult != null && getDeviceStatsResult.wasSuccessful()) {
                        boolean hasSlotCommand = false, hasTrackIDCommand = false;
                        for (String devLine : getDeviceStatsResult.getArray()) {
                            if (devLine.contains("ABS_MT_SLOT"))
                                hasSlotCommand = true;
                            if (devLine.contains("ABS_MT_TRACKING_ID"))
                                hasTrackIDCommand = true;
                        }

                        if (hasSlotCommand && hasTrackIDCommand) {
                            chosenDevice = currentDevice;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, "Exception while parsing output from getevent: " + ex.getMessage());
        }

        if(chosenDevice == null)
            throw new PluginFailedToInitializeException(this, "Did not find a compatible TouchScreen driver on this device. You either don't have a touchscreen or it's unsupported");
        else
            deviceToLog = chosenDevice;

        //setup logging
        myShellStreamer = new ShellStreamer();
        if(!myShellStreamer.connect(true)) //connect as root
            throw new PluginFailedToInitializeException(this, "Could not initialize ShellStreamer");
    }

    @Override
    public void Terminate() {
        if(myShellStreamer != null)
            myShellStreamer.disconnect();
        myShellStreamer = null;
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        if(myShellStreamer == null || !myShellStreamer.isConnected())
            throw new PluginFailedToStartLoggingException(this, "Could not connect to root shell");

        if(deviceToLog == null)
            throw new PluginFailedToStartLoggingException(this, "No device to log selected");

        myShellStreamer.startStream(new ShellStreamer.StreamListener(){
            @Override
            public void onStreamStart(ShellStreamer shell) {
                shell.writeLine("getevent -tl " + deviceToLog);

                // Send a stop signal once the above command is finished
                shell.stopStream();
            }

            @Override
            public void onStreamInput(ShellStreamer shell, String outputLine) {
                // New line has been received from the shell
                //parseInput(outputLine);
                parseInputSmart(outputLine);
            }

            @Override
            public void onStreamStop(ShellStreamer shell, int resultCode) {
                shell.disconnect();
            }
        });
    }

    private void parseInput(String line){
        try{
            EventLineParser.EventType eventType = EventLineParser.getEventType(line);
            if(currentSlot == -1l && !(eventType == EventLineParser.EventType.ABS_MT_SLOT || eventType == EventLineParser.EventType.ABS_MT_TRACKING_ID))
                return;

            switch (eventType){
                case UNSUPPORTED:
                    return;
                case SYN_REPORT:
                    reportTouches(EventLineParser.getTimestamp(line));
                    break;
                case ABS_MT_SLOT:
                    currentSlot = EventLineParser.getEventValue(line);
                    if(!slot2currentEventInfo.containsKey(currentSlot)) {
                        slot2currentEventInfo.put(currentSlot, new TouchEventInfo());
                        //reportFingerDown(currentSlot, EventLineParser.getTimestamp(line));
                    }
                    break;
                case ABS_MT_TRACKING_ID:
                    if(EventLineParser.getEventStringValue(line).equals("ffffffff")) { //finger up
                        reportFingerUp(currentSlot, EventLineParser.getTimestamp(line));
                        slot2currentEventInfo.remove(currentSlot);
                        //slot2trackingID.remove(currentSlot);
                    } else {
                        if (slot2currentEventInfo.isEmpty()) {
                            currentSlot = 0l;
                            slot2currentEventInfo.put(currentSlot, new TouchEventInfo());
                        }
                        reportFingerDown(currentSlot, EventLineParser.getTimestamp(line));
                    }
                    break;
                case ABS_MT_POSITION_X:
                    slot2currentEventInfo.get(currentSlot).setX(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_POSITION_Y:
                    slot2currentEventInfo.get(currentSlot).setY(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_TOUCH_MAJOR:
                    slot2currentEventInfo.get(currentSlot).setSize(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_PRESSURE:
                    slot2currentEventInfo.get(currentSlot).setPressure(EventLineParser.getEventValue(line));
                    break;
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }


    }

    private void parseInputSmart(String line){
        try{
            EventLineParser.EventType eventType = EventLineParser.getEventType(line);
            if(currentSlot == -1l && !(eventType == EventLineParser.EventType.ABS_MT_SLOT || eventType == EventLineParser.EventType.ABS_MT_TRACKING_ID))
                return;

            switch (eventType){
                case UNSUPPORTED:
                    return;
                case SYN_REPORT:
                    break;
                case ABS_MT_SLOT:
                    currentSlot = EventLineParser.getEventValue(line);
                    if(!slot2currentSmartEventInfo.containsKey(currentSlot)) {
                        slot2currentSmartEventInfo.put(currentSlot, new SmartTouchEventInfo());
                        //reportFingerDown(currentSlot, EventLineParser.getTimestamp(line));
                    }
                    break;
                case ABS_MT_TRACKING_ID:
                    if(EventLineParser.getEventStringValue(line).equals("ffffffff")) { //finger up
                        SmartTouchEventInfo s = slot2currentSmartEventInfo.remove(currentSlot);
                        s.fingerUp();
                        reportEvent(s);
                        //slot2trackingID.remove(currentSlot);
                    } else {
                        if (slot2currentSmartEventInfo.isEmpty()) {
                            currentSlot = 0l;
                            slot2currentSmartEventInfo.put(currentSlot, new SmartTouchEventInfo());
                        }
                    }
                    break;
                case ABS_MT_POSITION_X:
                    slot2currentSmartEventInfo.get(currentSlot).setX(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_POSITION_Y:
                    slot2currentSmartEventInfo.get(currentSlot).setY(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_TOUCH_MAJOR:
                    slot2currentSmartEventInfo.get(currentSlot).setSize(EventLineParser.getEventValue(line));
                    break;
                case ABS_MT_PRESSURE:
                    slot2currentSmartEventInfo.get(currentSlot).setPressure(EventLineParser.getEventValue(line));
                    break;
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void reportEvent(SmartTouchEventInfo s){
        JSONObject obj = new JSONObject();
        try {
            obj.put("finger_down_ts", s.event_start_ts);
            obj.put("finger_up_ts", s.event_end_ts);
            for(int i = 0; i < s.X.size(); i++) {
                obj.accumulate("X_moves", "{\"X\":\"" + s.X.get(i) +"\",\"ts\":\"" + s.X_ts.get(i) + "\"}");
            }
            for(int i = 0; i < s.Y.size(); i++) {
                obj.accumulate("Y_moves", "{\"Y\":\"" + s.Y.get(i) +"\",\"ts\":\"" + s.Y_ts.get(i) + "\"}");
            }
            for(int i = 0; i < s.pressure.size(); i++) {
                obj.accumulate("pressure_changes", "{\"pressure\":\"" + s.pressure.get(i) +"\",\"ts\":\"" + s.pressure_ts.get(i) + "\"}");
            }
            for(int i = 0; i < s.size.size(); i++) {
                obj.accumulate("size_changes", "{\"size\":\"" + s.size.get(i) +"\",\"ts\":\"" + s.size_ts.get(i) + "\"}");
            }

            DeltaDataEntry data = new DeltaDataEntry(s.event_start_ts, pluginName, obj);
            myDeltaMaster.Update(data);
        }
        catch (JSONException ex){
            ex.printStackTrace();
        }
    }

    private void reportFingerDown(Long slot, String timestamp) {
        DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), "{\"finger\":\"" + slot + "\"," +
                "\"ts\":\"" + timestamp + "\"," +
                "\"event\":\"finger_down\"," +
                "\"X\":\"" + "" +"\"," +
                "\"Y\":\"" + "" +"\"," +
                "\"size\":\"" + "" +"\"," +
                "\"pressure\":\"" + "" +"\"}");
        myDeltaMaster.Update(data);
    }

    private void reportFingerUp(Long slot, String timestamp) {
        DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), "{\"finger\":\"" + slot + "\"," +
                "\"ts\":\"" + timestamp + "\"," +
                "\"event\":\"finger_up\"," +
                "\"X\":\"" + "" +"\"," +
                "\"Y\":\"" + "" +"\"," +
                "\"size\":\"" + "" +"\"," +
                "\"pressure\":\"" + "" +"\"}");
        myDeltaMaster.Update(data);
    }

    private void reportTouches(String timestamp) {
        long ts = System.currentTimeMillis();
        for(Long slot : slot2currentEventInfo.keySet()){
            TouchEventInfo event = slot2currentEventInfo.get(slot);
            if(event.isDirty){
                DeltaDataEntry data = new DeltaDataEntry(ts, getClass().getName(), "{\"finger\":\"" + slot + "\"," +
                        "\"ts\":\"" + timestamp + "\"," +
                        "\"event\":\"position_update\"," +
                        "\"X\":\"" + event.X +"\"," +
                        "\"Y\":\"" + event.Y +"\"," +
                        "\"size\":\"" + event.size +"\"," +
                        "\"pressure\":\"" + event.pressure +"\"}");
                myDeltaMaster.Update(data);
                event.isDirty = false;
            }
        }
    }


    @Override
    public void StopLogging() {
        if(myShellStreamer != null)
            myShellStreamer.stopStream();
    }

}
