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

import controllers.base.StrideAction
import models.InputForms
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import services.SiProtectedUserListService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.gg.test.UnitSpec
import util.Generators
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaCheckDrivenPropertyChecks with Generators {

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val stubStrideActions: Gen[StrideAction] = for {
    appName      <- nonEmptyStringGen
    strideConfig <- authStrideEnrolmentsConfigGen
  } yield new StrideAction(appName, strideConfig, mockAuthConnector)

  protected val mockBackendService: SiProtectedUserListService = mock[SiProtectedUserListService]
  protected val injectViews: Views = app.injector.instanceOf[Views]
  protected val inputForms: InputForms = app.injector.instanceOf[InputForms]

  protected def expectStrideAuthenticated(assertionFrom: String => Unit): Unit = {
    val stridePid = nonEmptyStringGen.sample.get

    when(mockAuthConnector.authorise[Option[String]](any, any)(any, any)).thenReturn(Future.successful(Some(stridePid)))

    assertionFrom(stridePid)
  }
  protected def expectStrideAuthenticated(assertion: => Assertion): Unit = expectStrideAuthenticated(_ => assertion)
}
