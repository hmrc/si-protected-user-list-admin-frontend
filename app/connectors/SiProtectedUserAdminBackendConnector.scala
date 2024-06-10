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

import config.BackendConfig
import controllers.base.StrideRequest
import models.TaxIdentifierType.{NINO, SAUTR}
import models.{ProtectedUser, ProtectedUserRecord}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SiProtectedUserAdminBackendConnector @Inject() (
  @Named("appName") appName: String,
  auditConnector: AuditConnector,
  backendConfig: BackendConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext)
    extends Logging {
  private val backendUrl = backendConfig.endpoint + "/" + backendConfig.contextRoot

  def addEntry(protectedUser: ProtectedUser)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "AddUserToProtectedUserList",
      "add user's tax ID to the protected access list"
    ) {
      httpClient
        .POST[ProtectedUser, ProtectedUserRecord](s"$backendUrl/add", protectedUser)
        .transform(
          identity,
          {
            case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
            case UpstreamErrorResponse(_, 404, _, _) => new NotFoundException("Not Found")
            case err                                 => err
          }
        )
    }

  def updateEntry(entryId: String, protectedUser: ProtectedUser)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "EditUserInProtectedUserList",
      "edit user's tax ID in the protected access list"
    ) {
      httpClient
        .PATCH[ProtectedUser, ProtectedUserRecord](s"$backendUrl/update/$entryId", protectedUser)
        .transform(
          identity,
          {
            case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
            case UpstreamErrorResponse(_, 404, _, _) => new NotFoundException("Entry to update was already deleted")
            case err                                 => err
          }
        )
    }

  private def findBy(id: String)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient.GET[ProtectedUserRecord](s"$backendUrl/entry-id/$id")

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    findBy(entryId).transform {
      case Success(user)                                => Success(Some(user))
      case Failure(UpstreamErrorResponse(_, 404, _, _)) => Success(None)
      case Failure(err)                                 => Failure(err)
    }
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier, req: StrideRequest[_]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "DeleteUserFromProtectedUserList",
      "delete record from the protected access list"
    ) {
      for {
        record <- findBy(entryId)
        _      <- httpClient.DELETE[Unit](url = s"$backendUrl/entry-id/$entryId")
      } yield record
    }

  def findEntries(teamOpt: Option[String], queryOpt: Option[String])(implicit hc: HeaderCarrier): Future[Seq[ProtectedUserRecord]] = {
    var queryString = Map(
      "filterByTeam" -> teamOpt,
      "searchQuery"  -> queryOpt
    )
      .collect { case (key, Some(value)) => s"$key=$value" }
      .mkString("&")

    if (queryString.nonEmpty) queryString = s"?$queryString"

    httpClient.GET[Seq[ProtectedUserRecord]](s"$backendUrl/record/$queryString")
  }

  private def withAuditEvent(auditType: String, transactionType: String)(
    block: => Future[ProtectedUserRecord]
  )(implicit hc: HeaderCarrier, request: StrideRequest[_]): Future[ProtectedUserRecord] =
    block.transform(
      { record =>
        val team = record.body.addedByTeam.getOrElse("-")

        auditConnector.sendExtendedEvent(
          ExtendedDataEvent(
            auditSource = appName,
            auditType = auditType,
            tags = hc.toAuditTags(s"HMRC Session Creation - SI Protected User List - $transactionType", request.path),
            detail = Json.obj(
              "pid"   -> request.getUserPid,
              "group" -> (if (record.body.group.isBlank) "-" else record.body.group),
              "team"  -> team,
              "entry" -> {
                val taxIdFields = record.body.taxId.name match {
                  case NINO  => Json.obj("nino" -> record.body.taxId.value, "sautr" -> "-")
                  case SAUTR => Json.obj("nino" -> "-", "sautr" -> record.body.taxId.value)
                }
                val idpIdFields = record.body.identityProviderId match {
                  case Some(idpID) =>
                    Json.obj(
                      "action"               -> "lock",
                      "identityProviderType" -> idpID.name,
                      "identityProviderId"   -> idpID.value
                    )
                  case None =>
                    Json.obj(
                      "action"               -> "block",
                      "identityProviderType" -> "-",
                      "identityProviderId"   -> "-"
                    )
                }
                Json.obj("id" -> record.entryId) ++ taxIdFields ++ idpIdFields
              }
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
}
