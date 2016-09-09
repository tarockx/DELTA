package unipd.elia.delta.plugins.touchlogger;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.rootcommands.RootCommands;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.command.Command;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.Logger;
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
@DeltaPluginMetadata(PluginName = "Touch Screen logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Records all the user's touches on the screen.",
        DeveloperDescription = "This plugin records all the user's interactions with the device's touchscreen (taps, swipes, etc.) " +
                "including coordinates, timings and pressure (if available). " +
                "NOTE: due to security measures introduced in Android 4.2+ this plugin requires a rooted device.",
        RequiresRoot = true)
public class TouchLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    Shell loggingShell;
    GetEventCommand loggingCommand;

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

        if(RootCommands.rootAccessGiven()){
            try {
                Shell shell = Shell.startRootShell();
                SimpleCommand listCommand = new SimpleCommand("getevent -lp");
                shell.add(listCommand).waitForFinish();
                String output = listCommand.getOutput();
                if(listCommand.getExitCode() != 0 || output == null || output.isEmpty())
                    throw new PluginFailedToInitializeException(this, "Could not acquire statistics about input devices: getevent command failed");

                String chosenDevice = null;
                try {
                    for (String line : output.split("\\r?\\n")) {
                        if (line.contains("add device")) {
                            String currentDevice = line.split(":")[1].trim();
                            SimpleCommand deviceListCommand = new SimpleCommand("getevent -lp " + currentDevice);
                            shell.add(deviceListCommand).waitForFinish();

                            if (deviceListCommand.getExitCode() == 0) {
                                boolean hasSlotCommand = false, hasTrackIDCommand = false;
                                for (String devLine : deviceListCommand.getOutput().split("\\r?\\n")) {
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
            } catch (Exception e) {
                e.printStackTrace();
                throw new PluginFailedToInitializeException(this, "Could not obtain root shell");
            }
        }else {
            throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
        }

    }

    @Override
    public void Terminate() {

    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        if(RootCommands.rootAccessGiven()){
            try {
                loggingShell = Shell.startRootShell();
                GetEventCommand getEventCommand = new GetEventCommand("getevent -tl " + deviceToLog);
                loggingShell.add(getEventCommand);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
                    currentSlot = EventLineParser.getEventValue(line); //current slot has changed
                    if(!slot2currentEventInfo.containsKey(currentSlot)) {
                        slot2currentEventInfo.put(currentSlot, new TouchEventInfo());
                    }
                    break;
                case ABS_MT_TRACKING_ID:
                    if(EventLineParser.getEventStringValue(line).equals("ffffffff")) { //finger up
                        reportFingerUp(currentSlot, EventLineParser.getTimestamp(line));
                        slot2currentEventInfo.remove(currentSlot);
                        //slot2trackingID.remove(currentSlot);
                    } else {
                        if (slot2currentEventInfo.isEmpty()) { //we just started logging, no associations yet
                            currentSlot = 0l;
                            slot2currentEventInfo.put(currentSlot, new TouchEventInfo());
                        }
                        reportFingerDown(currentSlot, EventLineParser.getTimestamp(line)); //new tracking ID
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
        try {
            Shell shell = Shell.startRootShell();
            Toolbox toolbox = new Toolbox(shell);
            if(toolbox.isProcessRunning("getevent")){
                toolbox.killAll("getevent");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(loggingCommand != null){
            loggingCommand.terminate("Experiment stopping");
        }

        if(loggingShell != null)
            try {
                loggingShell.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private class GetEventCommand extends Command {

        public GetEventCommand(String command) {
            super(command);
        }

        @Override
        public void output(int id, String line) {
            //parseInputSmart(line);
            parseInput(line);
        }

        @Override
        public void afterExecution(int id, int exitCode) {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "GetEvent finished!");
        }

    }
}
