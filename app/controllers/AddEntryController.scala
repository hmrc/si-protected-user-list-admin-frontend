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
import models.InputForms
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddEntryController @Inject() (
  siProtectedUserListService: SiProtectedUserListService,
  views: Views,
  mcc: MessagesControllerComponents,
  val strideAction: StrideAction,
  inputForms: InputForms
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def showAddEntryPage(): Action[AnyContent] = StrideAction(implicit request => Ok(views.add(inputForms.entryForm)))

  def submit(): Action[AnyContent] = StrideAction.async { implicit request =>
    inputForms.entryForm
      .bindFromRequest()
      .fold(
        errorForm => Future.successful(BadRequest(views.add(errorForm))),
        entry => {
          val entryWithUserId = entry.copy(addedByUser = Some(request.getUserPid))

          siProtectedUserListService
            .addEntry(entryWithUserId)
            .map(protectedUserRecord => Redirect(controllers.routes.SiProtectedUserController.view(protectedUserRecord.entryId)))
            .recover {
              case _: ConflictException => Conflict(views.add(inputForms.entryForm.fill(entry).withGlobalError(Messages("add.error.conflict"))))
              case _: NotFoundException =>
                NotFound(views.add(inputForms.entryForm.fill(entry).withError("identityProviderId", "form.identityProviderId.doesNotExist")))
            }
        }
      )
  }
}
