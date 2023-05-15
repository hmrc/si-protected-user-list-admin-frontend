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

import com.google.inject.name.Named
import models.{Upload, User}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{ConflictException, HeaderCarrier, HttpClient, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
@Deprecated
class SiProtectedUserListAdminConnector @Inject() (@Named("siProtectedUserBackendEndpoint") serviceUrl: String, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) {
  private val rootPath = "si-protected-user-list-admin"

  def addEntry(login: User)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .POST[JsObject, Either[UpstreamErrorResponse, Unit]](
        s"$serviceUrl/$rootPath/add",
        Json.obj(
          "username"         -> login.username,
          "organisationName" -> login.organisationName.replaceAll("[\b\r\n\t]+", ", ").trim,
          "requesterEmail"   -> login.requesterEmail
        )
      )
      .map {
        case Left(UpstreamErrorResponse(_, 409, _, _)) => throw new ConflictException("Conflict")
        case Left(err)                                 => throw err
        case Right(_)                                  => ()
      }

  def findEntry(username: String)(implicit hc: HeaderCarrier): Future[User] =
    httpClient.GET[Either[UpstreamErrorResponse, User]](s"$serviceUrl/$rootPath/find/$username").map {
      case Left(err)       => throw err
      case Right(response) => response
    }

  def updateEntryList(upload: Upload)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .POST[JsObject, Either[UpstreamErrorResponse, Unit]](
        s"$serviceUrl/$rootPath/insert-update",
        Json.obj(
          "username"         -> upload.username,
          "organisationName" -> upload.organisationName,
          "requesterEmail"   -> upload.requesterEmail
        )
      )
      .map {
        case Left(err) => throw err
        case Right(_)  => ()
      }

  def getAllEntries()(implicit hc: HeaderCarrier): Future[List[User]] =
    httpClient.GET[Either[UpstreamErrorResponse, List[User]]](s"$serviceUrl/$rootPath/retrieve-all").map {
      case Left(err)       => throw err
      case Right(response) => response
    }

  def deleteUserEntry(username: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.DELETE[Either[UpstreamErrorResponse, Unit]](s"$serviceUrl/$rootPath/delete/$username").map {
      case Left(err) => throw err
      case Right(_)  => ()
    }
}
