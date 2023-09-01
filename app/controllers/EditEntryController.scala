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
import models.forms.Update
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditEntryController @Inject() (
  backendConnector: BackendConnector,
  mcc:              MessagesControllerComponents,
  val strideAction: StrideAction,
  views:            Views
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def show(entryID: String): Action[AnyContent] = StrideAction.async { implicit request =>
    backendConnector
      .findBy(entryID)
      .map { record =>
        val update = Update(record.body.identityProviderId, record.body.group, record.body.team)
        Ok(views.edit(Update.form fill update, entryID))
      }
      .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
  }

  def submit(entryID: String): Action[AnyContent] = StrideAction.async { implicit request =>
    Update.form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(views.edit(errorForm, entryID))),
        update =>
          backendConnector
            .updateBy(entryID, update)
            .map(_ => Ok(views.editSuccess()))
            .recover {
              case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                NotFound(views.errorTemplate("edit.error.not.found", "edit.error.not.found", "edit.error.already.deleted"))
              case UpstreamErrorResponse(_, CONFLICT, _, _) =>
                Conflict(views.edit(Update.form fill update withGlobalError Messages("edit.error.conflict"), entryID))
            }
      )
  }
}
