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

package audit

import models.User
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent

trait AuditEvents {

  private val auditSource = "si-protected-user-list-admin-frontend"
  private val auditType = "ListUpdate"

  def allowListAddEventSuccess(userId: String, login: User)(implicit hc: HeaderCarrier, request: Request[_]): DataEvent =
    DataEvent(
      auditSource,
      auditType,
      detail = Map(
        "strideUserPid"  -> userId,
        "operation"      -> "add",
        "success"        -> "true",
        "orgLoginId"     -> login.username,
        "orgName"        -> login.organisationName,
        "requesterEmail" -> login.requesterEmail
      ),
      tags = hc.toAuditTags("HMRC - Login - Restricted User List - add entry", request.path) ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
    )

  def bulkUploadAuditEvent(userId: String, rowsInFile: String)(implicit hc: HeaderCarrier): DataEvent =
    DataEvent(
      auditSource,
      auditType,
      detail = Map("strideUserPid" -> userId, "operation" -> "BulkUploadOfCredentialsToAllowlist", "rowsInFile" -> rowsInFile),
      tags = hc.toAuditTags("HMRC - Login - Restricted User List - bulk update") ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse(""))
    )

  def allowListAddEventFailure(userId: String, login: User)(implicit hc: HeaderCarrier, request: Request[_]): DataEvent =
    DataEvent(
      auditSource,
      auditType,
      detail = Map(
        "strideUserPid"  -> userId,
        "operation"      -> "add",
        "success"        -> "false",
        "failureReason"  -> "Record already exists",
        "orgLoginId"     -> login.username,
        "orgName"        -> login.organisationName,
        "requesterEmail" -> login.requesterEmail
      ),
      tags = hc.toAuditTags("HMRC - Login - Restricted User List - add entry", request.path) ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
    )

  def allowListDeleteEventSuccess(userId: String, login: User)(implicit hc: HeaderCarrier, request: Request[_]): DataEvent =
    DataEvent(
      auditSource,
      auditType,
      detail = Map(
        "strideUserPid"  -> userId,
        "operation"      -> "delete",
        "success"        -> "true",
        "orgLoginId"     -> login.username,
        "orgName"        -> login.organisationName,
        "requesterEmail" -> login.requesterEmail
      ),
      tags = hc.toAuditTags("HMRC - Login - Restricted User List - delete entry", request.path) ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
    )

  def allowListDeleteEventFailure(userId: String, login: User)(implicit hc: HeaderCarrier, request: Request[_]): DataEvent =
    DataEvent(
      auditSource,
      auditType,
      detail = Map(
        "strideUserPid"  -> userId,
        "operation"      -> "delete",
        "success"        -> "false",
        "failureReason"  -> "Record did not exist",
        "orgLoginId"     -> login.username,
        "orgName"        -> login.organisationName,
        "requesterEmail" -> login.requesterEmail
      ),
      tags = hc.toAuditTags("HMRC - Login - Restricted User List - delete entry", request.path) ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
    )
}

object AuditEvents extends AuditEvents
