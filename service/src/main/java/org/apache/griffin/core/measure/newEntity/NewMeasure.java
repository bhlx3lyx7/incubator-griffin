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

package org.apache.griffin.core.measure.newEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.griffin.core.measure.entity.AuditableEntity;

import javax.persistence.*;
import java.util.List;

@Entity
public class NewMeasure extends AuditableEntity {
    private static final long serialVersionUID = -4748881017029815714L;

    private String name;

    public enum ProcessType{
        batch,
        streaming
    }

    @Enumerated(EnumType.STRING)
    private ProcessType processType= ProcessType.batch;

//    @OneToMany(cascade = CascadeType.ALL)
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name="measure_id")
    private List<DataSource> dataSources;

//    @OneToOne(cascade = CascadeType.ALL)
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "evaluateRule_id")
    private EvaluateRule evaluateRule;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("process.type")
    public ProcessType getProcessType() {
        return processType;
    }

    @JsonProperty("process.type")
    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    @JsonProperty("data.sources")
    public List<DataSource> getDataSources() {
        return dataSources;
    }

    @JsonProperty("data.sources")
    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public EvaluateRule getEvaluateRule() {
        return evaluateRule;
    }

    public void setEvaluateRule(EvaluateRule evaluateRule) {
        this.evaluateRule = evaluateRule;
    }

    public NewMeasure() {
    }


    public NewMeasure(String name, ProcessType processType, List<DataSource> dataSources, EvaluateRule evaluateRule) {
        this.name = name;
        this.processType = processType;
        this.dataSources = dataSources;
        this.evaluateRule = evaluateRule;
    }
}
