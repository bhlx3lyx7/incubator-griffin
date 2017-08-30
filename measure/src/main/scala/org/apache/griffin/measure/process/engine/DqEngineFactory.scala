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
package org.apache.griffin.measure.process.engine

import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.StreamingContext


object DqEngineFactory {

  private val engineTypes = List("spark-sql", "df-opr")

  private final val SparkSqlEngineType = "spark-sql"
  private final val DataFrameOprEngineType = "df-opr"

  def genDqEngines(sqlContext: SQLContext, ssc: StreamingContext): DqEngines = {
    val engines = engineTypes.flatMap { et =>
      genDqEngine(et, sqlContext, ssc)
    }
    DqEngines(engines)
  }

  private def genDqEngine(engineType: String, sqlContext: SQLContext, ssc: StreamingContext): Option[DqEngine] = {
    engineType match {
      case SparkSqlEngineType => Some(SparkSqlEngine(sqlContext, ssc))
      case DataFrameOprEngineType => Some(DataFrameOprEngine(sqlContext, ssc))
      case _ => None
    }
  }

}