package delta.desktoptools.library.logtools;

import au.com.bytecode.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by Elia on 15/06/2015.
 */
public class LogWriter {
    private char separator;
    private List<LogFileEntry> logFileEntries;

    public LogWriter(List<LogFileEntry> logEntries, char separatorCharacter){
        logFileEntries = new LinkedList<>();
        if(logEntries != null)
            logFileEntries.addAll(logEntries);
        separator = separatorCharacter;
    }

    public LogWriter(LogFileEntry logEntry, char separatorCharacter){
        this(toEntries(logEntry), separatorCharacter);
    }

    private static List<LogFileEntry> toEntries(LogFileEntry logEntry){
        LinkedList<LogFileEntry> logFileEntries = new LinkedList<>();
        logFileEntries.add(logEntry);
        return logFileEntries;
    }

    public boolean exportBinaries(File outputDirectory, boolean merge){
        try {
            if(logFileEntries.size() == 0) //nothing to write
                return true;

            Map<String, FileOutputStream> myOutputStreams = new HashMap<>();
            for(LogFileEntry logFileEntry : logFileEntries) {
                if (!logFileEntry.isLoaded())
                    logFileEntry.reload();
            }

            //LogFileEntry firstEntry = logFileEntries.get(0);
            //LogFileEntry lastEntry = logFileEntries.get(logFileEntries.size() - 1);
            long firstTimestamp = logFileEntries.get(0).firstTimestamp;
            long lastTimestamp = logFileEntries.get(logFileEntries.size() - 1).lastTimestamp;

            //outputting all entries
            for(LogFileEntry logFileEntry : logFileEntries) {
                //writing all logs for each entry
                for (String rawFileName : logFileEntry.availableRawFiles) {
                    if(!merge){
                        firstTimestamp = logFileEntry.rawFilesStartTimes.get(rawFileName);
                        lastTimestamp = logFileEntry.rawFilesEndTimes.get(rawFileName);
                    }

                    //create filename and stream if needed
                    if(!merge || !myOutputStreams.containsKey(rawFileName)) {
                        String fileName = logFileEntry.experimentID + "_" +
                                logFileEntry.userID + "_" +
                                firstTimestamp + "_to_" + lastTimestamp + "_" +
                                rawFileName;
                        File rawFile = new File(outputDirectory, fileName);
                        FileOutputStream fileOutputStream = new FileOutputStream(rawFile);
                        myOutputStreams.put(rawFileName, fileOutputStream);
                    }

                    //write data
                    byte[] rawData = logFileEntry.readRawEntry(rawFileName);
                    myOutputStreams.get(rawFileName).write(rawData);

                    if(!merge){
                        myOutputStreams.get(rawFileName).close();
                        myOutputStreams.remove(rawFileName);
                    }
                }
            }

            for(FileOutputStream writer : myOutputStreams.values())
                writer.close();

        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean exportMergeOnly(File outputDirectory){
        try {
            if(logFileEntries.size() == 0) //nothing to write
                return true;

            Map<String, CSVWriter> myOutputStreams = new HashMap<>();
            for(LogFileEntry logFileEntry : logFileEntries) {
                if (!logFileEntry.isLoaded())
                    logFileEntry.reload();
            }

            long firstTimestamp = logFileEntries.get(0).firstTimestamp;
            long lastTimestamp = logFileEntries.get(logFileEntries.size() - 1).lastTimestamp;
            long experimentStartTime = logFileEntries.get(0).experimentStartTime;

            //outputting all entries
            for(LogFileEntry logFileEntry : logFileEntries){
                //writing all logs for each entry
                for(String pluginLog : logFileEntry.availableLogs){
                    boolean writeHeader = false;

                    //create file and outpustream if necessary
                    if(!myOutputStreams.containsKey(pluginLog)){
                        String fileName = logFileEntry.experimentID + "_" +
                                logFileEntry.userID + "_" +
                                "(ExperimentStartedAt " + experimentStartTime + ")_" +
                                firstTimestamp + "_to_" + lastTimestamp + "_" +
                                pluginLog.substring(0, pluginLog.length() - 4) + ".csv";
                        File csvFile = new File(outputDirectory, fileName);
                        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false)), separator);
                        myOutputStreams.put(pluginLog, csvWriter);
                        writeHeader = true;
                    }

                    //write
                    String entryString = logFileEntry.readEntry(pluginLog);
                    String lines[] = entryString.split("\\r?\\n");
                    writeToStream(myOutputStreams.get(pluginLog), lines, writeHeader);
                }
            }

            //close streams
            for(CSVWriter writer : myOutputStreams.values())
                writer.close();
            return true;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public boolean exportCSVs(File outputDirectory){
        try {
            if(logFileEntries.size() == 0) //nothing to write
                return true;

            Map<String, CSVWriter> myOutputStreams = new HashMap<>();
            for(LogFileEntry logFileEntry : logFileEntries) {
                if (!logFileEntry.isLoaded())
                    logFileEntry.reload();
            }

            long firstTimestamp = logFileEntries.get(0).firstTimestamp;
            long lastTimestamp = logFileEntries.get(logFileEntries.size() - 1).lastTimestamp;


            //outputting all entries
            for(LogFileEntry logFileEntry : logFileEntries){
                //writing all logs for each entry
                for(String pluginLog : logFileEntry.availableLogs){
                    boolean writeHeader = false;

                    //create file and outpustream if necessary
                    if(!myOutputStreams.containsKey(pluginLog)){
                        String fileName = logFileEntry.experimentID + "_" +
                                logFileEntry.userID + "_" +
                                firstTimestamp + "_to_" + lastTimestamp + "_" +
                                pluginLog.substring(0, pluginLog.length() - 4) + ".csv";
                        File csvFile = new File(outputDirectory, fileName);
                        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false)), separator);
                        myOutputStreams.put(pluginLog, csvWriter);
                        writeHeader = true;
                    }

                    //write
                    String entryString = logFileEntry.readEntry(pluginLog);
                    String lines[] = entryString.split("\\r?\\n");
                    writeToStream(myOutputStreams.get(pluginLog), lines, writeHeader);
                }
            }

