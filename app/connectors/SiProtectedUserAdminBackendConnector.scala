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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, HttpResponse, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SiProtectedUserAdminBackendConnector @Inject() (backendConfig: BackendConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) {

  def addEntry(protectedUser: ProtectedUser)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] = {
    httpClient
      .POST[JsObject, Either[UpstreamErrorResponse, ProtectedUserRecord]](
        s"${backendConfig.endpoint}/${backendConfig.contextRoot}/add",
        Json.toJsObject(protectedUser)
      )
      .map {
        case Left(UpstreamErrorResponse(_, 409, _, _)) => throw new ConflictException("Conflict")
        case Left(err)                                 => throw err
        case Right(user)                               => user
      }
  }

  def updateEntry(entryId: String, protectedUser: ProtectedUser)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] = {
    httpClient
      .PATCH[JsObject, Either[UpstreamErrorResponse, ProtectedUserRecord]](
        s"${backendConfig.endpoint}/${backendConfig.contextRoot}/update/$entryId",
        Json.toJsObject(protectedUser)
      )
      .map {
        case Left(UpstreamErrorResponse(_, 409, _, _)) => throw new ConflictException("Conflict")
        case Left(UpstreamErrorResponse(_, 404, _, _)) => throw new NotFoundException("Entry to update was already deleted")
        case Left(err)                                 => throw err
        case Right(user)                               => user
      }
  }

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    httpClient
      .GET[Either[UpstreamErrorResponse, ProtectedUserRecord]](
        url = s"${backendConfig.endpoint}/${backendConfig.contextRoot}/entry-id/$entryId"
      )
      .flatMap {
        case Right(user)                               => Future.successful(Some(user))
        case Left(UpstreamErrorResponse(_, 404, _, _)) => Future.successful(None)
        case Left(err)                                 => Future.failed(err)
      }
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    httpClient
      .DELETE[Either[UpstreamErrorResponse, HttpResponse]](
        url = s"${backendConfig.endpoint}/${backendConfig.contextRoot}/entry-id/$entryId"
      )
  }
}
