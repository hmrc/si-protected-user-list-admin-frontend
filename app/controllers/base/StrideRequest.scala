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

package controllers.base

import play.api.mvc.{MessagesRequest, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Name

final case class StrideRequest[A](
  underlying: MessagesRequest[A],
  userPidOpt: Option[String],
  nameOpt: Option[Name]
) extends WrappedRequest[A](underlying) {
  def getUserPid: String = userPidOpt getOrElse "Unknown_User_Pid"
}
