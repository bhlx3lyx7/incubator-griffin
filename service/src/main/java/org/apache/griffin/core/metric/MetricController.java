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

package org.apache.griffin.core.metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * In griffin, metricName usually equals to measureName, and we only save measureName in server.
 */

@RestController
@RequestMapping("/metrics")
public class MetricController {
    private static final Logger log = LoggerFactory.getLogger(MetricController.class);
    @Autowired
    MetricService metricService;

    @RequestMapping(value = "/org",method = RequestMethod.GET)
    public String getOrgByMeasureName(@RequestParam("measureName") String measureName) {
        return metricService.getOrgByMeasureName(measureName);
    }
}
