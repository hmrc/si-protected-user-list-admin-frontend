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

package services

import connectors.BackendConnector
import models.{Entry, ProtectedUserRecord}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SiProtectedUserListService @Inject() (backendConnector: BackendConnector) {
  def addEntry(entry: Entry)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] = {
    backendConnector.insertNew(entry.toProtectedUser())
  }

  def updateEntry(entry: Entry)(implicit hc: HeaderCarrier): Future[ProtectedUserRecord] =
    entry.entryId match {
      case Some(entryId) => backendConnector.updateBy(entryId, entry.toProtectedUser())
      case None          => Future.failed(new IllegalArgumentException("entryId not present for update"))
    }

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    backendConnector.findBy(entryId)
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HttpResponse]] = {
    backendConnector.deleteBy(entryId)
  }
}
