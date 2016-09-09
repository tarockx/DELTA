package unipd.delta.plugins.messagingtools;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 08/07/2015.
 */
@DeltaPluginMetadata(PluginName = "SMS/MMS logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs incoming and outgoing SMS/MMS messages. Logged information includes the time each message was received/sent, the length of the message " +
                "and a hashed value identifying the phone number of the sender/recipient. NOTE: the actual content of the message and the actual phone number of the " +
                "sender/recipient are NOT logged.",
        DeveloperDescription = "A log entry is added every time that a new text message is sent or received.")
public class SMSLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    ContentObserver mySMSContentObserver;
    Set<String> ids;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();
        ids = new HashSet<>();

        try {
            mySMSContentObserver = new ContentObserver(new android.os.Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);

                    logSentMessage();
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void logSentMessage() {
        try {
            Set<String> scanResults = performIdScan();
            Set<String> scanResultsClone = new HashSet<>(scanResults);
            scanResultsClone.removeAll(ids);

            ids = scanResults;
            if(scanResultsClone.size() == 0)
                return; //item deleted or modified, we don't log

            String affectedId = scanResultsClone.iterator().next();

            Uri uriSMSURI = Uri.parse("content://sms");
            Cursor cur = context.getContentResolver().query(uriSMSURI, null, null, null, null);

            while (cur.moveToNext()){
                if(cur.getString(cur.getColumnIndex("_id")).equals(affectedId)){
                    JSONObject obj = new JSONObject();
                    obj.put("type", cur.getString(cur.getColumnIndex("type")).equals("1") ? "incoming" : "sent");

                    String[] columnNames = cur.getColumnNames();
                    for(String column : columnNames){
                        if(!column.equals("type"))
                            obj.put(column, cur.getString(cur.getColumnIndex(column)));
                    }

                    cur.close();

                    DeltaDataEntry dataEntry = new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj);
                    myDeltaMaster.Update(dataEntry);
                }
            }

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }


    @Override
    public void Terminate() {
    }

    private Set<String> performIdScan() {
        Set<String> scannedIDs = new HashSet<>();

        try {
            Uri uriSMSURI = Uri.parse("content://sms");
            Cursor cur = context.getContentResolver().query(uriSMSURI, null, null, null, null);
            while (cur.moveToNext()){
                int type =  cur.getInt(cur.getColumnIndex("type"));
                if(type == 1 || type == 2)
                    scannedIDs.add(cur.getString(cur.getColumnIndex("_id")));
            }
            cur.close();
        } catch (Exception ex){
            ex.printStackTrace();
        }

        return scannedIDs;
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            ids = performIdScan();

            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.registerContentObserver(Uri.parse("content://sms"), true, mySMSContentObserver);
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.unregisterContentObserver(mySMSContentObserver);
    }
}