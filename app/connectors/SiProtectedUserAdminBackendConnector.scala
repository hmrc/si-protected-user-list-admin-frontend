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
import models.{CredIdNotFoundException, ProtectedUser, ProtectedUserRecord}
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpResponse, NotFoundException, StringContextOps, UpstreamErrorResponse}
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
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging {
  private val backendUrl = backendConfig.endpoint + "/" + backendConfig.contextRoot

  def addEntry(protectedUser: ProtectedUser)(implicit hc: HeaderCarrier, req: StrideRequest[?]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "AddUserToProtectedUserList",
      "Add User",
      "add user to the list"
    ) {
      httpClient
        .post(url"$backendUrl/add")
        .withBody(Json.toJson(protectedUser))
        .execute[ProtectedUserRecord]
        .transform(
          identity,
          {
            case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
            case UpstreamErrorResponse(_, 404, _, _) => new NotFoundException("Not Found")
            case err                                 => err
          }
        )
    }

  def updateEntry(entryId: String, protectedUser: ProtectedUser)(implicit hc: HeaderCarrier, req: StrideRequest[?]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "EditUserInProtectedUserList",
      "Edit User",
      "edit user in the list"
    ) {
      httpClient
        .patch(url"$backendUrl/update/$entryId")
        .withBody(Json.toJson(protectedUser))
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK                                                               => httpResponse.json.as[ProtectedUserRecord]
            case CONFLICT                                                         => throw new ConflictException("Conflict")
            case NOT_FOUND if httpResponse.body.contains("CREDID_DOES_NOT_EXIST") => throw CredIdNotFoundException // GG-7967
            case NOT_FOUND => throw new NotFoundException("Entry to update was already deleted")
            case status    => throw UpstreamErrorResponse(httpResponse.body, status)
          }
        }
    }

  private def findBy(id: String)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient.get(url"$backendUrl/entry-id/$id").execute[ProtectedUserRecord]

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    findBy(entryId).transform {
      case Success(user)                                => Success(Some(user))
      case Failure(UpstreamErrorResponse(_, 404, _, _)) => Success(None)
      case Failure(err)                                 => Failure(err)
    }
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier, req: StrideRequest[?]): Future[ProtectedUserRecord] =
    withAuditEvent(
      "DeleteUserFromProtectedUserList",
      "Delete User",
      "delete user from the list"
    ) {
      for {
        record <- findBy(entryId)
        _      <- httpClient.delete(url"$backendUrl/entry-id/$entryId").execute[Unit]
      } yield record
    }

  def findEntries(teamOpt: Option[String], queryOpt: Option[String])(implicit hc: HeaderCarrier): Future[Seq[ProtectedUserRecord]] = {
    val queryString = Map(
      "filterByTeam" -> teamOpt,
      "searchQuery"  -> queryOpt
    )
      .collect { case (key, Some(value)) => s"$key=$value" }
      .mkString("&")

    val url = if (queryString.isEmpty) url"$backendUrl/record/" else url"$backendUrl/record/?$queryString"

    httpClient.get(url).execute[Seq[ProtectedUserRecord]]
  }

  private def withAuditEvent(auditType: String, action: String, transactionType: String)(
    block: => Future[ProtectedUserRecord]
  )(implicit hc: HeaderCarrier, request: StrideRequest[?]): Future[ProtectedUserRecord] =
    block.transform(
      { record =>
        val team = record.body.addedByTeam.getOrElse("-")

        auditConnector.sendExtendedEvent(
          ExtendedDataEvent(
            auditSource = appName,
            auditType   = auditType,
            tags        = hc.toAuditTags(s"HMRC - SI Protected User List Admin - $action - $transactionType", request.path),
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
