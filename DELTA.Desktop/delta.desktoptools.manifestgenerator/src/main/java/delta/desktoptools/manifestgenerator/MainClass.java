package delta.desktoptools.manifestgenerator;

import delta.desktoptools.library.*;

/**
 * Created by Elia on 23/04/2015.
 */
public class MainClass {
    public static void main(String [ ] args)
    {
        if(args.length < 1){
            System.out.println("No arguments supplied!");
            System.out.println("Usage:\n\n" +
                    "To check if a directory contains DELTA Plugin implementations: manifestgenerator <directory_to_search>\n" +
                    "To write a DELTA Plugin manifest: manifestgenerator <directory_to_search> <output_manifest_file>");
            return;
        }

        try{
            //Just want to check if it's a plugin project
            if(args.length == 1){
                System.out.println(DeltaManifestHelper.isDeltaPluginProject(args[0]) ? "KO" : "OK");
            }

            //we actually want to write the Plugin Manifest
            else if(args.length == 2) {
                DeltaManifestHelper.writeDeltaManifest(args[0], args[1]);
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }

    }
}
