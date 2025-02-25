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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{noContent, post, stubFor}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.jdk.CollectionConverters._
import scala.util.Try

trait WiremockSupport extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite =>

  val wiremockPort = 11111
  val wiremockHost = "localhost"

  val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  val wireMockServer = new WireMockServer(wmConfig)
  WireMock.configureFor(wiremockHost, wiremockPort)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()

    // make datastream stop complaining endlessly
    stubFor(post("/write/audit").willReturn(noContent))
    stubFor(post("/write/audit/merged").willReturn(noContent))
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  def replaceExternalDependenciesWithMockServers: Map[String, Any] = flatStructurePortMapping() ++ devStructurePortMapping()

  private def flatStructurePortMapping(): Map[String, Int] = {
    lazy val config: Config = ConfigFactory.load()

    val servicePorts = Try(config.getConfig("microservice.services"))
      .getOrElse(ConfigFactory.empty())
      .entrySet()
      .asScala
      .collect {
        case e if e.getKey.endsWith("port") =>
          "microservice.services." + e.getKey -> wiremockPort
      }.toMap

    val auditingPort = if (config.hasPath("auditing.consumer.baseUri.port")) {
      Map("auditing.consumer.baseUri.port" -> wiremockPort)
    } else {
      Map.empty
    }

    servicePorts ++ auditingPort
  }

  private def devStructurePortMapping(): Map[String, Any] = {
    lazy val config: Config = ConfigFactory.load()

    val servicePorts = Try(config.getConfig("Dev.microservice.services"))
      .getOrElse(ConfigFactory.empty())
      .entrySet()
      .asScala
      .map {
        case e if e.getKey.endsWith("port") =>
          "Test.microservice.services." + e.getKey -> wiremockPort
        case e =>
          "Test.microservice.services." + e.getKey -> e.getValue
      }
      .toMap

    val auditingPort = if (config.hasPath("Test.auditing.consumer.baseUri.port")) {
      Map("Test.auditing.consumer.baseUri.port" -> wiremockPort)
    } else {
      Map.empty
    }

    servicePorts ++ auditingPort
  }

}
