/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import audit.AuditEvents
import config.SiProtectedUserConfig
import connectors.SiProtectedUserListAdminConnector
import models.Upload
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataProcessService @Inject() (adminConnector: SiProtectedUserListAdminConnector, sc: SiProtectedUserConfig, auditConnector: AuditConnector)(implicit
  ec: ExecutionContext
) extends Logging {

  def processBulkData(allowListData: Seq[Option[Upload]], filename: String, stridePid: Option[String])(implicit
    hc: HeaderCarrier
  ): Either[(Long, Int, Int), String] = {
    val formatError = allowListData.zipWithIndex.collectFirst { case (None, index) =>
      index + 2 // to offset the index to match with the lines in the document since the header is skipped
    }
    formatError match {
      case Some(lineNumber) =>
        logger.warn(
          s"[GG-6801] Admin ScreenAdmin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: format error on line number: $lineNumber"
        )
        Right(s"CSV line number $lineNumber, invalid format. Please check data and format")
      case None =>
        val rowLimit = sc.bulkUploadRowLimit
        val entryCount = allowListData.length
        if (entryCount <= rowLimit) {
          auditConnector.sendEvent(AuditEvents.bulkUploadAuditEvent(stridePid.getOrElse("UnknownUserId"), entryCount.toString))
          val batchSize = sc.bulkUploadBatchSize
          val uploadDelay = sc.bulkUploadBatchDelaySecs
          Future {
            allowListData.grouped(batchSize).foreach { batch =>
              batch.foldLeft(Future.successful(())) { case (f, entry) =>
                f.flatMap(_ => adminConnector.updateEntryList(entry.getOrElse(throw new RuntimeException()))(hc).recover { case _ => () })
              }
              Thread.sleep(uploadDelay * 1000)
            }
          }
          val estimatedSeconds = (((entryCount / batchSize) * uploadDelay) * 1.1).toInt
          val remainderSeconds = estimatedSeconds % 60
          val minutes = Duration(estimatedSeconds, SECONDS).toMinutes
          logger.warn(s"[GG-6801] Admin ScreenAdmin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', filename: $filename")
          Left((minutes, remainderSeconds, entryCount))
        } else {
          logger.warn(
            s"[GG-6801] Admin ScreenAdmin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: Row limit exceeded Row Limit: $rowLimit"
          )
          Right(s"CSV file line limit $rowLimit exceeded. File ignored")
        }
    }
  }
}
