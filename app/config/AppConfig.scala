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

package config

import play.api.Configuration
import uk.gov.hmrc.auth.core.Enrolment

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val runModeConfiguration: Configuration) {
  private def loadConfig(key: String) = runModeConfiguration.get[String](key)

  lazy val appName: String = loadConfig("appName")

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")

  lazy val strideEnrolments: Set[Enrolment] =
    runModeConfiguration
      .get[Seq[String]]("authentication.stride.enrolments")
      .map(Enrolment.apply)
      .toSet
  lazy val strideLoginBaseUrl: String = loadConfig("authentication.stride.loginBaseUrl")
  lazy val strideSuccessUrl: String = loadConfig("authentication.stride.successReturnUrl")
}
