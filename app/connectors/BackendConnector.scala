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
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, HttpResponse, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackendConnector @Inject() (
  backendURL: BackendConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext) {
  def findAll()(implicit hc: HeaderCarrier): Future[Seq[ProtectedUserRecord]] =
    httpClient.GET[Seq[ProtectedUserRecord]](backendURL("record/"))

  def insertNew(protectedUser: JsValue)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient
      .POST[JsValue, ProtectedUserRecord](backendURL("record/"), protectedUser)
      .transform(
        identity,
        {
          case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
          case err                                 => err
        }
      )

  def findBy(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] =
    httpClient
      .GET[ProtectedUserRecord](backendURL("record", entryId))
      .map(Some.apply)
      .recover { case UpstreamErrorResponse(_, 404, _, _) => None }

  def updateBy(entryId: String, protectedUser: ProtectedUser)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    httpClient
      .PATCH[ProtectedUser, ProtectedUserRecord](backendURL("record", entryId), protectedUser)
      .transform(
        identity,
        {
          case UpstreamErrorResponse(_, 404, _, _) => new NotFoundException("Entry to update was already deleted")
          case UpstreamErrorResponse(_, 409, _, _) => new ConflictException("Conflict")
          case err                                 => err
        }
      )

  def deleteBy(entryId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] =
    httpClient
      .DELETE[Either[UpstreamErrorResponse, HttpResponse]](backendURL("record", entryId))
}
