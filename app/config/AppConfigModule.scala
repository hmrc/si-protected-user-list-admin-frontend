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

import com.google.inject.{AbstractModule, Provides, Singleton}
import config.AppConfig.{AnalyticsConfig, SessionCacheConfig, SiProtectedUserConfig, StrideConfig}
import controllers.base.StrideAction
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Named
import scala.jdk.CollectionConverters.ListHasAsScala

class AppConfigModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()

    val isShuttered = configuration.get[Boolean]("si-protected-user.shutter-service")
    if (isShuttered)
      bind(classOf[StrideAction]).to(classOf[StrideAction.Shuttered])
  }

  @Provides @Singleton
  def analyticsConfig(appConfig: AppConfig): AnalyticsConfig = appConfig.analyticsConfig

  @Provides @Singleton
  def getStrideConfig(servicesConfig: ServicesConfig): StrideConfig = {
    val service = "stride-auth-frontend"
    val config = configuration.underlying.getConfig(s"microservice.services.$service")

    StrideConfig(
      outboundURL = servicesConfig.baseUrl(service) + config.getString("path"),
      enrolments = config
        .getStringList("enrolments")
        .asScala
        .map(Enrolment.apply)
        .toSet
    )
  }

  @Provides @Named("backend_url")
  def getBackendURL(servicesConfig: ServicesConfig): String = {
    val service = "si-protected-user-list-admin"
    servicesConfig.baseUrl(service) + "/" + service
  }

  @Provides @Singleton
  def siProtectedUserConfig(appConfig: AppConfig): SiProtectedUserConfig = appConfig.siProtectedUserConfig

  @Provides @Singleton
  def sessionCacheConfig(appConfig: AppConfig): SessionCacheConfig = appConfig.sessionCacheConfig
}
