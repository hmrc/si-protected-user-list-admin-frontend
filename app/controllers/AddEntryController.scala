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
import models.forms.Insert
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddEntryController @Inject() (
  views:            Views,
  backendConnector: BackendConnector,
  mcc:              MessagesControllerComponents,
  val strideAction: StrideAction
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def show(): Action[AnyContent] = StrideAction(implicit request => Ok(views.add(Insert.form)))

  def submit(): Action[AnyContent] = StrideAction.async { implicit request =>
    Insert.form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(views.add(errorForm))),
        insertModel =>
          backendConnector
            .insertNew(insertModel.toRequestJSON)
            .map(protectedUserRecord => Redirect(controllers.routes.SiProtectedUserController.view(protectedUserRecord.entryId)))
            .recover { case UpstreamErrorResponse(_, CONFLICT, _, _) =>
              Conflict(views.add(Insert.form fill insertModel withGlobalError Messages("add.error.conflict")))
            }
      )
  }
}
