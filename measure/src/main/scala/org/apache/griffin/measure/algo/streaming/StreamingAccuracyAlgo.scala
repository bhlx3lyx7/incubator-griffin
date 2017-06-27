/*-
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 */
package org.apache.griffin.measure.algo.streaming

import java.util.Date

import org.apache.griffin.measure.algo.AccuracyAlgo
import org.apache.griffin.measure.config.params.AllParam
import org.apache.griffin.measure.connector.{DataConnector, DataConnectorFactory}
import org.apache.griffin.measure.persist.{Persist, PersistFactory}
import org.apache.griffin.measure.rule.{ExprValueUtil, RuleAnalyzer, RuleFactory}
import org.apache.griffin.measure.rule.expr.StatementExpr
import org.apache.griffin.measure.utils.TimeUtil
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.streaming.{Milliseconds, Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.{Failure, Success, Try}


case class StreamingAccuracyAlgo(allParam: AllParam) extends AccuracyAlgo {
  val envParam = allParam.envParam
  val userParam = allParam.userParam

  def run(): Try[_] = {
    Try {
      val metricName = userParam.name

      val conf = new SparkConf().setAppName(metricName)
      val sc = new SparkContext(conf)
      val sqlContext = new HiveContext(sc)

      val interval = TimeUtil.milliseconds(envParam.sparkParam.batchInterval) match {
        case Some(interval) => Milliseconds(interval)
        case _ => throw new Exception("invalid batch interval")
      }
      val ssc = new StreamingContext(sc, interval)
      ssc.checkpoint(envParam.sparkParam.cpDir)

      // start time
      val startTime = new Date().getTime()

      // get persists to persist measure result
      val persist: Persist = PersistFactory(envParam.persistParams, metricName).getPersists(startTime)

      // get spark application id
      val applicationId = sc.applicationId

      // persist start id
      persist.start(applicationId)

      // generate rule from rule param, generate rule analyzer
      val ruleFactory = RuleFactory(userParam.evaluateRuleParam)
      val rule: StatementExpr = ruleFactory.generateRule()
      val ruleAnalyzer: RuleAnalyzer = RuleAnalyzer(rule)

      // const expr value map
      val constExprValueMap = ExprValueUtil.genExprValueMap(None, ruleAnalyzer.constCacheExprs, Map[String, Any]())
      val finalConstExprValueMap = ExprValueUtil.updateExprValueMap(ruleAnalyzer.constFinalCacheExprs, constExprValueMap)

      // data connector
      val sourceDataConnector: DataConnector =
      DataConnectorFactory.getStreamingDataConnector(ssc, userParam.sourceParam,
        ruleAnalyzer.sourceRuleExprs, finalConstExprValueMap
      ) match {
        case Success(cntr) => {
          if (cntr.available) cntr
          else throw new Exception("source data connection error!")
        }
        case Failure(ex) => throw ex
      }
      val targetDataConnector: DataConnector =
        DataConnectorFactory.getStreamingDataConnector(sqlContext, userParam.targetParam,
          ruleAnalyzer.targetRuleExprs, finalConstExprValueMap
        ) match {
          case Success(cntr) => {
            if (cntr.available) cntr
            else throw new Exception("target data connection error!")
          }
          case Failure(ex) => throw ex
        }

      // get metadata
      //      val sourceMetaData: Iterable[(String, String)] = sourceDataConnector.metaData() match {
      //        case Success(md) => md
      //        case _ => throw new Exception("source metadata error!")
      //      }
      //      val targetMetaData: Iterable[(String, String)] = targetDataConnector.metaData() match {
      //        case Success(md) => md
      //        case _ => throw new Exception("target metadata error!")
      //      }

      // get data
      val sourceData: RDD[(Product, Map[String, Any])] = sourceDataConnector.data() match {
        case Success(dt) => dt
        case Failure(ex) => throw ex
      }
      val targetData: RDD[(Product, Map[String, Any])] = targetDataConnector.data() match {
        case Success(dt) => dt
        case Failure(ex) => throw ex
      }

      // accuracy algorithm
      val (accuResult, missingRdd, matchedRdd) = accuracy(sourceData, targetData, ruleAnalyzer)

      // end time
      val endTime = new Date().getTime
      persist.log(endTime, s"calculation using time: ${endTime - startTime} ms")

      // persist result
      persist.result(endTime, accuResult)
      val missingRecords = missingRdd.map(record2String(_, ruleAnalyzer.sourceRuleExprs.persistExprs, ruleAnalyzer.targetRuleExprs.persistExprs))
      persist.missRecords(missingRecords)

      // persist end time
      val persistEndTime = new Date().getTime
      persist.log(persistEndTime, s"persist using time: ${persistEndTime - endTime} ms")

      // finish
      persist.finish()

      // context stop
      sc.stop
    }
  }

}
