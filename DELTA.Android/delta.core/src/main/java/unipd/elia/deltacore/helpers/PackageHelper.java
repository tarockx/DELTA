package unipd.elia.deltacore.helpers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.ExperimentStoreEntry;

/**
 * Created by Elia on 15/05/2015.
 */
public class PackageHelper {
    public static boolean isApkInstalled(PackageManager pm, String uri) {
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static boolean isApkCompatible(PackageManager pm, String apkPath){
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkPath, 0);
        if(packageInfo == null)
            return false;

        return  (packageInfo.applicationInfo.targetSdkVersion <= Build.VERSION.SDK_INT);
    }

    public static boolean isExperimentInStore(String experimentPackage){
        File dirToCheck = new File(IOHelpers.getExperimentsStoragePath(), experimentPackage);
        if(dirToCheck.exists() && dirToCheck.isDirectory()){
            File configFile = new File(dirToCheck, "configuration.xml");
            File apkFile = new File(dirToCheck, "experiment.apk");
            return configFile.exists() && apkFile.exists();
        }
        return false;
    }

    public static ExperimentStoreEntry getExperimentFromStore(String experimentPackage){
        if(isExperimentInStore(experimentPackage))
            return new ExperimentStoreEntry(experimentPackage);
        else
            return null;
    }

    public static List<ExperimentStoreEntry> getStoredExperiments(boolean cleanInvalidEntries){
        List<ExperimentStoreEntry> entries = new LinkedList<>();
        File dir = IOHelpers.getExperimentsStoragePath();
        if(dir.exists()) {
            for (File subdir : dir.listFiles()) {
                if (subdir.isDirectory()) {
                    File configFile = new File(subdir, "configuration.xml");
                    File apkFile = new File(subdir, "experiment.apk");
                    if (configFile.exists() && apkFile.exists()) //valid entry
                        entries.add(new ExperimentStoreEntry(subdir.getName()));
                    else if (cleanInvalidEntries) //corrupted entry
                        IOHelpers.deleteDirectoryRecursive(subdir);
                }
            }
        }
        return entries;
    }


    public static List<ExperimentStoreEntry> getInstalledExperiments(PackageManager packageManager){
        List<ExperimentStoreEntry> entries = getStoredExperiments(true);
        List<ExperimentStoreEntry> installedEntries = new LinkedList<>();
        for (ExperimentStoreEntry entry : entries){
            if(isApkInstalled(packageManager, entry.getPackageId()))
                installedEntries.add(entry);
        }
        return installedEntries;
    }


    public static ExperimentStoreEntry storeExperiment(File experimentFile, boolean overwriteIfExisting){
        try {
            FileInputStream ins = new FileInputStream(experimentFile);
            return storeExperiment(ins, overwriteIfExisting);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ExperimentStoreEntry storeExperiment(InputStream experimentFileStream, boolean overwriteIfExisting){
        try {
            File baseDir = IOHelpers.getExperimentsStoragePath();
            File dir = new File(baseDir, "temp");
            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Storing experiment to temp directory: " + dir.getAbsolutePath());
            if(dir.exists() && dir.isDirectory()) {
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Clearing temp directory...");
                ClearTempExperimentDir();
            }
            if(!dir.mkdirs()){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to create/access temp directory, aborting operation...");
                ClearTempExperimentDir();
                return null;
            }

            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Uncompressing experiment data...");
            ZipInputStream zipInputStream = new ZipInputStream((experimentFileStream));
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[1024];

            while ((ze = zipInputStream.getNextEntry()) != null) {
                if(ze.getName().equals("configuration.xml") || ze.getName().equals("experiment.apk")){
                    File file = new File(dir, ze.getName());
                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        while ((count = zipInputStream.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        fout.close();
                    }
                }
            }

            if(!(new File(dir, "configuration.xml").exists()) || !(new File(dir, "experiment.apk").exists())){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to decompress experiment. Aborting operation...");
                ClearTempExperimentDir();
                return null;
            }

            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Deserializing configuration...");
            ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(dir.getAbsolutePath() + "/configuration.xml");
            if(experimentConfiguration == null){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to deserialize experiment configuration. Aborting operation...");
                ClearTempExperimentDir();
                return null;
            }

            File newDir = new File(baseDir, experimentConfiguration.ExperimentPackage);
            if(newDir.exists()) {
                if (overwriteIfExisting)
                    IOHelpers.deleteDirectoryRecursive(newDir);
                else {
                    Logger.d(Constants.DEBUGTAG_DELTAAPP, "Experiment directory already exists. Aborting operation...");
                    ClearTempExperimentDir();
                    return null;
                }
            }
            if(newDir.exists()){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Experiment directory already exists and I could not overwrite it. Aborting operation...");
                ClearTempExperimentDir();
                return null;
            }

            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Moving directory...");
            boolean result = dir.renameTo(newDir);
            if(!result){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to rename temp directory...");
                ClearTempExperimentDir();
                return null;
            }
            else {
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Successfully stored experiment to: " + newDir.getAbsolutePath());
                ExperimentStoreEntry experimentStoreEntry = new ExperimentStoreEntry(experimentConfiguration.ExperimentPackage);
                return experimentStoreEntry;
            }
        } catch (IOException e) {
            e.printStackTrace();
            ClearTempExperimentDir();
            return null;
        }
    }

    public static boolean removeExperimentFromStore(String experimentPackage){
        File dirToCheck = new File(IOHelpers.getExperimentsStoragePath(), experimentPackage);
        IOHelpers.deleteDirectoryRecursive(dirToCheck);
        return dirToCheck.exists();
    }

    private static void ClearTempExperimentDir(){
        IOHelpers.deleteDirectoryRecursive(new File(IOHelpers.getExperimentsStoragePath(), "temp"));
    }

    public static X509Certificate getCertificateFromDELTAexp(File deltaexpFile, Context context){
        IOHelpers.unzip(deltaexpFile, IOHelpers.getPrivateStorageDirectory(), "experiment.apk", "tmpexp.apk");
        File tmpExp = new File(IOHelpers.getPrivateStorageDirectory(), "tmpexp.apk");
        if(tmpExp.exists()) {
            X509Certificate x509Certificate = getCertificateFromAPK(tmpExp, context);
            tmpExp.delete();
            return x509Certificate;
        } else {
            return null;
        }
    }

    public static X509Certificate getCertificateFromAPK(File apkFile, Context context) {
        PackageManager pm = context.getPackageManager();

        int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageArchiveInfo(apkFile.getCanonicalPath(), flags);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Signature[] signatures = packageInfo.signatures;
        byte[] cert = signatures[0].toByteArray();
        InputStream input = new ByteArrayInputStream(cert);
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        }
        X509Certificate c = null;
        try {
            c = (X509Certificate) cf.generateCertificate(input);
            return c;
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        }
        /*
        String hexString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(c.getEncoded());
            hexString = byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return hexString;
        */
    }
}
