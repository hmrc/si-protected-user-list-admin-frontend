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

import controllers.base.{StrideAction, StrideController}
import models.Entry
import models.InputForms.entryForm
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditEntryController @Inject() (
  siProtectedUserListService: SiProtectedUserListService,
  views: Views,
  mcc: MessagesControllerComponents,
  val strideAction: StrideAction
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def showEditEntryPage(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    siProtectedUserListService
      .findEntry(entryId)
      .map {
        case Some(protectedUserRecord) => Ok(views.edit(entryForm.fill(Entry.from(protectedUserRecord))))
        case None                      => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
  }

  def submit(): Action[AnyContent] = StrideAction.async { implicit request =>
    entryForm
      .bindFromRequest()
      .fold(
        errorForm => {
          Future.successful(BadRequest(views.edit(errorForm)))
        },
        entry => {
          siProtectedUserListService
            .updateEntry(entry.copy(updatedByUser = Some(request.getUserPid), updatedByTeam = entry.addedByTeam))
            .map(_ => Ok(views.editSuccess()))
            .recover {
              case _: NotFoundException => NotFound(views.errorTemplate("edit.error.not.found", "edit.error.not.found", "edit.error.already.deleted"))
              case _: ConflictException => Conflict(views.edit(entryForm.fill(entry).withGlobalError(Messages("edit.error.conflict"))))
            }
        }
      )
  }
}
