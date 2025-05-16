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

package controllers

import audit.Auditor
import controllers.base.{StrideAction, StrideController, StrideRequest}
import models.{InputForms, Search}
import play.api.mvc.*
import services.SiProtectedUserListService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import views.Views

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SiProtectedUserController @Inject() (
  val strideAction: StrideAction,
  backendService: SiProtectedUserListService,
  views: Views,
  mcc: MessagesControllerComponents,
  inputForms: InputForms,
  val auditConnector: AuditConnector,
  @Named("appName") appName: String
)(implicit ec: ExecutionContext)
    extends StrideController(mcc)
    with Auditor {

  def homepage(): Action[AnyContent] = StrideAction.async { implicit request =>
    sendAuditEventWithMoreDetails(
      auditType       = "ViewProtectedUserList",
      transactionName = "HMRC - SI Protected User List Admin - Home Page - view all users in the list"
    )

    retrieveEntries(None, None)
  }

  private def retrieveEntries(filterByTeam: Option[String], searchValue: Option[String])(implicit request: StrideRequest[AnyContent]) = {
    val teamOpt = filterByTeam.filterNot(_.equalsIgnoreCase("all"))
    backendService
      .findEntries(teamOpt, searchValue)
      .map(entries => Ok(views.home(entries, inputForms.searchForm.fill(Search(filterByTeam, searchValue)))))
  }

  def search(): Action[AnyContent] = StrideAction.async { implicit request =>
    def searchProtectedUserListAuditEvent(filterByTeam: Option[String], searchQuery: Option[String]): Future[Unit] = {
      sendAuditEventWithMoreDetails(
        auditType       = "SearchProtectedUserList",
        transactionName = "HMRC - SI Protected User List Admin - Search - filter users in the list",
        moreDetails = Map(
          "filterByTeam" -> filterByTeam,
          "searchQuery"  -> searchQuery
        )
      )
    }

    inputForms.searchForm
      .bindFromRequest()
      .fold(
        errorForm => {
          searchProtectedUserListAuditEvent(errorForm.data.get("filterByTeam"), errorForm.data.get("searchQuery"))
          Future.successful(BadRequest(views.home(Seq(), errorForm)))
        },
        search => {
          searchProtectedUserListAuditEvent(search.filterByTeam, search.searchQuery)
          retrieveEntries(search.filterByTeam, search.searchQuery)
        }
      )
  }

  def view(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    backendService
      .findEntry(entryId)
      .map {
        case Some(protectedUser) => Ok(views.view(protectedUser))
        case None                => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
      .recover { case exception => InternalServerError(views.somethingWentWrong()) }
  }
}
