package org.apache.griffin.core.measure;


import org.apache.griffin.core.measure.entity.*;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MeasureTestHelper {
    public static Measure createATestMeasure(String name, String org) throws Exception{
        HashMap<String, String> configMap1 = new HashMap<>();
        configMap1.put("database", "default");
        configMap1.put("table.name", "test_data_src");
        HashMap<String, String> configMap2 = new HashMap<>();
        configMap2.put("database", "default");
        configMap2.put("table.name", "test_data_tgt");
        String configJson1 = new ObjectMapper().writeValueAsString(configMap1);
        String configJson2 = new ObjectMapper().writeValueAsString(configMap2);

        DataSource dataSource = new DataSource("source", Arrays.asList(new DataConnector("HIVE", "1.2", configJson1)));
        DataSource targetSource = new DataSource("target", Arrays.asList(new DataConnector("HIVE", "1.2", configJson2)));

        List<DataSource> dataSources = new ArrayList<>();
        dataSources.add(dataSource);
        dataSources.add(targetSource);
        String rules = "source.id=target.id AND source.name=target.name AND source.age=target.age";
        Rule rule = new Rule("griffin-dsl", "accuracy", rules);
        EvaluateRule evaluateRule = new EvaluateRule(Arrays.asList(rule));
        return new Measure(name, "description", org, "batch", "test", dataSources, evaluateRule);
    }
}
