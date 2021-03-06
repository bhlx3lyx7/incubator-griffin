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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * In griffin, metricName usually equals to measureName, and we only save measureName in server.
 */

@ApiIgnore
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricController.class);
    @Autowired
    MetricService metricService;

    @ApiOperation(value = "Get org by measure name", response = String.class)
    @RequestMapping(value = "/org", method = RequestMethod.GET)
    public String getOrgByMeasureName(@ApiParam(value = "measure name", required = true) @RequestParam("measureName") String measureName) {
        return metricService.getOrgByMeasureName(measureName);
    }
}
