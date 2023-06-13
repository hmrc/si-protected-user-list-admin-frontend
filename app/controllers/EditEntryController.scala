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

import config.SiProtectedUserConfig
import controllers.actions.StrideAction
import models.Entry
import models.InputForms.entryForm
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
import uk.gov.hmrc.http.{ConflictException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditEntryController @Inject() (siProtectedUserConfig: SiProtectedUserConfig,
                                     siProtectedUserListService: SiProtectedUserListService,
                                     views: Views,
                                     mcc: MessagesControllerComponents,
                                     strideAction: StrideAction
                                    )(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Logging
    with I18nSupport {

  def showEditEntryPage(entryId: String): Action[AnyContent] = (Action andThen strideAction).async { implicit request =>
    if (!siProtectedUserConfig.shutterService) {
      siProtectedUserListService
        .findEntry(entryId)
        .map {
          case Some(protectedUserRecord) => Ok(views.edit(entryForm.fill(Entry.from(protectedUserRecord))))
          case None                      => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))

        }
        .recover { case exception => InternalServerError(views.errorTemplate("error.internal_server_error", "error.internal_server_error", exception.getMessage)) }

    } else {
      Future.successful(Ok(views.home()))
    }
  }

  def submit(): Action[AnyContent] = (Action andThen strideAction).async { implicit request =>
    if (!siProtectedUserConfig.shutterService) {
      entryForm
        .bindFromRequest()
        .fold(
          errorForm => {
            Future.successful(BadRequest(views.edit(errorForm)))
          },
          entry => {
            siProtectedUserListService
              .updateEntry(entry.copy(updatedByUser = Some(request.clientId), updatedByTeam = entry.addedByTeam))
              .map(_ => Ok(views.editSuccess()))
              .recover {
                case ex: NotFoundException => NotFound(views.errorTemplate("edit.error.not.found", "edit.error.not.found", "edit.error.already.deleted"))
                case ex: ConflictException => Conflict(views.edit(entryForm.fill(entry).withGlobalError(Messages("edit.error.conflict"))))
                case exception             => InternalServerError(views.errorTemplate("error.internal_server_error", "error.internal_server_error", exception.getMessage))
              }
          }
        )
    } else {
      Future.successful(Ok(views.home()))
    }
  }
}
