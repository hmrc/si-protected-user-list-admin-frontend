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

package actions

import config.AppConfig
import models.StrideRequest
import play.api.Logging
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentials, name}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StrideAction @Inject() (
  val authConnector: AuthConnector,
  appConfig: AppConfig
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedFunctions
    with Logging {
  private lazy val strideLoginUrl: String = s"${appConfig.strideLoginBaseUrl}/stride/sign-in"
  private lazy val strideSuccessUrl: String = appConfig.strideSuccessUrl

  def publicRefine[A](request: Request[A]): Future[Either[Result, StrideRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val enrolment = appConfig.strideEnrolment

    authorised(Enrolment(enrolment))
      .retrieve(credentials and name and allEnrolments)
      .apply {
        case Some(credentials) ~ Some(name) ~ enrolments =>
          val id = credentials.providerId
          val roles: Set[String] = enrolments.enrolments
            .map(_.key)
            .intersect(Set(enrolment))
          val fullName = Some(Seq(name.name, name.lastName).flatten mkString " ") filter (_.nonEmpty)
          val operator = StrideRequest.Operator(id, fullName, roles)
          logger.debug("User Authenticated with Stride auth")
          Future.successful(Right(StrideRequest(request, credentials, operator)))
        case _ =>
          val msg = "Failed Stride Auth - Missing Data"
          logger.info(msg)
          Future.successful(Left(Unauthorized(msg)))
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
