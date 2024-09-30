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

import connectors.SiProtectedUserAdminBackendConnector
import controllers.base.StrideRequest
import models.{Entry, ProtectedUserRecord}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SiProtectedUserListService @Inject() (
  backendConnector: SiProtectedUserAdminBackendConnector
) {
  def addEntry(entry: Entry)(implicit hc: HeaderCarrier, request: StrideRequest[?]): Future[ProtectedUserRecord] =
    backendConnector.addEntry(entry.toProtectedUser(isUpdate = false))

  def updateEntry(entryId: String, entry: Entry)(implicit hc: HeaderCarrier, request: StrideRequest[?]): Future[ProtectedUserRecord] =
    backendConnector.updateEntry(entryId, entry.toProtectedUser(isUpdate = true))

  def findEntry(entryId: String)(implicit hc: HeaderCarrier): Future[Option[ProtectedUserRecord]] = {
    backendConnector.findEntry(entryId)
  }

  def deleteEntry(entryId: String)(implicit hc: HeaderCarrier, request: StrideRequest[?]): Future[ProtectedUserRecord] =
    backendConnector.deleteEntry(entryId)

  def findEntries(teamOpt: Option[String], queryOpt: Option[String])(implicit hc: HeaderCarrier): Future[Seq[ProtectedUserRecord]] =
    backendConnector.findEntries(teamOpt, queryOpt)
}
