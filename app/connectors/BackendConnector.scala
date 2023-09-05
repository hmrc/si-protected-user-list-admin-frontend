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

package connectors

import controllers.base.StrideRequest
import models.backend.ProtectedUserRecord
import models.forms.{Insert, Update}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackendConnector @Inject() (
  val auditConnector:               AuditConnector,
  @Named("backend_url") backendURL: String,
  httpClient:                       HttpClient
)(implicit ec: ExecutionContext)
    extends Logging {
  private def resource(pathSegments: String*) = backendURL +: pathSegments mkString "/"

  def findAll()(implicit hc: HeaderCarrier): Future[Seq[ProtectedUserRecord]] =
    httpClient.GET[Seq[ProtectedUserRecord]](resource("record/"))

  def insertNew(insertion: Insert)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "AddUserToProtectedUserList",
      "add user's tax ID to the protected access list"
    ) {
      httpClient.POST[Insert, ProtectedUserRecord](resource("record/"), insertion)
    }

  def findBy(entryId: String)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient.GET[ProtectedUserRecord](resource("record", entryId))

  def updateBy(entryId: String, update: Update)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "EditUserInProtectedUserList",
      "edit user's tax ID in the protected access list"
    ) {
      httpClient.PATCH[Update, ProtectedUserRecord](resource("record", entryId), update)
    }

  def deleteBy(entryId: String)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "DeleteUserFromProtectedUserList",
      "delete record from the protected access list"
    ) {
      httpClient.DELETE[ProtectedUserRecord](resource("record", entryId))
    }

  private def withAuditEvent(auditType: String, transactionType: String)(
    block: => Future[ProtectedUserRecord]
  )(implicit hc: HeaderCarrier, request: StrideRequest[_]): Future[ProtectedUserRecord] =
    block.transform(
      { record =>
        auditConnector.sendExtendedEvent(
          ExtendedDataEvent(
            auditSource = "si-protected-user-list-admin",
            auditType   = auditType,
            tags        = hc.toAuditTags(s"HMRC Session Creation - SI Protected User List - $transactionType", request.path),
            detail = Json.obj(
              "pid"   -> request.userPID,
              "group" -> notBlankOrHyphen(record.body.group),
              "team"  -> notBlankOrHyphen(record.body.team),
              "entry" -> Json.obj(
                "id"                                        -> record.entryId,
                "action"                                    -> (if (record.body.identityProviderId.isEmpty) "block" else "lock"),
                record.body.taxId.name.toString.toLowerCase -> record.body.taxId.value,
                "identityProviderType"                      -> record.body.identityProviderId.fold("-")(_.name),
                "identityProviderId"                        -> record.body.identityProviderId.fold("-")(_.value)
              )
            )
          )
        )
        record
      },
      {
        case ex @ UpstreamErrorResponse(_, statusCode, _, _) =>
          logger.error(s"[GG-7210] backend data change failed for $auditType, status code: $statusCode")
          ex
        case ex => ex
      }
    )

  private def notBlankOrHyphen(string: String) = if (string.isBlank) "-" else string
}
