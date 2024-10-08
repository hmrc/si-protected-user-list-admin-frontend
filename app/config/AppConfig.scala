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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val configuration: Configuration, servicesConfig: ServicesConfig) {
  import configuration.underlying.*

  lazy val appName: String = getString("appName")

  lazy val analyticsConfig: AnalyticsConfig =
    AnalyticsConfig(analyticsToken = getString(s"google-analytics.token"), analyticsHost = getString(s"google-analytics.host"))

  lazy val searchQueryMaxValue: String = getString("search.query.max.length")

  lazy val authStrideEnrolments: StrideConfig = {
    val service = "stride-auth-frontend"
    val configPath = s"microservice.services.$service."

    StrideConfig(
      strideLoginBaseUrl = servicesConfig.baseUrl(service) + servicesConfig.getString(configPath + "path"),
      strideEnrolments = configuration
        .get[Seq[String]](configPath + "enrolments")
        .map(Enrolment.apply)
        .toSet
    )
  }

  lazy val siProtectedUserConfig: SiProtectedUserConfig = SiProtectedUserConfig(
    dashboardUrl      = configuration.get[String]("account-protection-tools-dashboard-linkUrl"),
    identityProviders = configuration.get[Seq[String]]("si-protected-user.identity-providers"),
    addedByTeams      = configuration.get[Seq[String]]("si-protected-user.added-by-teams")
  )

  lazy val backendConfig: BackendConfig = BackendConfig(
    endpoint = servicesConfig.baseUrl("si-protected-user-list-admin"),
    contextRoot = servicesConfig.getConfString(
      s"si-protected-user-list-admin.context-root",
      throw new RuntimeException(s"Could not find config key 'si-protected-user-list-admin.context-root'")
    )
  )
}

case class AnalyticsConfig(analyticsToken: String, analyticsHost: String)
case class StrideConfig(strideLoginBaseUrl: String, strideEnrolments: Set[Enrolment])
case class SiProtectedUserConfig(
  dashboardUrl: String,
  identityProviders: Seq[String],
  addedByTeams: Seq[String]
)
case class BackendConfig(endpoint: String, contextRoot: String)
