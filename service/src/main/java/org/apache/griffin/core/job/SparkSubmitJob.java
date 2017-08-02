/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.job.config.Conf;
import org.apache.griffin.core.job.entity.JobInstance;
import org.apache.griffin.core.job.entity.LivySessionStateMap;
import org.apache.griffin.core.job.entity.SparkJobDO;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.measure.entity.DataConnector;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.apache.griffin.core.util.GriffinUtil;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SparkSubmitJob implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(SparkSubmitJob.class);

    @Autowired
    private MeasureRepo measureRepo;

    @Autowired
    private JobInstanceRepo jobInstanceRepo;


    @Autowired
    private Properties sparkJobProps;

    private String patItem;
    /**
     * partitionItemSet
     * for example
     * partitionItemSet like "date","hour",...
     */
    private String[] partitionItemSet;
    /**
     * sourcePatternItemSet targetPatternItemSet
     * for example
     * sourcePatternItemSet like "YYYYMMDD","HH",...
     */
    private String[] sourcePatternItemSet;
    private String[] targetPatternItemSet;
    /**
     * sourcePatternPartitionSizeMin is the min between partitionItemSet.length and sourcePatternItemSet.length
     */
    private int sourcePatternPartitionSizeMin = 0;
    private int targetPatternPartitionSizeMin = 0;

    private Measure measure;
    private String sourcePattern;
    private String targetPattern;
    private String dataStartTimestamp;
    private String eachJoblastTimestamp;
    private String periodTime;

    private RestTemplate restTemplate = new RestTemplate();


    private String uri;
    private SparkJobDO sparkJobDO = new SparkJobDO();

    public SparkSubmitJob() {
    }

    @Override
    public void execute(JobExecutionContext context) {
        patItem = sparkJobProps.getProperty("sparkJob.dateAndHour");
        partitionItemSet = patItem.split(",");
        uri = sparkJobProps.getProperty("sparkJob.uri");

        JobDetail jd = context.getJobDetail();
        String groupName=jd.getJobDataMap().getString("groupName");
        String jobName=jd.getJobDataMap().getString("jobName");
        String measureName = jd.getJobDataMap().getString("measure");
        measure = measureRepo.findByName(measureName);
        if (measure==null) {
            LOGGER.error(measureName + " is not find!");
            return;
        }
        sourcePattern = jd.getJobDataMap().getString("sourcePat");
        targetPattern = jd.getJobDataMap().getString("targetPat");
        dataStartTimestamp = jd.getJobDataMap().getString("dataStartTimestamp");
        eachJoblastTimestamp = jd.getJobDataMap().getString("lastTime");
        LOGGER.info(eachJoblastTimestamp);
        periodTime = jd.getJobDataMap().getString("periodTime");
        //prepare current system timestamp
        long currentSystemTimestamp = System.currentTimeMillis();
        LOGGER.info("currentSystemTimestamp: "+currentSystemTimestamp);
        if (StringUtils.isNotEmpty(sourcePattern)) {
            sourcePatternItemSet = sourcePattern.split("-");
            long currentTimstamp = setCurrentTimestamp(currentSystemTimestamp);
            setDataConnectorPartitions(measure.getSource(), sourcePatternItemSet, partitionItemSet, currentTimstamp);
            jd.getJobDataMap().put("lastTime", currentTimstamp + "");
        }
        if (StringUtils.isNotEmpty(targetPattern)) {
            targetPatternItemSet = targetPattern.split("-");
            long currentTimstamp = setCurrentTimestamp(currentSystemTimestamp);
            setDataConnectorPartitions(measure.getTarget(), targetPatternItemSet, partitionItemSet, currentTimstamp);
            jd.getJobDataMap().put("lastTime", currentTimstamp + "");
        }
        //final String uri = "http://10.9.246.187:8998/batches";
        setSparkJobDO();
        String result = restTemplate.postForObject(uri, sparkJobDO, String.class);
        LOGGER.info(result);
        saveJobInstance(groupName,jobName,result);
    }

    public void saveJobInstance(String groupName,String jobName,String result){
        //save JobInstance info into DataBase
        Map<String,Object> resultMap=new HashMap<String,Object>();
        TypeReference<HashMap<String,Object>> type=new TypeReference<HashMap<String,Object>>(){};
        try {
            resultMap= GriffinUtil.toEntity(result,type);
        } catch (IOException e) {
            LOGGER.error("jobInstance jsonStr convert to map failed. "+e);
        }
        JobInstance jobInstance=new JobInstance();
        if(resultMap!=null) {
            jobInstance.setGroupName(groupName);
            jobInstance.setJobName(jobName);
            try {
                jobInstance.setSessionId(Integer.parseInt(resultMap.get("id").toString()));
                jobInstance.setState(LivySessionStateMap.State.valueOf(resultMap.get("state").toString()));
                jobInstance.setAppId(resultMap.get("appId").toString());
            }catch (Exception e){
                LOGGER.warn("jobInstance has null field. "+e);
            }
            jobInstance.setTimestamp(System.currentTimeMillis());
            jobInstanceRepo.save(jobInstance);
        }
    }

    public Map<String, String> genPartitions(String[] patternItemSet, String[] partitionItemSet, long timestamp) {
        int comparableSizeMin=Math.min(patternItemSet.length,partitionItemSet.length);
        Map<String, String> res = new HashMap<>();
        for (int i = 0; i < comparableSizeMin; i++) {
            /**
             * in order to get a standard date like 20170427 01
             */
            String pattrn = patternItemSet[i].replace("mm", "MM");
            pattrn = pattrn.replace("DD", "dd");
            pattrn = pattrn.replace("hh", "HH");
            SimpleDateFormat sdf = new SimpleDateFormat(pattrn);
            res.put(partitionItemSet[i], sdf.format(new Date(timestamp)));
        }
        return res;
    }

    public void setDataConnectorPartitions(DataConnector dc, String[] patternItemSet, String[] partitionItemSet, long timestamp) {
        Map<String, String> partitionItemMap = genPartitions(patternItemSet, partitionItemSet, timestamp);
        String partitions = partitionItemMap.toString().substring(1, partitionItemMap.toString().length() - 1);

        Map<String, String> configMap = dc.getConfigInMaps();
        //config should not be null
        configMap.put("partitions", partitions);
        try {
            dc.setConfig(configMap);
        } catch (JsonProcessingException e) {
            LOGGER.error(""+e);
        }
    }

    public long setCurrentTimestamp(long currentSystemTimestamp) {
        long currentTimstamp=0;
        if (StringUtils.isNotEmpty(eachJoblastTimestamp)) {
            try {
                currentTimstamp = Long.parseLong(eachJoblastTimestamp) + Integer.parseInt(periodTime) * 1000;
            }catch (Exception e){
                LOGGER.info("lasttime or periodTime format problem! "+e);
            }
        } else {
            if (StringUtils.isNotEmpty(dataStartTimestamp)) {
                try{
                    currentTimstamp = Long.parseLong(dataStartTimestamp);
                }catch (Exception e){
                    LOGGER.info("dataStartTimestamp format problem! "+e);
                }
            } else {
                currentTimstamp = currentSystemTimestamp;
            }
        }
        return currentTimstamp;
    }

    public void setSparkJobDO() {
        sparkJobDO.setFile(sparkJobProps.getProperty("sparkJob.file"));
        sparkJobDO.setClassName(sparkJobProps.getProperty("sparkJob.className"));

        List<String> args = new ArrayList<>();
        args.add(sparkJobProps.getProperty("sparkJob.args_1"));
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String measureJson = "";
        try {
            measureJson = ow.writeValueAsString(measure);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        args.add(measureJson);  //partition
        args.add(sparkJobProps.getProperty("sparkJob.args_3"));
        sparkJobDO.setArgs(args);

        sparkJobDO.setName(sparkJobProps.getProperty("sparkJob.name"));
        sparkJobDO.setQueue(sparkJobProps.getProperty("sparkJob.queue"));
        sparkJobDO.setNumExecutors(Long.parseLong(sparkJobProps.getProperty("sparkJob.numExecutors")));
        sparkJobDO.setExecutorCores(Long.parseLong(sparkJobProps.getProperty("sparkJob.executorCores")));
        sparkJobDO.setDriverMemory(sparkJobProps.getProperty("sparkJob.driverMemory"));
        sparkJobDO.setExecutorMemory(sparkJobProps.getProperty("sparkJob.executorMemory"));

        Conf conf = new Conf();
        conf.setSpark_jars_packages(sparkJobProps.getProperty("sparkJob.spark.jars.packages"));
        sparkJobDO.setConf(conf);

        List<String> jars = new ArrayList<>();
        jars.add(sparkJobProps.getProperty("sparkJob.jars_1"));
        jars.add(sparkJobProps.getProperty("sparkJob.jars_2"));
        jars.add(sparkJobProps.getProperty("sparkJob.jars_3"));
        sparkJobDO.setJars(jars);

        List<String> files = new ArrayList<>();
        sparkJobDO.setFiles(files);

//        CreateBatchRequest createBatchRequest=new CreateBatchRequest;
//        createBatchRequest.file_$eq(sparkJobProps.getProperty("sparkJob.file"));
//        createBatchRequest.className_$eq(new Some(sparkJobProps.getProperty("sparkJob.className")));
//        createBatchRequest.args_$eq(args);
//        scala.collection.immutable.List argss=new scala.collection.immutable.List<String>();
//        createBatchRequest.name_$eq(new Some(sparkJobProps.getProperty("sparkJob.name")));
    }
}