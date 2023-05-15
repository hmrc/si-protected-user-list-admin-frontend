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

import audit.AuditEvents
import config.SiProtectedUserConfig
import connectors.SiProtectedUserListAdminConnector
import controllers.actions.StrideAction
import models.InputForms.{searchAllowListForm, userForm}
import models._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc._
import services.{AllowListSessionCache, DataProcessService}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.Views
import zamblauskas.csv.parser.Parser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.BufferedSource

@Singleton
@Deprecated
class SiProtectedUserController @Inject() (siProtectedUserConfig: SiProtectedUserConfig,
                                           allowlistSessionCache: AllowListSessionCache,
                                           dataProcessService: DataProcessService,
                                           auditConnector: AuditConnector,
                                           adminConnector: SiProtectedUserListAdminConnector,
                                           views: Views,
                                           mcc: MessagesControllerComponents,
                                           strideAction: StrideAction
                                          )(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Logging
    with I18nSupport {
  private val showAllEnabled = siProtectedUserConfig.showAllEnabled

  def homepage: Action[AnyContent] = (Action andThen strideAction)(implicit request => Ok(views.home(siProtectedUserConfig)))

  def fileUploadPage(): Action[AnyContent] = (Action andThen strideAction).async { implicit request =>
    if (siProtectedUserConfig.shutterService) {
      Future.successful(Ok(views.home(siProtectedUserConfig)))
    } else {
      if (siProtectedUserConfig.bulkUploadScreenEnabled) {
        logger.warn("[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist")
        Future.successful(Ok(views.fileUpload()))
      } else {
        Future.successful(ServiceUnavailable)
      }
    }
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] =
    (Action andThen strideAction).async(parse.multipartFormData) { implicit request =>
      if (siProtectedUserConfig.bulkUploadScreenEnabled) {
        request.body
          .file("csvfile")
          .map { csvfile =>
            val contentType: Option[String] = csvfile.contentType
            val source: BufferedSource = scala.io.Source.fromFile(csvfile.ref.path.toFile)

            contentType match {
              case Some("application/octet-stream") =>
                logger.warn(s"[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: No file attached")
                Future.successful(
                  Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> "Only .csv files are supported. Please try again")
                )

              case Some(_) =>
                val lines: Either[Parser.Failure, Seq[Option[Upload]]] =
                  try {
                    Parser.parse[Option[Upload]](source.mkString)
                  } catch {
                    case _: Throwable => Right(Nil)
                  } finally {
                    source.close()
                  }
                lines match {
                  case Left(_) =>
                    logger.warn(s"[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: content format error")
                    Future.successful(
                      Redirect(routes.SiProtectedUserController.fileUploadPage())
                        .flashing(
                          "error"  -> "1. Check the header row exists AND contains the case-sensitive string: UserID,OrganisationName,RequesterEmail",
                          "error2" -> "2. Ensure all the data in the columns is ordered as per the header row",
                          "error3" -> "File upload ignored"
                        )
                    )
                  case Right(Nil) =>
                    logger.warn(s"[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: No file attached")
                    Future.successful(
                      Redirect(routes.SiProtectedUserController.fileUploadPage())
                        .flashing("error" -> "Only .csv files are supported. Please try again")
                    )
                  case Right(allowListData) =>
                    dataProcessService.processBulkData(allowListData, csvfile.filename, request.clientIdOpt) match {
                      case Left((minutes, remainderSeconds, rows)) =>
                        Future.successful(Ok(views.fileUploadTime(minutes, remainderSeconds, rows)))
                      case Right(error) =>
                        Future.successful(Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> error))
                    }
                }

              case None =>
                Future.successful(
                  Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> "Only .csv files are supported. Please try again")
                )
            }
          }
          .getOrElse {
            logger.warn(s"[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist' / 'Upload', Error: No file attached")
            Future.successful(
              Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> "Only .csv files are supported. Please try again")
            )
          }
      } else
        Future.successful(ServiceUnavailable)
    }

  def sortAllAllowlistedUsers: Action[AnyContent] = (Action andThen strideAction).async { implicit request =>
    if (siProtectedUserConfig.shutterService) {
      Future.successful(Ok(views.home(siProtectedUserConfig)))
    } else {
      if (showAllEnabled) {
        logger.warn("[GG-6801] Admin Screen - Button click: 'List entries in Allowlist'")
        Future.successful(Ok(views.showAllSorted()))
      } else Future.successful(NotFound)
    }
  }

  def getAllAllowlist(sortByOrganisationName: Boolean = true): Action[AnyContent] =
    (Action andThen strideAction).async { implicit request =>
      if (showAllEnabled) {
        val rowLimit = siProtectedUserConfig.listScreenRowLimit
        val startedTime: Long = System.currentTimeMillis()
        adminConnector.getAllEntries().map { allAllowlistedUsers =>
          val searchTime: Long = System.currentTimeMillis() - startedTime
          val allowListCount: Int = allAllowlistedUsers.size
          if (sortByOrganisationName) {
            val sortedUsers: List[User] = allAllowlistedUsers.sortBy(_.organisationName)
            logger.warn(
              s"[GG-6801] Admin Screen - Button click: 'List entries in Allowlist' / 'Count Allowlist Entries' sorted by organisation name returned $allowListCount rows, took $searchTime ms"
            )
            Ok(views.showAll(sortedUsers.take(rowLimit), allowListCount, rowLimit))
          } else {
            val sortedUsers: List[User] = allAllowlistedUsers.sortBy(_.username)
            logger.warn(
              s"[GG-6801] Admin Screen - Button click: 'List entries in Allowlist' / 'Count Allowlist Entries' sorted by user id returned $allowListCount rows, took $searchTime ms"
            )
            Ok(views.showAll(sortedUsers.take(rowLimit), allowListCount, rowLimit))
          }
        }
      } else Future.successful(NotFound)
    }

  def showSearchForm(): Action[AnyContent] =
    (Action andThen strideAction) { implicit request =>
      logger.warn("[GG-6801] Admin Screen - Button click: 'Remove From Allowlist'")
      if (siProtectedUserConfig.shutterService) {
        Ok(views.home(siProtectedUserConfig))
      } else {
        Ok(views.deleteForm(searchAllowListForm))
      }
    }

  def handleSearchRequest: Action[AnyContent] =
    (Action andThen strideAction).async { implicit request =>
      searchAllowListForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(views.deleteForm(formWithErrors))),
          formData => {
            val updatedUsername: String = formData.username.replace(" ", "")
            if (updatedUsername.length == 12) {
              adminConnector
                .findEntry(updatedUsername)
                .map { login =>
                  logger.warn("[GG-6801] Admin Screen - Button click: 'Remove From Allowlist' / 'Search'")
                  Ok(views.deleteConfirmation(userForm, login))
                }
                .recover { case UpstreamErrorResponse(_, 404, _, _) =>
                  NotFound(views.deleteForm(searchAllowListForm.withError("not.found", "")))
                }
            } else {
              Future.successful(
                BadRequest(
                  views.deleteForm(
                    searchAllowListForm
                      .fill(Search(formData.username))
                      .withError("name", "form.username.regex")
                  )
                )
              )
            }
          }
        )
    }

  def handleDeleteConfirmation(): Action[AnyContent] =
    (Action andThen strideAction).async { implicit request =>
      userForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            adminConnector.findEntry(formWithErrors.get.username) flatMap { allowListedUser =>
              Future.successful(BadRequest(views.deleteConfirmation(formWithErrors, allowListedUser)))
            }
          },
          formData => {
            logger.warn("[GG-6801] Admin Screen - Button click: 'Remove From Allowlist' / 'Confirm' removal")
            adminConnector
              .deleteUserEntry(formData.username)
              .map { _ =>
                auditConnector.sendEvent(AuditEvents.allowListDeleteEventSuccess(request.clientId, formData))
                Ok(views.deleteComplete())
              }
              .recover { case UpstreamErrorResponse(_, 404, _, _) =>
                auditConnector.sendEvent(AuditEvents.allowListDeleteEventFailure(request.clientId, formData))
                NotFound(views.deleteForm(searchAllowListForm.withError("not.found", "")))
              }
          }
        )
    }
}
