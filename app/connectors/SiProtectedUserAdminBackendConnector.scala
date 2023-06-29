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
import models.{ProtectedUser, ProtectedUserRecord}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, HttpResponse, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import util.{Failure, Success}

@Singleton
class SiProtectedUserAdminBackendConnector @Inject() (
  backendConfig: BackendConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext) {
  private val backendUrl = backendConfig.endpoint + "/" + backendConfig.contextRoot

  def addEntry(protectedUser: ProtectedUser)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient
      .POST[ProtectedUser, ProtectedUserRecord](s"$backendUrl/add", protectedUser)
      .transform(
        identity,
        {
          case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
          case err                                 => err
        }
      )

  def updateEntry(entryId: String, protectedUser: ProtectedUser)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
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

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    httpClient
      .GET[ProtectedUserRecord](s"$backendUrl/entry-id/$entryId")
      .transform {
        case Success(user)                                => Success(Some(user))
        case Failure(UpstreamErrorResponse(_, 404, _, _)) => Success(None)
        case Failure(err)                                 => Failure(err)
      }
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    httpClient
      .DELETE[Either[UpstreamErrorResponse, HttpResponse]](url = s"$backendUrl/entry-id/$entryId")

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
}
