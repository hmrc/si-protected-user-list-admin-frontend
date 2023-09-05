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
  import configuration.underlying._

  lazy val appName: String = getString("appName")

  lazy val analyticsConfig: AnalyticsConfig = AnalyticsConfig(analyticsToken = getString(s"google-analytics.token"), analyticsHost = getString(s"google-analytics.host"))

  lazy val authStrideEnrolments: StrideConfig = StrideConfig(
    strideLoginBaseUrl = getString("authentication.stride.loginBaseUrl"),
    strideSuccessUrl = getString("authentication.stride.successReturnUrl"),
    strideEnrolments = configuration
      .get[Seq[String]]("authentication.stride.enrolments")
      .map(Enrolment.apply)
      .toSet
  )

  lazy val siProtectedUserConfig: SiProtectedUserConfig = SiProtectedUserConfig(
    dashboardUrl = configuration.get[String]("account-protection-tools-dashboard-linkUrl"),
    identityProviders = configuration.get[Seq[String]]("si-protected-user.identity-providers"),
    addedByTeams = configuration.get[Seq[String]]("si-protected-user.added-by-teams")
  )

  lazy val backendConfig: BackendConfig = BackendConfig(
    endpoint = servicesConfig.baseUrl("si-protected-user-list-admin"),
    contextRoot = servicesConfig.getConfString(
      s"si-protected-user-list-admin.context-root",
      throw new RuntimeException(s"Could not find config key 'si-protected-user-list-admin.context-root'")
    )
  )

  lazy val sessionCacheConfig: SessionCacheConfig = SessionCacheConfig(
    baseUri = servicesConfig.baseUrl("cacheable.session-cache"),
    domain = servicesConfig.getConfString("cacheable.session-cache.domain", throw new RuntimeException("missing required config cacheable.session-cache.domain"))
  )
}

case class AnalyticsConfig(analyticsToken: String, analyticsHost: String)
case class StrideConfig(strideLoginBaseUrl: String, strideSuccessUrl: String, strideEnrolments: Set[Enrolment])
case class SiProtectedUserConfig(
  dashboardUrl: String,
  identityProviders: Seq[String],
  addedByTeams: Seq[String]
)
case class SessionCacheConfig(baseUri: String, domain: String)
case class BackendConfig(endpoint: String, contextRoot: String)
