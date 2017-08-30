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
package org.apache.griffin.measure.rules.adaptor

import org.apache.griffin.measure.config.params.user.{EvaluateRuleParam, RuleParam}
import org.apache.griffin.measure.rules.dsl._
import org.apache.griffin.measure.rules.step._
import org.apache.spark.sql.SQLContext

import scala.collection.mutable.{Map => MutableMap}

object RuleAdaptorGroup {

  val _dslType = "dsl.type"

  var dataSourceNames: Seq[String] = _
  var functionNames: Seq[String] = _

  def init(sqlContext: SQLContext, dsNames: Seq[String]): Unit = {
    val functions = sqlContext.sql("show functions")
    functionNames = functions.map(_.getString(0)).collect
    dataSourceNames = dsNames
  }

  private def getDslType(param: Map[String, Any], defDslType: DslType) = {
    val dt = DslType(param.getOrElse(_dslType, "").toString)
    dt match {
      case UnknownDslType => defDslType
      case _ => dt
    }
  }

  private def genRuleAdaptor(dslType: DslType, dataSourceNames: Seq[String]): Option[RuleAdaptor] = {
    dslType match {
      case SparkSqlType => Some(SparkSqlAdaptor())
      case DfOprType => Some(DataFrameOprAdaptor())
      case GriffinDslType => Some(GriffinDslAdaptor(dataSourceNames, functionNames))
      case _ => None
    }
  }

//  def genRuleSteps(evaluateRuleParam: EvaluateRuleParam): Seq[RuleStep] = {
//    val dslTypeStr = if (evaluateRuleParam.dslType == null) "" else evaluateRuleParam.dslType
//    val defaultDslType = DslType(dslTypeStr)
//    val rules = evaluateRuleParam.rules
//    var dsNames = dataSourceNames
//    val steps = rules.flatMap { param =>
//      val dslType = getDslType(param)
//      genRuleAdaptor(dslType) match {
//        case Some(ruleAdaptor) => ruleAdaptor.genRuleStep(param)
//        case _ => Nil
//      }
//    }
//    steps.foreach(println)
//    steps
//  }

  def genConcreteRuleSteps(evaluateRuleParam: EvaluateRuleParam): Seq[ConcreteRuleStep] = {
    val dslTypeStr = if (evaluateRuleParam.dslType == null) "" else evaluateRuleParam.dslType
    val defaultDslType = DslType(dslTypeStr)
    val ruleParams = evaluateRuleParam.rules
    val (steps, dsNames) = ruleParams.foldLeft((Seq[ConcreteRuleStep](), dataSourceNames)) { (res, param) =>
      val (preSteps, preNames) = res
      val dslType = getDslType(param, defaultDslType)
      val (curSteps, curNames) = genRuleAdaptor(dslType, preNames) match {
        case Some(ruleAdaptor) => (ruleAdaptor.genConcreteRuleStep(param), preNames ++ ruleAdaptor.getTempSourceNames(param))
        case _ => (Nil, preNames)
      }
      (preSteps ++ curSteps, curNames)
    }
    steps
  }



}