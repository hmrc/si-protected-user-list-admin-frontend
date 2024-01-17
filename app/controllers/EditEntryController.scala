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
import models.request.Update
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditEntryController @Inject() (
  backendConnector: SiProtectedUserAdminBackendConnector,
  views: Views,
  mcc: MessagesControllerComponents,
  val strideAction: StrideAction
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def showEditEntryPage(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    backendConnector
      .findEntry(entryId)
      .map {
        case Some(record) => Ok(views.edit(entryId, Update.form.fill(Update from record.body)))
        case None         => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
  }

  def submit(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    Update.form
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(views.edit(entryId, errorForm))),
        update =>
          backendConnector
            .updateEntry(entryId, update.toProtectedUser)
            .map(_ => Ok(views.editSuccess()))
            .recover {
              case _: NotFoundException => NotFound(views.errorTemplate("edit.error.not.found", "edit.error.not.found", "edit.error.already.deleted"))
              case _: ConflictException => Conflict(views.edit(entryId, Update.form.fill(update).withGlobalError(Messages("edit.error.conflict"))))
            }
      )
  }
}
