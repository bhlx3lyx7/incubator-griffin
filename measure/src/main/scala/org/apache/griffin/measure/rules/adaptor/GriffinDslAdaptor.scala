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

import org.apache.griffin.measure.config.params.user.RuleParam
import org.apache.griffin.measure.rules.dsl._
import org.apache.griffin.measure.rules.dsl.analyzer._
import org.apache.griffin.measure.rules.dsl.expr._
import org.apache.griffin.measure.rules.dsl.parser.GriffinDslParser
import org.apache.griffin.measure.rules.step._

case class GriffinDslAdaptor(dataSourceNames: Seq[String], functionNames: Seq[String]) extends RuleAdaptor {

  object StepInfo {
    val _Name = "name"
    val _PersistType = "persist.type"
    def getNameOpt(param: Map[String, Any]): Option[String] = param.get(_Name).flatMap(a => Some(a.toString))
    def getPersistType(param: Map[String, Any]): PersistType = PersistType(param.getOrElse(_PersistType, "").toString)
  }
  object AccuracyInfo {
    val _Source = "source"
    val _Target = "target"
    val _MissRecords = "miss.records"
    val _Accuracy = "accuracy"
    val _Miss = "miss"
    val _Total = "total"
    val _Matched = "matched"
  }
  object ProfilingInfo {
    val _Source = "source"
    val _Profiling = "profiling"
  }

  def getNameOpt(param: Map[String, Any], key: String): Option[String] = param.get(key).flatMap(a => Some(a.toString))
  def resultName(param: Map[String, Any], key: String): String = {
    val nameOpt = param.get(key) match {
      case Some(prm: Map[String, Any]) => StepInfo.getNameOpt(prm)
      case _ => None
    }
    nameOpt.getOrElse(key)
  }
  def resultPersistType(param: Map[String, Any], key: String, defPersistType: PersistType): PersistType = {
    param.get(key) match {
      case Some(prm: Map[String, Any]) => StepInfo.getPersistType(prm)
      case _ => defPersistType
    }
  }

  val _dqType = "dq.type"

  protected def getDqType(param: Map[String, Any]) = DqType(param.getOrElse(_dqType, "").toString)

  val filteredFunctionNames = functionNames.filter { fn =>
    fn.matches("""^[a-zA-Z_]\w*$""")
  }
  val parser = GriffinDslParser(dataSourceNames, filteredFunctionNames)

  def genRuleStep(param: Map[String, Any]): Seq[RuleStep] = {
    GriffinDslStep(getName(param), getRule(param), getDqType(param), getDetails(param)) :: Nil
  }

  def getTempSourceNames(param: Map[String, Any]): Seq[String] = {
    val dqType = getDqType(param)
    param.get(_name) match {
      case Some(name) => {
        dqType match {
          case AccuracyType => {
            Seq[String](
              resultName(param, AccuracyInfo._MissRecords),
              resultName(param, AccuracyInfo._Accuracy)
            )
          }
          case ProfilingType => {
            Seq[String](
              resultName(param, ProfilingInfo._Profiling)
            )
          }
          case TimelinessType => {
            Nil
          }
          case _ => Nil
        }
      }
      case _ => Nil
    }
  }

  def adaptConcreteRuleStep(ruleStep: RuleStep): Seq[ConcreteRuleStep] = {
    ruleStep match {
      case rs @ GriffinDslStep(_, rule, dqType, _) => {
        val exprOpt = try {
          val result = parser.parseRule(rule, dqType)
          if (result.successful) Some(result.get)
          else {
            warn(s"adapt concrete rule step warn: parse rule [ ${rule} ] fails")
            None
          }
        } catch {
          case e: Throwable => {
            error(s"adapt concrete rule step error: ${e.getMessage}")
            None
          }
        }

        exprOpt match {
          case Some(expr) => {
            try {
              transConcreteRuleSteps(rs, expr)
            } catch {
              case e: Throwable => {
                error(s"trans concrete rule step error: ${e.getMessage}")
                Nil
              }
            }
          }
          case _ => Nil
        }
      }
      case _ => Nil
    }
  }

  private def transConcreteRuleSteps(ruleStep: GriffinDslStep, expr: Expr): Seq[ConcreteRuleStep] = {
    val details = ruleStep.details
    ruleStep.dqType match {
      case AccuracyType => {
        val sourceName = getNameOpt(details, AccuracyInfo._Source) match {
          case Some(name) => name
          case _ => dataSourceNames.head
        }
        val targetName = getNameOpt(details, AccuracyInfo._Target) match {
          case Some(name) => name
          case _ => dataSourceNames.tail.head
        }
        val analyzer = AccuracyAnalyzer(expr.asInstanceOf[LogicalExpr], sourceName, targetName)

        // 1. miss record
        val missRecordsSql = {
//          val selClause = analyzer.selectionExprs.map { sel =>
//            val alias = sel.alias match {
//              case Some(a) => s" AS ${a}"
//              case _ => ""
//            }
//            s"${sel.desc}${alias}"
//          }.mkString(", ")
          val selClause = s"`${sourceName}`.*"

          val onClause = expr.coalesceDesc

          val sourceIsNull = analyzer.sourceSelectionExprs.map { sel =>
            s"${sel.desc} IS NULL"
          }.mkString(" AND ")
          val targetIsNull = analyzer.targetSelectionExprs.map { sel =>
            s"${sel.desc} IS NULL"
          }.mkString(" AND ")
          val whereClause = s"(NOT (${sourceIsNull})) AND (${targetIsNull})"

          s"SELECT ${selClause} FROM `${sourceName}` LEFT JOIN `${targetName}` ON ${onClause} WHERE ${whereClause}"
        }
        val missRecordsName = resultName(details, AccuracyInfo._MissRecords)
        val missRecordsStep = SparkSqlStep(
          missRecordsName,
          missRecordsSql,
          resultPersistType(details, AccuracyInfo._MissRecords, RecordPersistType)
        )

        // 2. miss count
        val missTableName = "_miss_"
        val missColName = getNameOpt(details, AccuracyInfo._Miss).getOrElse(AccuracyInfo._Miss)
        val missSql = {
          s"SELECT COUNT(*) AS `${missColName}` FROM `${missRecordsName}`"
        }
        val missStep = SparkSqlStep(
          missTableName,
          missSql,
          NonePersistType
        )

        // 3. total count
        val totalTableName = "_total_"
        val totalColName = getNameOpt(details, AccuracyInfo._Total).getOrElse(AccuracyInfo._Total)
        val totalSql = {
          s"SELECT COUNT(*) AS `${totalColName}` FROM `${sourceName}`"
        }
        val totalStep = SparkSqlStep(
          totalTableName,
          totalSql,
          NonePersistType
        )

        // 4. accuracy metric
        val matchedColName = getNameOpt(details, AccuracyInfo._Matched).getOrElse(AccuracyInfo._Matched)
        val accuracyMetricSql = {
          s"""
             |SELECT `${totalTableName}`.`${totalColName}` AS `${totalColName}`,
             |`${missTableName}`.`${missColName}` AS `${missColName}`,
             |(`${totalColName}` - `${missColName}`) AS `${matchedColName}`
             |FROM `${totalTableName}` JOIN `${missTableName}`
           """.stripMargin
        }
        val accuracyMetricName = resultName(details, AccuracyInfo._Accuracy)
        val accuracyMetricStep = SparkSqlStep(
          accuracyMetricName,
          accuracyMetricSql,
          resultPersistType(details, AccuracyInfo._Accuracy, MetricPersistType)
        )

        missRecordsStep :: missStep :: totalStep :: accuracyMetricStep :: Nil
      }
      case ProfilingType => {
        val sourceName = getNameOpt(details, ProfilingInfo._Source) match {
          case Some(name) => name
          case _ => dataSourceNames.head
        }
        val analyzer = ProfilingAnalyzer(expr.asInstanceOf[Expressions], sourceName)

        // 1. select statement
        val profilingSql = {
          val selClause = analyzer.selectionExprs.map { sel =>
            val alias = sel match {
              case s: AliasableExpr if (s.alias.nonEmpty) => s" AS ${s.alias.get}"
              case _ => ""
            }
            s"${sel.desc}${alias}"
          }.mkString(", ")

          s"SELECT ${selClause} FROM ${sourceName}"
        }
        val profilingMetricName = resultName(details, ProfilingInfo._Profiling)
        val profilingStep = SparkSqlStep(
          profilingMetricName,
          profilingSql,
          resultPersistType(details, ProfilingInfo._Profiling, RecordPersistType)
        )

        profilingStep :: Nil
      }
      case TimelinessType => {
        Nil
      }
      case _ => Nil
    }
  }

}