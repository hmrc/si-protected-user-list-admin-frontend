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

package services

import javax.inject.{Inject, Named, Singleton}
import models.User
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePut, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AllowListSessionCache @Inject() (sc: ServicesConfig, @Named("appName") appName: String, httpClient: HttpClient)(implicit ec: ExecutionContext)
    extends SessionCache {
  override def baseUri: String = sc.baseUrl("cacheable.session-cache")

  override def domain: String =
    sc.getConfString("cacheable.session-cache.domain", throw new RuntimeException("missing required config cacheable.session-cache.domain"))

  override def defaultSource: String = appName

  override def http: CoreGet with CorePut with CoreDelete = httpClient

  def getAll()(implicit hc: HeaderCarrier): Future[List[User]] =
    fetchAndGetEntry[List[User]]("bulkAllowlist").map(_.getOrElse(Nil))

  def add(login: User)(implicit hc: HeaderCarrier): Future[List[User]] =
    getAll().flatMap { all =>
      val updated = login :: all
      cache("bulkAllowlist", updated).map(_ => updated)
    }

  def clear()(implicit hc: HeaderCarrier): Future[Unit] =
    remove().map(_ => ())
}
