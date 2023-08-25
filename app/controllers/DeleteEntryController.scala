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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SiProtectedUserListService
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteEntryController @Inject() (
  siProtectedUserListService: SiProtectedUserListService,
  views:                      Views,
  mcc:                        MessagesControllerComponents,
  val strideAction:           StrideAction
)(implicit ec: ExecutionContext)
    extends StrideController(mcc) {

  def showConfirmDeletePage(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    siProtectedUserListService
      .findEntry(entryId)
      .map {
        case Some(protectedUser) => Ok(views.deleteConfirmation(protectedUser))
        case None                => NotFound(views.errorTemplate("error.not.found", "error.not.found", "protectedUser.details.not.found"))
      }
  }

  def delete(entryId: String): Action[AnyContent] = StrideAction.async { implicit request =>
    siProtectedUserListService
      .deleteEntry(entryId)
      .map {
        case Right(_)                                  => Ok(views.deleteSuccess())
        case Left(UpstreamErrorResponse(_, 404, _, _)) => NotFound(views.errorTemplate("delete.entry.not.found", "delete.entry.not.found", "delete.entry.already.deleted"))
        case Left(err)                                 => InternalServerError(views.somethingWentWrong())
      }
  }
}
