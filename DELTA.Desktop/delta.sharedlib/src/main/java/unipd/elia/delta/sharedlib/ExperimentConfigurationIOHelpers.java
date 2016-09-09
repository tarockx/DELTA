package unipd.elia.delta.sharedlib;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Elia on 13/05/2015.
 */
public class ExperimentConfigurationIOHelpers {

    public static ExperimentConfiguration DeserializeExperiment(String filePath){
        try {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            return DeserializeExperimentConfiguration(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    public static ExperimentConfiguration DeserializeExperiment(InputStream stream){
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);

            return DeserializeExperimentConfiguration(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    public static void SerializeExperimentConfiguration(ExperimentConfiguration exp, String outputFile){
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("DeltaExperiment");
            doc.appendChild(rootElement);

            rootElement.setAttribute("version", "1");

            //main data
            rootElement.setAttribute("ExperimentName", exp.ExperimentName);
            rootElement.setAttribute("ExperimentAuthor", exp.ExperimentAuthor);
            rootElement.setAttribute("ExperimentPackage", exp.ExperimentPackage);
            if(exp.DeltaServerUrl != null)
                rootElement.setAttribute("DeltaServerUrl", exp.DeltaServerUrl);
            rootElement.setAttribute("SuspendOnScreenOff", Boolean.toString(exp.SuspendOnScreenOff));

            Element experimentDescriptionElem = doc.createElement("ExperimentDescription");
            rootElement.appendChild(experimentDescriptionElem);
            experimentDescriptionElem.setTextContent(exp.ExperimentDescription);

            // plugin class elements
            Element pluginsElement = doc.createElement("Plugins");
            rootElement.appendChild(pluginsElement);

            for(List<PluginConfiguration> pluginClassList : exp.Plugins.values()) {
                for (PluginConfiguration pluginClass : pluginClassList) {
                    writePluginConfiguration(doc, pluginsElement, pluginClass);
                }
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(outputFile));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    public static void writePluginConfiguration(Document doc, Element containerElement, PluginConfiguration pluginClass) {
        Element pluginClassElem = doc.createElement("PluginClass");
        containerElement.appendChild(pluginClassElem);

        pluginClassElem.setAttribute("PluginClassQualifiedName", pluginClass.PluginClassQualifiedName);
        pluginClassElem.setAttribute("PluginAuthor", pluginClass.PluginAuthor);
        pluginClassElem.setAttribute("PluginName", pluginClass.PluginName);
        pluginClassElem.setAttribute("PluginPackage", pluginClass.PluginPackage);
        pluginClassElem.setAttribute("PollingFrequency", Integer.toString(pluginClass.PollingFrequency));
        pluginClassElem.setAttribute("MinPollingFrequency", Integer.toString(pluginClass.MinPollingFrequency));
        pluginClassElem.setAttribute("SupportsPolling", Boolean.toString(pluginClass.supportsPolling));
        pluginClassElem.setAttribute("SupportsEvents", Boolean.toString(pluginClass.supportsEvents));
        pluginClassElem.setAttribute("IsEnabled", Boolean.toString(pluginClass.IsEnabled));
        pluginClassElem.setAttribute("AllowOptOut", Boolean.toString(pluginClass.AllowOptOut));
        pluginClassElem.setAttribute("RequiresRoot", Boolean.toString(pluginClass.RequiresRoot));
        pluginClassElem.setAttribute("RequiresWakelock", Boolean.toString(pluginClass.RequiresWakelock));
        pluginClassElem.setAttribute("MinSDK", Integer.toString(pluginClass.MinSDK));

        Element pluginDescriptionElem = doc.createElement("PluginDescription");
        pluginClassElem.appendChild(pluginDescriptionElem);
        pluginDescriptionElem.setTextContent(pluginClass.PluginDescription);

        Element developerDescriptionElem = doc.createElement("DeveloperDescription");
        pluginClassElem.appendChild(developerDescriptionElem);
        developerDescriptionElem.setTextContent(pluginClass.DeveloperDescription);

        if(pluginClass.Options != null && pluginClass.Options.size() > 0){
            Element optionsElement = doc.createElement("Options");
            pluginClassElem.appendChild(optionsElement);

            for(DeltaOption option : pluginClass.Options){
                if (StringOption.class.isInstance(option)) {
                    StringOption stringOption = (StringOption) option;
                    Element stringOptionElement = doc.createElement("StringOption");
                    stringOptionElement.setAttribute("ID", stringOption.ID);
                    stringOptionElement.setAttribute("Name", stringOption.Name);

                    Element descriptionElement = doc.createElement("Description");
                    descriptionElement.setTextContent(stringOption.Description);
                    stringOptionElement.appendChild(descriptionElement);

                    Element defaultValueElement = doc.createElement("DefaultValue");
                    defaultValueElement.setTextContent(stringOption.defaultValue);
                    stringOptionElement.appendChild(defaultValueElement);

                    Element valueElement = doc.createElement("Value");
                    valueElement.setTextContent(stringOption.Value);
                    stringOptionElement.appendChild(valueElement);

                    stringOptionElement.setAttribute("Multiline", Boolean.toString(stringOption.Multiline));

                    Element availableChoicesElement = doc.createElement("AvailableChoices");
                    for(String choice : stringOption.AvailableChoices){
                        Element choiceElement = doc.createElement("Choice");
                        choiceElement.setTextContent(choice);
                        availableChoicesElement.appendChild(choiceElement);
                    }
                    stringOptionElement.appendChild(availableChoicesElement);

                    optionsElement.appendChild(stringOptionElement);
                }
                else if(BooleanOption.class.isInstance(option)){
                    BooleanOption booleanOption = (BooleanOption) option;
                    Element booleanOptionElement = doc.createElement("BooleanOption");
                    booleanOptionElement.setAttribute("ID", booleanOption.ID);
                    booleanOptionElement.setAttribute("Name", booleanOption.Name);

                    Element descriptionElement = doc.createElement("Description");
                    descriptionElement.setTextContent(booleanOption.Description);
                    booleanOptionElement.appendChild(descriptionElement);

                    booleanOptionElement.setAttribute("Value", Boolean.toString(booleanOption.Value));
                    booleanOptionElement.setAttribute("DefaultValue", Boolean.toString(booleanOption.defaultValue));
                    optionsElement.appendChild(booleanOptionElement);
                }
                else if(IntegerOption.class.isInstance(option)){
                    IntegerOption integerOption = (IntegerOption) option;
                    Element integerOptionElement = doc.createElement("IntegerOption");
                    integerOptionElement.setAttribute("ID", integerOption.ID);
                    integerOptionElement.setAttribute("Name", integerOption.Name);

                    Element descriptionElement = doc.createElement("Description");
                    descriptionElement.setTextContent(integerOption.Description);
                    integerOptionElement.appendChild(descriptionElement);

                    integerOptionElement.setAttribute("Value", Integer.toString(integerOption.Value));
                    integerOptionElement.setAttribute("DefaultValue", Integer.toString(integerOption.defaultValue));
                    if(integerOption.MinValue != null)
                        integerOptionElement.setAttribute("MinValue", Integer.toString(integerOption.MinValue));
                    if(integerOption.MaxValue != null)
                        integerOptionElement.setAttribute("MaxValue", Integer.toString(integerOption.MaxValue));

                    optionsElement.appendChild(integerOptionElement);
                }
                else if(DoubleOption.class.isInstance(option)){
                    DoubleOption doubleOption = (DoubleOption) option;
                    Element doubleOptionElement = doc.createElement("IntegerOption");
                    doubleOptionElement.setAttribute("ID", doubleOption.ID);
                    doubleOptionElement.setAttribute("Name", doubleOption.Name);

                    Element descriptionElement = doc.createElement("Description");
                    descriptionElement.setTextContent(doubleOption.Description);
                    doubleOptionElement.appendChild(descriptionElement);

                    doubleOptionElement.setAttribute("Value", Double.toString(doubleOption.Value));
                    doubleOptionElement.setAttribute("DefaultValue", Double.toString(doubleOption.defaultValue));
                    if(doubleOption.MinValue != null)
                        doubleOptionElement.setAttribute("MinValue", Double.toString(doubleOption.MinValue));
                    if(doubleOption.MaxValue != null)
                        doubleOptionElement.setAttribute("MaxValue", Double.toString(doubleOption.MaxValue));

                    optionsElement.appendChild(doubleOptionElement);
                }
            }
        }
    }

    private static ExperimentConfiguration DeserializeExperimentConfiguration(Document doc) {
        ExperimentConfiguration experimentConfiguration;
        try {
            experimentConfiguration = new ExperimentConfiguration();

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            // root elements
            Element rootElement = (Element) doc.getElementsByTagName("DeltaExperiment").item(0);

            experimentConfiguration.ExperimentName = rootElement.getAttribute("ExperimentName");
            experimentConfiguration.ExperimentPackage = rootElement.getAttribute("ExperimentPackage");
            experimentConfiguration.ExperimentAuthor = rootElement.getAttribute("ExperimentAuthor");
            experimentConfiguration.DeltaServerUrl = rootElement.getAttribute("DeltaServerUrl");
            experimentConfiguration.SuspendOnScreenOff = Boolean.parseBoolean(rootElement.getAttribute("SuspendOnScreenOff"));

            Element descriptionElement = (Element) rootElement.getElementsByTagName("ExperimentDescription").item(0);
            experimentConfiguration.ExperimentDescription = descriptionElement.getTextContent();

            Element pluginsElement = (Element) rootElement.getElementsByTagName("Plugins").item(0);
            List<PluginConfiguration> pluginConfigurations = getPluginConfigurations(pluginsElement);
            for(PluginConfiguration pluginConfiguration : pluginConfigurations)
                experimentConfiguration.AddPlugin(pluginConfiguration);

        }
        catch (Exception ex){
            ex.printStackTrace();
            experimentConfiguration = null;
        }

        return experimentConfiguration;
    }

    public static List<PluginConfiguration> getPluginConfigurations(Element containerElement){
        List<PluginConfiguration> plugins = new LinkedList<>();
        NodeList pluginElements = containerElement.getElementsByTagName("PluginClass");
        if(pluginElements != null) {

            try {
                for (int i = 0; i < pluginElements.getLength(); i++) {
                    Element pluginClassElement = (Element) pluginElements.item(i);
                    PluginConfiguration pluginConfiguration = new PluginConfiguration();
                    pluginConfiguration.PluginClassQualifiedName = pluginClassElement.getAttribute("PluginClassQualifiedName");
                    pluginConfiguration.PluginPackage = pluginClassElement.getAttribute("PluginPackage");
                    pluginConfiguration.PluginAuthor = pluginClassElement.getAttribute("PluginAuthor");
                    pluginConfiguration.PluginName = pluginClassElement.getAttribute("PluginName");
                    String pluginPollingInterval = pluginClassElement.getAttribute("PollingFrequency");
                    pluginConfiguration.PollingFrequency = pluginPollingInterval.isEmpty() ? -1 : Integer.parseInt(pluginPollingInterval);
                    String pluginPollingMinInterval = pluginClassElement.getAttribute("MinPollingFrequency");
                    pluginConfiguration.MinPollingFrequency = pluginPollingMinInterval.isEmpty() ? 0 : Integer.parseInt(pluginPollingMinInterval);
                    pluginConfiguration.supportsPolling = Boolean.parseBoolean(pluginClassElement.getAttribute("SupportsPolling"));
                    pluginConfiguration.supportsEvents = Boolean.parseBoolean(pluginClassElement.getAttribute("SupportsEvents"));
                    pluginConfiguration.IsEnabled = Boolean.parseBoolean(pluginClassElement.getAttribute("IsEnabled"));
                    pluginConfiguration.AllowOptOut = Boolean.parseBoolean(pluginClassElement.getAttribute("AllowOptOut"));
                    pluginConfiguration.RequiresRoot = Boolean.parseBoolean(pluginClassElement.getAttribute("RequiresRoot"));
                    pluginConfiguration.RequiresWakelock = Boolean.parseBoolean(pluginClassElement.getAttribute("RequiresWakelock"));
                    pluginConfiguration.MinSDK = Integer.parseInt(pluginClassElement.getAttribute("MinSDK"));

                    Element pluginDescriptionElem = (Element) pluginClassElement.getElementsByTagName("PluginDescription").item(0);
                    pluginConfiguration.PluginDescription = pluginDescriptionElem.getTextContent();

                    if (pluginClassElement.getElementsByTagName("DeveloperDescription").getLength() > 0) {
                        Element developerDescriptionElem = (Element) pluginClassElement.getElementsByTagName("DeveloperDescription").item(0);
                        pluginConfiguration.DeveloperDescription = developerDescriptionElem.getTextContent();
                    } else {
                        pluginConfiguration.DeveloperDescription = "N/A";
                    }



                    NodeList optionsElemNodes = pluginClassElement.getElementsByTagName("Options");
                    if (optionsElemNodes != null && optionsElemNodes.getLength() > 0) {
                        NodeList optionElems = optionsElemNodes.item(0).getChildNodes();
                        if (optionElems != null && optionElems.getLength() > 0) {
                            pluginConfiguration.Options = new LinkedList<>();

                            for (int j = 0; j < optionElems.getLength(); j++) {
                                if (!Element.class.isInstance(optionElems.item(j)))
                                    continue;

                                Element optionElement = (Element) optionElems.item(j);
                                if (optionElement.getTagName().equals("StringOption")) {
                                    StringOption stringOption = new StringOption();
                                    stringOption.ID = optionElement.getAttribute("ID");
                                    stringOption.Name = optionElement.getAttribute("Name");
                                    stringOption.Description = optionElement.getElementsByTagName("Description").item(0).getTextContent();
                                    stringOption.Value = optionElement.getElementsByTagName("Value").item(0).getTextContent();
                                    stringOption.defaultValue = optionElement.getElementsByTagName("DefaultValue").item(0).getTextContent();
                                    stringOption.Multiline = Boolean.parseBoolean(optionElement.getAttribute("Multiline"));

                                    NodeList choices = optionElement.getElementsByTagName("Choice");
                                    if(choices != null && choices.getLength() > 0){
                                        String[] choicesArray = new String[choices.getLength()];
                                        for(int k = 0; k < choices.getLength(); k++)
                                            choicesArray[k] = choices.item(k).getTextContent();
                                    }

                                    pluginConfiguration.Options.add(stringOption);
                                } else if (optionElement.getTagName().equals("BooleanOption")) {
                                    BooleanOption booleanOption = new BooleanOption();
                                    booleanOption.ID = optionElement.getAttribute("ID");
                                    booleanOption.Name = optionElement.getAttribute("Name");
                                    booleanOption.Description = optionElement.getElementsByTagName("Description").item(0).getTextContent();
                                    booleanOption.Value = Boolean.parseBoolean(optionElement.getAttribute("Value"));
                                    booleanOption.defaultValue = Boolean.parseBoolean(optionElement.getAttribute("DefaultValue"));
                                    pluginConfiguration.Options.add(booleanOption);
                                } else if (optionElement.getTagName().equals("IntegerOption")) {
                                    IntegerOption integerOption = new IntegerOption();
                                    integerOption.ID = optionElement.getAttribute("ID");
                                    integerOption.Name = optionElement.getAttribute("Name");
                                    integerOption.Description = optionElement.getElementsByTagName("Description").item(0).getTextContent();
                                    integerOption.Value = Integer.parseInt(optionElement.getAttribute("Value"));
                                    integerOption.defaultValue = Integer.parseInt(optionElement.getAttribute("DefaultValue"));
                                    if(optionElement.hasAttribute("MaxValue"))
                                        integerOption.MaxValue = Integer.parseInt(optionElement.getAttribute("MaxValue"));
                                    if(optionElement.hasAttribute("MinValue"))
                                        integerOption.MinValue = Integer.parseInt(optionElement.getAttribute("MinValue"));

                                    pluginConfiguration.Options.add(integerOption);
                                } else if (optionElement.getTagName().equals("DoubleOption")) {
                                    DoubleOption doubleOption = new DoubleOption();
                                    doubleOption.ID = optionElement.getAttribute("ID");
                                    doubleOption.Name = optionElement.getAttribute("Name");
                                    doubleOption.Description = optionElement.getElementsByTagName("Description").item(0).getTextContent();
                                    doubleOption.Value = Double.parseDouble(optionElement.getAttribute("Value"));
                                    doubleOption.defaultValue = Integer.parseInt(optionElement.getAttribute("DefaultValue"));
                                    if(optionElement.hasAttribute("MaxValue"))
                                        doubleOption.MaxValue = Double.parseDouble(optionElement.getAttribute("MaxValue"));
                                    if(optionElement.hasAttribute("MinValue"))
                                        doubleOption.MinValue = Double.parseDouble(optionElement.getAttribute("MinValue"));

                                    pluginConfiguration.Options.add(doubleOption);
                                }
                            }
                        }
                    }

                    plugins.add(pluginConfiguration);
                }
            } catch (DOMException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return plugins;
    }
}
