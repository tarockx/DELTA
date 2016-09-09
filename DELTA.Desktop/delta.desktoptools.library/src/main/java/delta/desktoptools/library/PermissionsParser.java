package delta.desktoptools.library;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Elia on 28/04/2015.
 */
public class PermissionsParser {
    public List<AndroidPermission> GetManifestPermissions(String manifestFilePath){
        try {
            List<AndroidPermission> androidPermissions = null;

            File fXmlFile = new File(manifestFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            NodeList permissionNodes = doc.getElementsByTagName("uses-permission");

            for(int i = 0; i < permissionNodes.getLength(); i++) {
                Element permissionNode = (Element)permissionNodes.item(i);
                AndroidPermission androidPermission = new AndroidPermission(
                        permissionNode.getAttribute("android:name"),
                        permissionNode.getAttribute("android:maxSdkVersion")
                );
                if(androidPermissions == null)
                    androidPermissions = new LinkedList<AndroidPermission>();

                androidPermissions.add(androidPermission);
            }
            
            return  androidPermissions;

        } catch (Exception e) {
            return  null;
        }
    }

}