            //close streams
            for(CSVWriter writer : myOutputStreams.values())
                writer.close();
            return true;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    private boolean writeToStream(CSVWriter csvWriter, String[] lines, boolean writeHeader){
        try{

            List<String> jsonKeys = new LinkedList<>();
            boolean headerWritten = false;

            for(String jsonString : lines){
                if(jsonString.trim().isEmpty())
                    continue;

                JSONObject jsonWrapper = new JSONObject(jsonString.trim());

                Long timestamp = jsonWrapper.getLong("timestamp");
                Object jsonData = jsonWrapper.get("data");
                JSONObject jsonObject = null;
                JSONArray jsonArray = null;
                if(JSONObject.class.isInstance(jsonData))
                    jsonObject =(JSONObject)jsonData;
                else if(JSONArray.class.isInstance(jsonData))
                    jsonArray = (JSONArray)jsonData;

                //initialize keylist if first run
                if(jsonKeys.size() == 0){
                    jsonKeys.add("timestamp");

                    if(jsonObject != null){
                        for(Object key : jsonObject.keySet()){
                            jsonKeys.add(key.toString());
                        }
                    }
                    else if(jsonArray != null)
                        jsonKeys.add("data");

                }

                //build CSV record
                String[] csvRecord = new String[jsonKeys.size()];
                csvRecord[0] = Long.toString(timestamp);

                if(jsonObject != null){
                    for(int i = 1; i < jsonKeys.size(); i++){
                        csvRecord[i] = jsonObject.get(jsonKeys.get(i)).toString();
                    }
                }
                else if(jsonArray != null){
                    csvRecord[1] = jsonArray.toString();
                }


                //write header if necessary
                if(writeHeader && !headerWritten){
                    String[] header = jsonKeys.toArray(new String[0]);
                    csvWriter.writeNext(header);
                    headerWritten = true;
                }

                //write to file
                csvWriter.writeNext(csvRecord);
            }
            csvWriter.flush();
            return true;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }
}
