package delta.desktoptools.library;

import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;

import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Elia on 23/04/2015.
 */
public class IOHelpers {
    public static void deleteDirectoryRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteDirectoryRecursive(child);

        fileOrDirectory.delete();
    }

    public static List<File> getAllFilesInFolderByExtension(File directory, String extension, boolean searchSubfolders){
        LinkedList<File> files = new LinkedList<>();

        if(directory != null && directory.isDirectory()) {
            // get all the files from a directory
            File[] fList = directory.listFiles();
            if(fList != null) {
                for (File file : fList) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith("." + extension.toLowerCase())) {
                        files.add(file);
                    } else if (file.isDirectory() && searchSubfolders) {
                        files.addAll(getAllFilesInFolderByExtension(file, extension, searchSubfolders));
                    }
                }
            }
        }
        return files;
    }

    public static Map<String, List<File>> getAllAndroidProjects(String baseFolder){
        HashMap<String, List<File>> results = new HashMap<>();
        File directory = new File(baseFolder);
        if(directory.exists() && directory.isDirectory()){
            File[] fList = directory.listFiles();
            if(fList != null) {
                for (File file : fList) {
                    try {
                        if (file.isDirectory()) {
                            File buildfile = new File(file.getCanonicalPath(), "build.gradle");
                            if (buildfile.exists()) {
                                //is an Android project
                                String manifestPath = getAndroidManifestPath(file.getCanonicalPath());
                                if (manifestPath != null) {
                                    List<File> javaFiles = getAllFilesInFolderByExtension(file, "java", true);
                                    results.put(manifestPath, javaFiles);
                                }
                            }
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        }
        return results;
    }

    public static String getAndroidManifestPath(String baseFolder){
        File directory = new File(baseFolder);

        if(directory.exists()) {
            // get all the files from a directory
            File[] fList = directory.listFiles();
            if(fList != null) {
                for (File file : fList) {
                    try {
                        if (file.isFile() && file.getName().equals("AndroidManifest.xml")) {
                            return file.getCanonicalPath();
                        } else if (file.isDirectory()) {
                            String recPath = getAndroidManifestPath(file.getCanonicalPath());
                            if (recPath != null)
                                return recPath;
                        }
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static void buildExperimentAsync(final ExperimentConfiguration experimentConfiguration,
                                            final String projectRootPath,
                                            final String outputFilePath,
                                            final boolean debugBuild,
                                            @Nullable final SigningInfo signingInfo,
                                            @Nullable final String externalPluginsPath,
                                            @Nullable final ProgressReportListener reportListener
                                            ){
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                boolean success = buildExperiment(experimentConfiguration, projectRootPath, outputFilePath, debugBuild, signingInfo, externalPluginsPath, reportListener);
                if(reportListener != null)
                    reportListener.Finished(success);
            }
        });
        t1.start();
    }

    public static boolean buildExperiment(ExperimentConfiguration experimentConfiguration,
                                          String projectRootPath,
                                          String outputFilePath,
                                          boolean debugBuild,
                                          @Nullable SigningInfo signingInfo,
                                          @Nullable String externalPluginsPath,
                                          @Nullable ProgressReportListener reportListener){
        File outputDir = null;
        try {
            outputDir = Files.createTempDirectory("deltabuild").toFile();
            reportProgress("Temp build directory set at: " + outputDir.getCanonicalPath(), reportListener);

            File projectDir = new File(projectRootPath);
            File configOutputFile = new File(outputDir + "/configuration.xml");
            File apkOutFile = new File(outputDir + "/experiment.apk");
            File experimentOutFile = new File(outputFilePath);

            //check and cleanup
            if(experimentOutFile.exists() && !experimentOutFile.delete() ){
                throw new Exception("Cannot get write access to output file: " + experimentOutFile.getCanonicalPath());
            }
            if(!projectDir.exists() || !projectDir.isDirectory()) {
                throw new Exception("Main DELTA project dir [" + projectDir.getCanonicalPath() + "] doesn't exist or is inaccessible");
            }

            reportProgress("Serializing experiment to file: " + configOutputFile.getCanonicalPath(), reportListener);
            ExperimentConfigurationIOHelpers.SerializeExperimentConfiguration(experimentConfiguration, configOutputFile.getCanonicalPath());
            if(!configOutputFile.exists()) {
                throw new Exception("Failed to serialize experiment file! Aborting build process...");
            }
            else {
                reportProgress("Successfully serialized experiment configuration!", reportListener);
            }

            boolean isWin = System.getProperty("os.name").startsWith("Windows");

            List<String> arguments = new LinkedList<>();
            arguments.add(projectDir.getCanonicalPath() + "/" + (isWin ? "gradlew.bat" : "gradlew"));
            arguments.add(debugBuild ? "assembleDebug" : "assembleRelease");
            arguments.add("-bdelta.logsubstrate/build.gradle");
            arguments.add("-PexperimentConfigurationPath=" + configOutputFile);
            arguments.add("-PexperimentOutputPath=" + outputDir);
            if(signingInfo != null){
                File keySotreFile = new File(signingInfo.keystoreFilePath);
                if(!keySotreFile.exists())
                    throw new Exception("Could not find KeyStore file");

                arguments.add("-PkeystoreFile=" + keySotreFile.getCanonicalPath());
                arguments.add("-PkeystorePassword=" + signingInfo.keystorePassword);
                arguments.add("-PkeyAlias=" + signingInfo.keyAlias);
                arguments.add("-PkeyPassword=" + signingInfo.keyPassword);

                reportProgress("Signing information supplied, using keystore: [" + signingInfo.keystoreFilePath + "] and key: [" + signingInfo.keyAlias + "]", reportListener);
            }
            if(externalPluginsPath != null)
                arguments.add("-PexternalPluginsDir=" + externalPluginsPath);

            ProcessBuilder pb = new ProcessBuilder(
                    /*
                    projectDir.getCanonicalPath() + "/" + (isWin ? "gradlew.bat" : "gradlew"),
                    debugBuild ? "assembleDebug" : "assembleRelease",
                    "-bdelta.logsubstrate/build.gradle",
                    "-PexperimentConfigurationPath=" + configOutputFile,
                    "-PexperimentOutputPath=" + outputDir
                    */
                    arguments
            );

            pb.directory(projectDir);
            reportProgress("Running command line: " + pb.command(), reportListener);
            Process proc = pb.start();

            /*
            Runtime runtime = Runtime.getRuntime();

            String command = projectDir.getCanonicalPath() + "/" + (isWin ? "gradlew.bat" : "gradlew");
            command += debugBuild ? " assembleDebug" : " assembleRelease";
            command += " -b \"" + projectDir.getCanonicalPath() + "/delta.logsubstrate/build.gradle\"";
            command += " -PexperimentConfigurationPath=\"" + configOutputFile + "\"";
            command += " -PexperimentOutputPath=\"" + outputDir + "\"";

            reportProgress("Running command line: " + command, reportListener);
            Process proc = runtime.exec(command);
            */

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            if(reportListener != null){
                // read the output from the command
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    reportListener.ReportProgress(s);
                }

                // read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    reportListener.ReportError(s);
                }
            }

            int returnCode = proc.waitFor();
            if(returnCode != 0) {
                throw new Exception("Gradle build reported a build error.");
            }
            else {
                boolean result = packageExperiment(configOutputFile.getCanonicalPath(), apkOutFile.getCanonicalPath(), experimentOutFile.getCanonicalPath(), reportListener);
                cleanup(outputDir.getCanonicalPath());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportError(e.getMessage(), reportListener);
            try {
                cleanup(outputDir.getCanonicalPath());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            return false;
        }
    }

    private static void cleanup(String absolutePath) {
        deleteDirectoryRecursive(new File(absolutePath));
    }

    public static boolean packageExperiment(String experimentConfigurationFilePath, String experimentPackageFilePath, String outputFilePath, @Nullable ProgressReportListener reportListener){
        File experimentConfigurationFile = new File(experimentConfigurationFilePath);
        File experimentPackageFile = new File(experimentPackageFilePath);

        if(!experimentConfigurationFile.exists() || !experimentPackageFile.exists()){
            reportError("Couldn't find or access the experiment configuration file: " + experimentConfigurationFilePath, reportListener);
            return false;
        }
        if(!experimentPackageFile.exists()){
            reportError("Couldn't find or access the experiment package file: " + experimentPackageFilePath, reportListener);
            return false;
        }

        File outputFile = new File(outputFilePath);
        if(outputFile.exists() && !outputFile.delete()){
            reportError("I was unable to overwrite pre-existing experiment [ " + outputFilePath + "]. File is locked or write access is denied.", reportListener);
            return false;
        }

        reportProgress("Experiment files found, building DELTA experiment bundle: [" + outputFilePath + "]", reportListener);

        try{
            FileOutputStream fos = new FileOutputStream(outputFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            File[] entries = {experimentConfigurationFile, experimentPackageFile};

            for(File entry : entries){
                byte[] buffer = new byte[1024];
                ZipEntry ze= new ZipEntry(entry.getName());
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(entry.getCanonicalPath());

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
                zos.closeEntry();
            }
            //remember close it
            zos.close();

            reportProgress("DELTA Experiment bundle successfully compiled!", reportListener);
            return true;

        }catch(IOException ex){
            ex.printStackTrace();
            reportError(ex.getMessage(), reportListener);
            return false;
        }
    }

    public static boolean storeIncomingLogData(String deviceID, String experimentID, String filename, String data){
        byte[] binData = DatatypeConverter.parseBase64Binary(data);
        return storeIncomingLogData(deviceID, experimentID, filename, binData);
    }

    public static boolean storeIncomingLogData(String deviceID, String experimentID, String filename, byte[] data){
        File logsDir = new File(SettingsHelper.PATH_LOG_REPOSITORY);
        if(!logsDir.exists() && !logsDir.mkdirs())
            return false;

        File experimentLogsDir = new File(logsDir, experimentID);
        if(!experimentLogsDir.exists() && !experimentLogsDir.mkdirs())
            return false;

        File userLogsDirForExperiment = new File(experimentLogsDir, deviceID);
        if(!userLogsDirForExperiment.exists() && !userLogsDirForExperiment.mkdirs())
            return false;

        File out = new File(userLogsDirForExperiment, filename);
        try {
            FileOutputStream stream = new FileOutputStream(out);
            stream.write(data);
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void reportProgress(String what, ProgressReportListener listener){
        if(listener != null)
            listener.ReportProgress(what);
    }

    private static void reportError(String what, ProgressReportListener listener){
        if(listener != null)
            listener.ReportError(what);
    }

}
