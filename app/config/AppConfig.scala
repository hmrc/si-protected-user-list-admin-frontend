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
  private def getString(key: String) = configuration.get[String](key)
  private def getBoolean(key: String) = configuration.get[Boolean](key)
  private def getInt(key: String) = configuration.get[Int](key)

  lazy val appName: String = getString("appName")

  lazy val analyticsToken: String = getString(s"google-analytics.token")
  lazy val analyticsHost: String = getString(s"google-analytics.host")
  lazy val analyticsConfig: AnalyticsConfig = AnalyticsConfig(analyticsToken = analyticsToken, analyticsHost = analyticsHost)

  lazy val strideEnrolments: Set[Enrolment] =
    configuration
      .get[Seq[String]]("authentication.stride.enrolments")
      .map(Enrolment.apply)
      .toSet
  lazy val strideLoginBaseUrl: String = getString("authentication.stride.loginBaseUrl")
  lazy val strideSuccessUrl: String = getString("authentication.stride.successReturnUrl")
  lazy val authStrideEnrolments: AuthStrideEnrolmentsConfig =
    AuthStrideEnrolmentsConfig(strideLoginBaseUrl = strideLoginBaseUrl, strideSuccessUrl = strideSuccessUrl, strideEnrolments = strideEnrolments)

  lazy val siProtectedUserConfig: SiProtectedUserConfig = SiProtectedUserConfig(
    bulkUploadScreenEnabled = getBoolean("siprotecteduser.allowlist.bulkupload.screen.enabled"),
    bulkUploadRowLimit = getInt("siprotecteduser.allowlist.bulkupload.file.row.limit"),
    bulkUploadBatchSize = getInt("siprotecteduser.allowlist.bulkupload.insert.batch.size"),
    bulkUploadBatchDelaySecs = getInt("siprotecteduser.allowlist.bulkupload.insert.batch.delay.secs"),
    showAllEnabled = getBoolean("siprotecteduser.allowlist.show.all.enabled"),
    shutterService = getBoolean("siprotecteduser.allowlist.shutter.service"),
    listScreenRowLimit = getInt("siprotecteduser.allowlist.listscreen.rowlimit"),
    addEntryActions = configuration.get[Seq[String]]("siprotecteduser.addEntry.actions")
  )

  lazy val siProtectedUserBackendEndpoint = servicesConfig.baseUrl("si-protected-user-list-admin")

  lazy val sessionCacheConfig = SessionCacheConfig(
    baseUri = servicesConfig.baseUrl("cacheable.session-cache"),
    domain = servicesConfig.getConfString("cacheable.session-cache.domain",
                                          throw new RuntimeException("missing required config cacheable.session-cache.domain")
                                         )
  )
}

case class AnalyticsConfig(analyticsToken: String, analyticsHost: String)
case class AuthStrideEnrolmentsConfig(strideLoginBaseUrl: String, strideSuccessUrl: String, strideEnrolments: Set[Enrolment])
case class SiProtectedUserConfig(
  bulkUploadScreenEnabled: Boolean,
  bulkUploadRowLimit: Int,
  bulkUploadBatchSize: Int,
  bulkUploadBatchDelaySecs: Int,
  showAllEnabled: Boolean,
  shutterService: Boolean,
  listScreenRowLimit: Int,
  addEntryActions: Seq[String]
)
case class SessionCacheConfig(baseUri: String, domain: String)
