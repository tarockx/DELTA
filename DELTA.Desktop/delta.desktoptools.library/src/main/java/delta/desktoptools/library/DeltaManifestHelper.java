package delta.desktoptools.library;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.delta.sharedlib.PluginConfiguration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;


/**
 * Created by Elia on 23/04/2015.
 */
public class DeltaManifestHelper {

    /**
     * Checks whether a certain directory contains sourcecode that implements one (or more) DELTA Plugins
     * @param directory The directory to check
     * @return True if the java files inside the directory (and subdirectories) contain at least one valid DELTA Plugin implementation, false otherwise
     */
    public static boolean isDeltaPluginProject(String directory){
        try{
            //Parse directories in search of plugin projects
            PluginClassParser p = new PluginClassParser();
            List<File> files = IOHelpers.getAllFilesInFolderByExtension(new File(directory), "java", true);
            List<PluginConfiguration> pluginClasses = p.Parse(files);

            //Just want to check if it's a plugin project
            return pluginClasses != null && pluginClasses.size() > 0;

        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static void writeDeltaManifest(String projectPath, String outputFile){
        String manifestPath = IOHelpers.getAndroidManifestPath(projectPath);
        if(manifestPath == null)
            return;

        String packageID = getPackageIdFromManifest(manifestPath);

        PluginClassParser p = new PluginClassParser();
        List<File> files = IOHelpers.getAllFilesInFolderByExtension(new File(projectPath), "java", true);
        List<PluginConfiguration> pluginClasses = p.Parse(files);

        DeltaManifestHelper.writeDeltaManifest(packageID, pluginClasses, outputFile);
    }

    /**
     * Writes an XML document that describes the plugins inside a DELTA Plugin Package. The manifest can be used to know which plugins
     * are provided by a certain Android package
     * @param pluginClasses The available Plugins
     * @param outputFilePath Output file path (will be overwritten if existing)
     */
    public static void writeDeltaManifest(String packageID, List<PluginConfiguration> pluginClasses, String outputFilePath){
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("DeltaPluginManifest");
            doc.appendChild(rootElement);

            rootElement.setAttribute("version", "1");
            rootElement.setAttribute("packageID", packageID);

            // plugin class elements
            Element pluginClassesElem = doc.createElement("PluginClasses");
            rootElement.appendChild(pluginClassesElem);

            for(PluginConfiguration pluginClass : pluginClasses) {
                pluginClass.PluginPackage = packageID;
                ExperimentConfigurationIOHelpers.writePluginConfiguration(doc, pluginClassesElem, pluginClass);
            }

            //permissions
            /*
            Element permissionsElem = doc.createElement("RequiredPermissions");
            rootElement.appendChild(permissionsElem);

            if(requiredPermissions != null) {
                for (AndroidPermission permission : requiredPermissions) {
                    Element permissionElem = doc.createElement("uses-permission");
                    permissionsElem.appendChild(permissionElem);
                    permissionElem.setAttribute("name", permission.name);
                    if(permission.maxSdkVersion != null && !permission.maxSdkVersion.isEmpty())
                        permissionElem.setAttribute("maxSdkVersion", permission.maxSdkVersion);
                }
            }
            */

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);

            File outputFile = new File(outputFilePath);
            if(outputFile.exists())
                outputFile.delete();
            else {
                File outputDir = outputFile.getParentFile();
                if(!outputDir.exists())
                    outputDir.mkdirs();
            }


            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }


    public static List<PluginConfiguration> readPluginManifest(String manifestFilePath){
        try {
            FileInputStream fileInputStream = new FileInputStream(manifestFilePath);
            return readPluginManifest(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<PluginConfiguration> readPluginManifest(InputStream manifestInputStream){
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(manifestInputStream);
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            // root elements
            Element rootElement = (Element) doc.getElementsByTagName("PluginClasses").item(0);
            return ExperimentConfigurationIOHelpers.getPluginConfigurations(rootElement);
        }catch (Exception ex){
            return null;
        }
    }




    /**
     * Returns the Android package ID attribute from an Android Manifest file
     * @param manifestFilePath the path to the manifest file
     * @return the package ID
     */
    public static String getPackageIdFromManifest(String manifestFilePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(manifestFilePath);
            return getPackageIdFromManifest(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the Android package ID attribute from an Android Manifest input stream
     * @param manifestInputStream an inputstream containing data from an Android Manifest
     * @return the package ID
     */
    public static String getPackageIdFromManifest(InputStream manifestInputStream) {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(manifestInputStream);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            // root elements
            Element rootElement = (Element)doc.getElementsByTagName("manifest").item(0);

            String packageID = rootElement.getAttribute("package");

            return packageID;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Injects the specified permissions in an AndroidManifest file.
     * @deprecated The new Gradle build system merges manifests automatically, this is currently unused
     * @param requiredPermissions List of required permissions
     * @param manifestFilePath Path to the AndroidManifest.xml to patch
     */
    public static void PatchLogSubstrateAndroidManifest(List<AndroidPermission> requiredPermissions, String manifestFilePath){
        try {

            File fXmlFile = new File(manifestFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            // root elements
            Element rootElement = (Element)doc.getElementsByTagName("manifest").item(0);

            //Filter out permissions already requested
            NodeList permissionElements = rootElement.getElementsByTagName("uses-permission");
            for(int i = 0; i < permissionElements.getLength(); i++){
                Element permissionElement = (Element)permissionElements.item(i);
                String permissionName = permissionElement.getAttribute("android:name");
                if(permissionName != null && !permissionName.isEmpty()){
                    for(int j = 0; j < requiredPermissions.size(); j++){
                        if(permissionName.equals(requiredPermissions.get(j).name)){
                            requiredPermissions.remove(j);
                            break;
                        }
                    }
                }
            }

            //Write new permissions
            for(AndroidPermission requiredPermission : requiredPermissions){
                Element permissionElement = doc.createElement("uses-permission");
                permissionElement.setAttribute("android:name", requiredPermission.name);
                if(requiredPermission.maxSdkVersion != null && !requiredPermission.maxSdkVersion.isEmpty())
                    permissionElement.setAttribute("android:maxSdkVersion", requiredPermission.maxSdkVersion);

                rootElement.appendChild(permissionElement);
            }


            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(manifestFilePath));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
