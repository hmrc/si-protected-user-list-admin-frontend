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
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, UpstreamErrorResponse}

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
}