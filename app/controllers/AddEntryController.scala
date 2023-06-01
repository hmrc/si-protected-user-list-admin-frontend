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
import models.InputForms.entryForm
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
import uk.gov.hmrc.http.ConflictException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddEntryController @Inject() (siProtectedUserConfig: SiProtectedUserConfig,
                                    siProtectedUserListService: SiProtectedUserListService,
                                    views: Views,
                                    mcc: MessagesControllerComponents,
                                    strideAction: StrideAction
                                   )(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Logging
    with I18nSupport {

  def showAddEntryPage(): Action[AnyContent] = (Action andThen strideAction).async { implicit request =>
    if (!siProtectedUserConfig.shutterService) {
      Future.successful(Ok(views.add(entryForm)))
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
            Future.successful(BadRequest(views.add(errorForm)))
          },
          entry => {
            val entryWithUserId = entry.copy(addedByUser = Some(request.getUserPid))
            siProtectedUserListService
              .addEntry(entryWithUserId)
              .map(protectedUserRecord => Redirect(controllers.routes.SiProtectedUserController.view(protectedUserRecord.entryId)))
              .recoverWith { case _: ConflictException =>
                Future.successful(Conflict(views.add(entryForm.fill(entry).withGlobalError(Messages("add.error.conflict")))))
              }
          }
        )
    } else {
      Future.successful(Ok(views.home()))
    }
  }
}
