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

import config.AppConfig
import controllers.base.StrideAction
import models.InputForms
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import support.UnitSpec
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.Generators
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseControllerSpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with ScalaCheckDrivenPropertyChecks
    with Generators
    with BeforeAndAfterEach {

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val stubStrideActions: Gen[StrideAction] = for {
    appName      <- nonEmptyStringGen
    strideConfig <- authStrideEnrolmentsConfigGen
  } yield new StrideAction(appName, strideConfig, mockAuthConnector)

  protected val mockAuditConnector: AuditConnector = mock[AuditConnector]
  protected val mockBackendService: SiProtectedUserListService = mock[SiProtectedUserListService]
  protected val injectViews: Views = app.injector.instanceOf[Views]
  protected val inputForms: InputForms = app.injector.instanceOf[InputForms]
  protected val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  protected def expectStrideAuthenticated(forceStridePid: Boolean = true)(assertionFrom: (Option[String], Option[Name]) => Unit): Unit = {
    val retrievalResult = retrievalResultGen(forceStridePid).sample.get

    when(mockAuthConnector.authorise[Option[String] ~ Option[Name]](any, any)(any, any)).thenReturn(Future.successful(retrievalResult))

    assertionFrom(retrievalResult.a, retrievalResult.b)
  }
  protected def expectStrideAuthenticated(assertion: => Assertion): Unit = expectStrideAuthenticated()((_, _) => assertion)

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuditConnector)
  }
}
