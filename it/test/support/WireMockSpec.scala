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

package support

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

trait WireMockSpec
  extends AnyWordSpec
    with Matchers
    with WiremockSupport
    with OptionValues
    with HeaderNames
    with Status
    with DefaultAwaitTimeout
    with GuiceOneServerPerSuite
    with FutureAwaits {

  protected def extraConfig: Map[String, Any] = Map.empty

  override def fakeApplication(): Application = GuiceApplicationBuilder().configure(
    replaceExternalDependenciesWithMockServers
      ++ csrfIgnoreFlags
      ++ extraConfig
  ).build()

  protected def resource(resource: String): String = s"http://localhost:$port$resource"

  protected def resourceRequest(url: String): WSRequest = wsClient.url(resource(url)).withHttpHeaders("Csrf-Token" -> "nocheck")

  protected def wsClient: WSClient = app.injector.instanceOf[WSClient]

  private val csrfIgnoreFlags = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck"
  )
}
