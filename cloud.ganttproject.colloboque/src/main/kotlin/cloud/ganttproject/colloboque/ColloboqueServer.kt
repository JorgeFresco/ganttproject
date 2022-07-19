/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package cloud.ganttproject.colloboque

import biz.ganttproject.core.io.XmlTasks
import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.io.walkTasksDepthFirst
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.*
import org.jooq.*
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import java.sql.Connection
import java.text.DateFormat
import java.util.*
import java.time.LocalDateTime
import java.util.concurrent.Executors

internal typealias ProjectRefid = String
internal typealias BaseTxnId = String

class ColloboqueServerException: Exception {
  constructor(message: String): super(message)
  constructor(message: String, cause: Throwable): super(message, cause)
}

class ColloboqueServer(
  private val initProject: (projectRefid: String) -> Unit,
  private val connectionFactory: (projectRefid: String) -> Connection,
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<InputXlog>,
  private val serverResponseChannel: Channel<String>) {
  private val refidToBaseTxnId: MutableMap<ProjectRefid, ProjectRefid> = mutableMapOf()

  private val wsCommunicationScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

  init {
    wsCommunicationScope.launch {
      processUpdatesLoop()
    }
  }

  fun init(projectRefid: ProjectRefid, projectXml: String? = null): BaseTxnId {
    try {
      initProject(projectRefid)
      connectionFactory(projectRefid).use {
        if (projectXml != null) {
          DSL.using(it, SQLDialect.POSTGRES)
            .configuration()
            .deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
            .dsl().let { dsl ->
              loadProject(projectXml, dsl)
            }
        }
        // TODO: get from the database
        return "0".also {
          refidToBaseTxnId[projectRefid] = it
        }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to init project $projectRefid", e)
    }
  }

  private suspend fun processUpdatesLoop() {
    for (inputXlog in updateInputChannel) {
      try {
        val newBaseTxnId = applyXlog(inputXlog.projectRefid, inputXlog.baseTxnId, inputXlog.transactions[0])
          ?: continue
        val response = ServerCommitResponse(
          inputXlog.baseTxnId,
          newBaseTxnId,
          inputXlog.projectRefid,
          SERVER_COMMIT_RESPONSE_TYPE
        )
        serverResponseChannel.send(Json.encodeToString(response))
      } catch (e: Exception) {
        LOG.error("Failed to commit\n {}", inputXlog, exception = e)
        val errorResponse = ServerCommitError(
          inputXlog.baseTxnId,
          inputXlog.projectRefid,
          e.message.orEmpty(),
          SERVER_COMMIT_ERROR_TYPE
        )
        serverResponseChannel.send(Json.encodeToString(errorResponse))
      }
    }
  }

  /**
   * Performs transaction commit if `baseTxnId` corresponds to the value hold by the server.
   * Returns new baseTxnId on success.
   */
  private fun applyXlog(projectRefid: ProjectRefid, baseTxnId: String, transaction: XlogRecord): String? {
    if (transaction.sqlStatements.isEmpty()) throw ColloboqueServerException("Empty transactions not allowed")
    if (getBaseTxnId(projectRefid) != baseTxnId) throw ColloboqueServerException("Invalid transaction id $baseTxnId")
    try {
      connectionFactory(projectRefid).use { connection ->
        return DSL
          .using(connection, SQLDialect.POSTGRES)
          .transactionResult { config ->
            val context = config.dsl()
            transaction.sqlStatements.forEach { context.execute(it) }
            generateNextTxnId(projectRefid, baseTxnId, transaction)
            // TODO: update transaction id in the database
          }.also { refidToBaseTxnId[projectRefid] = it }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to commit transaction", e)
    }
  }

  private fun applyTransactionJSON(projectRefid: ProjectRefid, baseTxnId: String, transactionJson: JsonArray): String? {
    if (transactionJson.isEmpty()) throw ColloboqueServerException("Empty transactions not allowed")
    if (getBaseTxnId(projectRefid) != baseTxnId) throw ColloboqueServerException("Invalid transaction id $baseTxnId")

    try {
      connectionFactory(projectRefid).use { connection ->
        return DSL
          .using(connection, SQLDialect.POSTGRES)
          .transactionResult { config ->
            val context = config.dsl()
            transactionJson.forEach { operationJson ->
              val operationDto = Json.decodeFromJsonElement(OperationDto.serializer(), operationJson)
              applyOperation(context, operationDto)
            }
            return@transactionResult LocalDateTime.now().toString()
          }.also { refidToBaseTxnId[projectRefid] = it }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to commit transaction", e)
    }
  }
  private fun applyOperation(context: DSLContext, operation: OperationDto): Int {
    return when (operation) {
      is OperationDto.InsertOperationDto -> {
        val table = DSL.table(operation.tableName.lowercase())
        context
          .insertInto(table)
          .set(operation.values)
          .execute()
      }
      is OperationDto.DeleteOperationDto -> {
        val binaryCondition = buildBinaryCondition(operation.deleteBinaryConditions)
        val rangeCondition = buildRangeCondition(operation.deleteRangeConditions)
        val table = DSL.table(operation.tableName.lowercase())
        context
          .deleteFrom(table)
          .where(binaryCondition).and(rangeCondition)
          .execute()
      }
      is OperationDto.UpdateOperationDto -> {
        val table = DSL.table(operation.tableName.lowercase())
        val binaryCondition = buildBinaryCondition(operation.updateBinaryConditions)
        val rangeCondition = buildRangeCondition(operation.updateRangeConditions)
        context
          .update(table)
          .set(operation.newValues)
          .where(binaryCondition).and(rangeCondition)
          .execute()
      }
      is OperationDto.MergeOperationDto -> {
        val table = DSL.table(operation.tableName.lowercase())
        val binaryCondition = buildBinaryCondition(operation.mergeBinaryConditions)
        val rangeCondition = buildRangeCondition(operation.mergeRangeConditions)
        context.mergeInto(table).using(DSL.selectOne())
          .on(binaryCondition).and(rangeCondition)
          .whenMatchedThenUpdate().set(operation.whenMatchedThenUpdate)
          .whenNotMatchedThenInsert().set(operation.whenNotMatchedThenInsert)
          .execute()
      }
    }
  }

  private fun buildBinaryCondition(conditionsList: List<Triple<String, BinaryPred, String>>): Condition {
    var result: Condition = DSL.trueCondition()
    for ((column, pred, value) in conditionsList) {
      val field = DSL.field(column)
      val condition = when (pred) {
        BinaryPred.EQ -> field.eq(value)
        BinaryPred.GT -> field.gt(value)
        BinaryPred.LT -> field.lt(value)
        BinaryPred.LE -> field.le(value)
        BinaryPred.GE -> field.ge(value)
      }
      result = result.and(condition)
    }
    return result
  }

  private fun buildRangeCondition(conditionsList: List<Triple<String, RangePred, List<String>>>): Condition {
    var result: Condition = DSL.trueCondition()
    for ((column, pred, values) in conditionsList) {
      val field = DSL.field(column)
      val condition = when (pred) {
        RangePred.IN -> field.`in`(values)
        RangePred.NOT_IN -> field.notIn(values)
      }
      result = result.and(condition)
    }
    return result
  }


  fun getBaseTxnId(projectRefid: ProjectRefid) = refidToBaseTxnId[projectRefid]

  // TODO
  private fun generateNextTxnId(projectRefid: ProjectRefid, oldTxnId: String, transaction: XlogRecord): String {
    return LocalDateTime.now().toString()
  }
}


private fun loadProject(xmlInput: String, dsl: DSLContext) {
  object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return Locale.US
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        }
      })
    }
  }
  val bufferProject = GanttProjectImpl()
  val taskLoader = TaskLoader(bufferProject.taskManager, SimpleTreeCollapseView())
  parseXmlProject(xmlInput).let { xmlProject ->
    taskLoader.loadTaskCustomPropertyDefinitions(xmlProject)
    xmlProject.walkTasksDepthFirst { parent: XmlTasks.XmlTask?, child: XmlTasks.XmlTask ->
      taskLoader.loadTask(parent, child)
      true
    }
  }
  bufferProject.taskManager.tasks.forEach { task ->
    buildInsertTaskQuery(dsl, task).execute()
  }

}

private val LOG = GPLogger.create("ColloboqueServer")