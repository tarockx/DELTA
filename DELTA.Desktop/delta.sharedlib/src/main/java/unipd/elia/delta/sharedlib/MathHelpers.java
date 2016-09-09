package unipd.elia.delta.sharedlib;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Elia on 18/05/2015.
 */
public class MathHelpers {
    public static long gcd(long a, long b)
    {
        while (b > 0)
        {
            long temp = b;
            b = a % b; // % is remainder
            a = temp;
        }
        return a;
    }

    public static long gcd(long[] input)
    {
        long result = input[0];
        for(int i = 1; i < input.length; i++) result = gcd(result, input[i]);
        return result;
    }

    public static long lcm(long a, long b)
    {
        return a * (b / gcd(a, b));
    }

    public static long lcm(long[] input)
    {
        long result = input[0];
        for(int i = 1; i < input.length; i++) result = lcm(result, input[i]);
        return result;
    }

    public static List<Integer> getFrequencies(int min){
        List<Integer> frequencies = new LinkedList<>();

        //Short periods (msecs)
        for(int i = 50; i < 1000; i += 50)
            if(i >= min)
                frequencies.add(i);

        //short periods (seconds)
        for(int i = 1000; i <= 10 * 1000; i += 1000)
            if(i >= min)
                frequencies.add(i);

        //sporadic periods (minutes)
        if(30000 >= min)
            frequencies.add(30000);
        if(60000 >= min)
            frequencies.add(60000);
        if(120000 >= min)
            frequencies.add(120000);
        for(int i = 60 * 5 * 1000; i < 3600 * 1000; i+= 60 * 5 * 1000)
            if(i >= min)
                frequencies.add(i);

        //sporadic periods (hours)
        for(int i = 1; i <= 24; i++)
            if(i * 3600 * 1000 >= min)
                frequencies.add(i * 3600 * 1000);

        return frequencies;
    }

    public static String getReadableTime(int milliseconds){
        if(milliseconds < 1000)
            return Integer.toString(milliseconds) + " msec";

        if(milliseconds < 60 * 1000)
            return Integer.toString(milliseconds / 1000) + " seconds";

        if(milliseconds < 3600 * 1000)
            return Integer.toString(milliseconds / 60 / 1000) + " minutes";

        return Integer.toString(milliseconds / 3600 / 1000) + " hours";
    }

    public static class ExperimentHarmonicValues{
        public long HARMONIC_POLLING_FREQUENCY = -1;
        public long HARMONIC_CYCLE = -1;
    }

    public static class CycleValues{
        public int frequency;
        public int iterations;
    }

    public static class ExperimentCycles{
        public Map<PluginConfiguration, CycleValues> plugin2cycle = new HashMap<>();
        public Map<CycleValues, List<PluginConfiguration>> cycle2plugins = new HashMap<>();
    }

    public static ExperimentCycles getStrictTimerCycles(ExperimentConfiguration experimentConfiguration){
        List<PluginConfiguration> plugins = experimentConfiguration.getAllPluginConfigurations(true);
        return getStrictTimerCycles(plugins);
    }

    public static ExperimentCycles getStrictTimerCycles(List<PluginConfiguration> plugins){

        Collections.sort(plugins, new Comparator<PluginConfiguration>() {
            @Override
            public int compare(PluginConfiguration o1, PluginConfiguration o2) {
                return o1.PollingFrequency - o2.PollingFrequency;
            }
        });

        List<PluginConfiguration> selectedPlugins = new LinkedList<>();
        for (PluginConfiguration pluginConfiguration : plugins)
            if(pluginConfiguration.PollingFrequency <= 10000)
                selectedPlugins.add(pluginConfiguration);

        return selectedPlugins.size() == 0 ? null : getExperimentHarmonicValues(selectedPlugins);
    }


    public static ExperimentCycles getSporadicCycles(ExperimentConfiguration experimentConfiguration){
        List<PluginConfiguration> plugins = experimentConfiguration.getAllPluginConfigurations(true);
        return getSporadicCycles(plugins);
    }

    public static ExperimentCycles getSporadicCycles(List<PluginConfiguration> plugins){
        Collections.sort(plugins, new Comparator<PluginConfiguration>() {
            @Override
            public int compare(PluginConfiguration o1, PluginConfiguration o2) {
                return o1.PollingFrequency - o2.PollingFrequency;
            }
        });

        List<PluginConfiguration> selectedPlugins = new LinkedList<>();
        for (PluginConfiguration pluginConfiguration : plugins)
            if(pluginConfiguration.PollingFrequency >= 30000)
                selectedPlugins.add(pluginConfiguration);

        return selectedPlugins.size() == 0 ? null : getExperimentHarmonicValues(selectedPlugins);
    }

    private static ExperimentCycles getExperimentHarmonicValues(List<PluginConfiguration> plugins){
        //ExperimentHarmonicValues experimentHarmonicValues = new ExperimentHarmonicValues();

        ExperimentCycles experimentCycles = new ExperimentCycles();

        for(PluginConfiguration pc : plugins){
            //try to add to existing cycle, if harmonic one exists
            for(CycleValues cycleValues : experimentCycles.cycle2plugins.keySet()){
                if(pc.PollingFrequency % cycleValues.frequency == 0) {
                    experimentCycles.plugin2cycle.put(pc, cycleValues);
                    experimentCycles.cycle2plugins.get(cycleValues).add(pc);
                    break;
                }
            }

            //if not possible, new entry
            if(!experimentCycles.plugin2cycle.containsKey(pc)){
                CycleValues cycleValues = new CycleValues();
                cycleValues.frequency = pc.PollingFrequency;
                experimentCycles.cycle2plugins.put(cycleValues, new LinkedList<PluginConfiguration>());
                experimentCycles.cycle2plugins.get(cycleValues).add(pc);
                experimentCycles.plugin2cycle.put(pc, cycleValues);
            }

        }

        for(CycleValues cycleValues : experimentCycles.cycle2plugins.keySet()){
            //compute iteration counts
            long lcm = cycleValues.frequency;
            for(PluginConfiguration pluginConfiguration : experimentCycles.cycle2plugins.get(cycleValues)){
                if(pluginConfiguration.PollingFrequency > cycleValues.frequency)
                    lcm = MathHelpers.lcm(lcm, pluginConfiguration.PollingFrequency);
            }
            cycleValues.iterations = (int)lcm / cycleValues.frequency;
        }

        return experimentCycles;

        /*
        long lcm = -1;

        for(PluginConfiguration pc : plugins){
            if(experimentHarmonicValues.HARMONIC_POLLING_FREQUENCY == -1 || experimentHarmonicValues.HARMONIC_POLLING_FREQUENCY > pc.PollingFrequency)
                experimentHarmonicValues.HARMONIC_POLLING_FREQUENCY = pc.PollingFrequency;
            if(lcm == -1)
                lcm = experimentHarmonicValues.HARMONIC_POLLING_FREQUENCY;
            else
                lcm = MathHelpers.lcm(lcm, pc.PollingFrequency);
        }

        experimentHarmonicValues.HARMONIC_CYCLE = lcm / experimentHarmonicValues.HARMONIC_POLLING_FREQUENCY;

        return experimentHarmonicValues;
        */
    }


}
