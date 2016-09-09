package unipd.elia.delta.logsubstrate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;

/**
 * Created by Elia on 31/07/2015.
 */
public class DeltaUpdateBufferedListener implements IDeltaPluginMaster {
    private final Object entryCacheLock = new Object();
    private List<DeltaDataEntry> entryCache = new LinkedList<>();
    private CountingOutputStream myCountingOutputStream = new CountingOutputStream();
    private Writer myWriter = new OutputStreamWriter(myCountingOutputStream);
    private String pluginID;
    private long cacheSize;

    public DeltaUpdateBufferedListener(String pluginID){
        this.pluginID = pluginID;
    }

    @Override
    public void Update(DeltaDataEntry data) {
        synchronized (entryCacheLock){
            entryCache.add(data);
        }
    }

    protected List<DeltaDataEntry> flushCache(){
        synchronized (entryCacheLock){
            List<DeltaDataEntry> temp = entryCache;
            entryCache = new LinkedList<>();
            return entryCache;
        }
    }

    private void updateSize(DeltaDataEntry entry){
        try {
            if(entry.rawMode)
                myCountingOutputStream.increase(entry.getSize());
            myWriter.flush();
        } catch (IOException e) {
            ;
        }
    }

    private class CountingOutputStream extends OutputStream {
        private int _total;

        @Override public void write(int b) {
            ++_total;
        }

        @Override public void write(byte[] b) {
            _total += b.length;
        }

        @Override public void write(byte[] b, int offset, int len) {
            _total += len;
        }

        public void increase(int amount){
            _total += amount;
        }

        public int getTotalSize(){
            return  _total;
        }

        public void resetTotalSize(){
            _total = 0;
        }
    }
}
