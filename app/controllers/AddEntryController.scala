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
import connectors.SiProtectedUserListAdminConnector
import controllers.actions.StrideAction
import models.addEntryForm
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
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
      Future.successful(Ok(views.add(addEntryForm, Nil, None)))
    } else {
      Future.successful(Ok(views.home(siProtectedUserConfig)))
    }
  }
}
