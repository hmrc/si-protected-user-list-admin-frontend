/*
 * Copyright 2025 HM Revenue & Customs
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

package audit

import controllers.base.StrideRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier

import scala.concurrent.ExecutionContext
trait Auditor {

  def auditConnector: AuditConnector

  def sendAuditEventWithMoreDetails(auditType: String, transactionName: String, moreDetails: Map[String, Option[String]] = Map.empty)(implicit
    request: StrideRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ) = {
    val details = Map(
      "pid"  -> Some(request.getUserPid),
      "name" -> request.nameOpt.map(name => s"${name.name.getOrElse("")} ${name.lastName.getOrElse("")}".trim)
    )

    val dataEvent = DataEvent(
      auditSource = "si-protected-user-list-admin-frontend",
      auditType   = auditType,
      tags        = hc.toAuditTags(transactionName, request.path),
      detail = (details ++ moreDetails) map {
        case (key, Some(value)) if value.nonEmpty => key -> value
        case (key, _)                             => key -> "-"
      }
    )

    auditConnector.sendEvent(dataEvent).map(_ => ())
  }

}
