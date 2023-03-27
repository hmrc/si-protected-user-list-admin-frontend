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
import connectors.SiProtectedUserListAdminConnector
import models._
import play.api.Logging
import play.api.libs.Files
import play.api.mvc._
import services.{AllowListSessionCache, DataProcessService}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{Request => _, SessionId, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.Views
import zamblauskas.csv.parser.Parser

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.BufferedSource

@Singleton
class SiProtectedUserController @Inject() (
  servicesConfig: ServicesConfig,
  allowlistSessionCache: AllowListSessionCache,
  dataProcessService: DataProcessService,
  auditConnector: AuditConnector,
  adminConnector: SiProtectedUserListAdminConnector,
  views: Views,
  mcc: MessagesControllerComponents,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Logging
    with AuthorisedFunctions {

  private lazy val showAllEnabled: Boolean = servicesConfig.getBoolean("siprotecteduser.allowlist.show.all.enabled")

  def homepage: Action[AnyContent] = Action(implicit request => Ok(views.home()))

  def reload: Action[AnyContent] = Action.async { implicit request =>
    logger.warn(
      "[GG-6801] Admin Screen - Button click: 'Add to Allowlist' / 'Clear fields'" + servicesConfig.getBoolean(
        "siprotecteduser.allowlist.shutter.service"
      )
    )
    if (!servicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")) {
      withSessionId(implicit hc => allowlistSessionCache.clear().map(_ => Ok(views.add(addAllowListForm, Nil, None))))
    } else {
      Future.successful(Ok(views.home()))
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    withSessionId { implicit hc =>
      addAllowListForm
        .bindFromRequest()
        .fold(
          formWithErrors => allowlistSessionCache.getAll().map(users => BadRequest(views.add(formWithErrors, users, None))),
          formData =>
            authorised().retrieve(Retrievals.clientId) { clientId =>
              adminConnector
                .addEntry(formData)
                .flatMap { _ =>
                  allowlistSessionCache.add(formData).map { users =>
                    auditConnector.sendEvent(AuditEvents.allowListAddEventSuccess(clientId.getOrElse("UnknownUserId"), formData))
                    logger.warn("[GG-6801] Admin Screen - Button click: 'Add to Allowlist' / 'Add' entry")
                    Ok(views.add(addAllowListForm.fill(formData.copy(username = "")), users, Some("User record saved to allowlist")))
                  }
                }
                .recoverWith { case _: ConflictException =>
                  auditConnector.sendEvent(AuditEvents.allowListAddEventFailure(clientId.getOrElse("UnknownUserId"), formData))
                  adminConnector.findEntry(formData.username) map { existingUser =>
                    Conflict(views.add(addAllowListForm.fill(formData), List(existingUser), Some("Entry not added, already exists, see below")))
                  }
                }
            }
        )
    }
  }

  def fileUploadPage(): Action[AnyContent] = Action.async { implicit request =>
    if (servicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")) {
      Future.successful(Ok(views.home()))
    } else {
      if (servicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")) {
        logger.warn("[GG-6801] Admin Screen - Button click: 'Bulk Add to Allowlist")
        Future.successful(Ok(views.fileUpload()))
      } else {
        Future.successful(ServiceUnavailable)
      }
    }
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    if (servicesConfig.getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled")) {
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
                    Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> "Only .csv files are supported. Please try again")
                  )
                case Right(allowListData) =>
                  authorised().retrieve(Retrievals.clientId) { clientId =>
                    dataProcessService.processBulkData(allowListData, csvfile.filename, clientId) match {
                      case Left((minutes, remainderSeconds, rows)) =>
                        Future.successful(Ok(views.fileUploadTime(minutes, remainderSeconds, rows)))
                      case Right(error) =>
                        Future.successful(Redirect(routes.SiProtectedUserController.fileUploadPage()).flashing("error" -> error))
                    }
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

  def sortAllAllowlistedUsers: Action[AnyContent] = Action.async { implicit request =>
    if (servicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")) {
      Future.successful(Ok(views.home()))
    } else {
      if (showAllEnabled) {
        logger.warn("[GG-6801] Admin Screen - Button click: 'List entries in Allowlist'")
        Future.successful(Ok(views.showAllSorted()))
      } else Future.successful(NotFound)
    }
  }

  def getAllAllowlist(sortByOrganisationName: Boolean = true): Action[AnyContent] = Action.async { implicit request =>
    if (showAllEnabled) {
      val rowLimit = servicesConfig.getInt("siprotecteduser.allowlist.listscreen.rowlimit")
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

  def showSearchForm(): Action[AnyContent] = Action { implicit request =>
    logger.warn("[GG-6801] Admin Screen - Button click: 'Remove From Allowlist'")
    if (servicesConfig.getBoolean("siprotecteduser.allowlist.shutter.service")) {
      Ok(views.home())
    } else {
      Ok(views.deleteForm(searchAllowListForm))
    }
  }

  def handleSearchRequest: Action[AnyContent] = Action.async { implicit request =>
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
                Ok(views.deleteConfirmation(addAllowListForm, login))
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

  def handleDeleteConfirmation(): Action[AnyContent] = Action.async { implicit request =>
    addAllowListForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          adminConnector.findEntry(formWithErrors.get.username) flatMap { allowListedUser =>
            Future.successful(BadRequest(views.deleteConfirmation(formWithErrors, allowListedUser)))
          }
        },
        formData => {
          authorised().retrieve(Retrievals.clientId) { clientId =>
            logger.warn("[GG-6801] Admin Screen - Button click: 'Remove From Allowlist' / 'Confirm' removal")
            adminConnector
              .deleteUserEntry(formData.username)
              .map { _ =>
                auditConnector.sendEvent(AuditEvents.allowListDeleteEventSuccess(clientId.getOrElse("UnknownUserId"), formData))
                Ok(views.deleteComplete())
              }
              .recover { case UpstreamErrorResponse(_, 404, _, _) =>
                auditConnector.sendEvent(AuditEvents.allowListDeleteEventFailure(clientId.getOrElse("UnknownUserId"), formData))
                NotFound(views.deleteForm(searchAllowListForm.withError("not.found", "")))
              }
          }
        }
      )
  }

  private def withSessionId(f: HeaderCarrier => Future[Result])(implicit request: Request[_]): Future[Result] = {
    val headerCarrier = hc(request)
    headerCarrier.sessionId match {
      case Some(_) =>
        f(headerCarrier)
      case None =>
        val sessionId = UUID.randomUUID().toString
        f(headerCarrier.copy(sessionId = Some(SessionId(sessionId)))).map(_.addingToSession(SessionKeys.sessionId -> sessionId))
    }
  }
}