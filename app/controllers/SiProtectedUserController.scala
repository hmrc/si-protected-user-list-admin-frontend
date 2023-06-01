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

import controllers.base.StrideController
import play.api.mvc._
import services.SiProtectedUserListService
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SiProtectedUserController @Inject() (
  siProtectedUserListService: SiProtectedUserListService,
  homeView: views.html.Home,
  views: Views,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def homepage(): Action[AnyContent] = StrideAction(implicit request => Ok(homeView()))

  def view(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    siProtectedUserListService
      .findEntry(entryId)
      .map {
        case Some(protectedUser) => Ok(views.view(protectedUser))
        case None                => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
      .recover { case exception => InternalServerError(views.errorTemplate("error.internal_server_error", "error.internal_server_error", exception.getMessage)) }
  }
}
