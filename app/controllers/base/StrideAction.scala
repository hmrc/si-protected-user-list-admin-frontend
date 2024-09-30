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

package controllers.base

import com.google.inject.name.Named
import config.StrideConfig
import play.api.Logging
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{ActionRefiner, MessagesRequest, Result, Results}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.clientId
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StrideAction @Inject() (
  @Named("appName") appName: String,
  strideConfig: StrideConfig,
  val authConnector: AuthConnector
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[MessagesRequest, StrideRequest]
    with FrontendHeaderCarrierProvider
    with AuthorisedFunctions
    with Logging {
  def refine[A](request: MessagesRequest[A]): Future[Either[Result, StrideRequest[A]]] = {
    implicit val req: MessagesRequest[A] = request

    val hasPrivilegedApp = AuthProviders(PrivilegedApplication)
    val hasAnyOfRequiredRoles = strideConfig.strideEnrolments.reduceOption[Predicate](_ or _) getOrElse EmptyPredicate

    authorised(hasPrivilegedApp and hasAnyOfRequiredRoles)
      .retrieve(clientId) { userPidOpt =>
        logger.debug("User Authenticated with Stride auth")
        Future.successful(Right(StrideRequest(request, userPidOpt)))
      }
      .recover {
        case _: InsufficientEnrolments =>
          val msg = "Failed Stride Auth - InsufficientEnrolments"
          logger.info(msg)
          Left(Unauthorized(msg))
        case _: NoActiveSession =>
          logger.info("Failed Stride Auth - NoActiveSession")

          val protocol = if (request.secure) "https" else "http"
          val strideSuccessUrl = s"$protocol://${request.host}${request.path}"

          Left(
            Redirect(
              strideConfig.strideLoginBaseUrl,
              Map(
                "successURL" -> Seq(strideSuccessUrl),
                "origin"     -> Seq(appName)
              )
            )
          )
      }
  }
}
object StrideAction {
  class Shuttered @Inject() (
    @Named("appName") appName: String,
    strideConfig: StrideConfig,
    authConnector: AuthConnector,
    shutterView: views.html.Shutter
  )(implicit ec: ExecutionContext)
      extends StrideAction(appName, strideConfig, authConnector)
      with Results {
    override def refine[A](request: MessagesRequest[A]): Future[Either[Result, StrideRequest[A]]] =
      super.refine(request) map { resultOrStrideRequest =>
        resultOrStrideRequest flatMap { strideRequest => Left(ServiceUnavailable(shutterView()(strideRequest, request.messages))) }
      }
  }
}
