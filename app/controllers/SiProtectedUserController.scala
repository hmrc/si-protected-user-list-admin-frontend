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

import connectors.SiProtectedUserAdminBackendConnector
import controllers.base.{StrideAction, StrideController}
import play.api.mvc._
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SiProtectedUserController @Inject() (
  val strideAction: StrideAction,
  backendConnector: SiProtectedUserAdminBackendConnector,
  views: Views,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def homepage(filterByTeam: Option[String], searchQuery: Option[String]): Action[AnyContent] = StrideAction.async { implicit request =>
    val teamOpt = filterByTeam filterNot "all".equalsIgnoreCase
    val queryOpt = searchQuery filter (_.nonEmpty)

    backendConnector
      .findEntries(teamOpt, queryOpt)
      .map(entries => Ok(views.home(entries, teamOpt, queryOpt)))
  }

  def view(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    backendConnector
      .findEntry(entryId)
      .map {
        case Some(protectedUser) => Ok(views.view(protectedUser))
        case None                => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
      .recover { case exception => InternalServerError(views.errorTemplate("error.internal_server_error", "error.internal_server_error", exception.getMessage)) }
  }
}
