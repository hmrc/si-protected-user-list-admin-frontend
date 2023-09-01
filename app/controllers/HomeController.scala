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

import connectors.BackendConnector
import controllers.base.{StrideAction, StrideController}
import play.api.mvc._
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject() (
  val strideAction: StrideAction,
  backendConnector: BackendConnector,
  views:            Views,
  mcc:              MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def homepage(): Action[AnyContent] = StrideAction.async { implicit request =>
    backendConnector
      .findAll()
      .map(records => Ok(views.home(records)))
  }

  def view(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    backendConnector
      .findBy(entryId)
      .map(protectedUser => Ok(views.view(protectedUser)))
      .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
  }
}