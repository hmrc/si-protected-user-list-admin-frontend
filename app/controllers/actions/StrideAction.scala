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

package controllers.actions

import config.AppConfig
import play.api.Logging
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.clientId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StrideAction @Inject() (val authConnector: AuthConnector, appConfig: AppConfig)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[Request, StrideRequest]
    with AuthorisedFunctions
    with Logging {
  private lazy val strideLoginUrl: String = s"${appConfig.strideLoginBaseUrl}/stride/sign-in"
  private lazy val strideSuccessUrl: String = appConfig.strideSuccessUrl

  def refine[A](request: Request[A]): Future[Either[Result, StrideRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val hasRequiredRole = (appConfig.strideEnrolments fold EmptyPredicate)(_ or _)

    authorised(hasRequiredRole)
      .retrieve(clientId) { clientIdOpt =>
        logger.debug("User Authenticated with Stride auth")
        Future.successful(Right(StrideRequest(request, clientIdOpt)))
      }
      .recover {
        case _: InsufficientEnrolments =>
          val msg = "Failed Stride Auth - InsufficientEnrolments"
          logger.info(msg)
          Left(Unauthorized(msg))
        case _: NoActiveSession =>
          logger.info("Failed Stride Auth - NoActiveSession")
          Left(
            Redirect(
              strideLoginUrl,
              Map(
                "successURL" -> Seq(strideSuccessUrl),
                "origin"     -> Seq(appConfig.appName)
              )
            )
          )
      }
  }
}
