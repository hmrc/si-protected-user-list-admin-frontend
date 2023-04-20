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

package connectors

import models.User
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SiProtectedUserListAdminConnectorSpec extends UnitSpec {

  trait Setup {
    val aServiceUrl = "service-url"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockHttpClient = mock[HttpClient]

    val adminConnector: SiProtectedUserListAdminConnector = new SiProtectedUserListAdminConnector(aServiceUrl, mockHttpClient)

    def responseGood(): HttpResponse = HttpResponse(
      201,
      ""
    )

    def responseBadRequest(errorMessage: String) = HttpResponse(
      400,
      errorMessage
    )

    def responseConflict(errorMessage: JsValue) = HttpResponse(
      409,
      errorMessage.toString()
    )

    val conflictJson: JsObject = Json.obj("error" -> "conflict!")

    val missingOrgError: String = "missing organisationName"
    val missingUsernameError: String = "missing username"
    val orgNameOver300Chars: String = "organisationName length exceeds 300 characters"

    // Todo remove in next UI iteration
    def generateSilentLoginPostCall(response: HttpResponse): OngoingStubbing[Future[HttpResponse]] =
      when(mockHttpClient.POST[JsObject, HttpResponse](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(response))
  }

  "findEntry" should {
    "returns existing user in entry database" in new Setup {
      val expectedLoginUser = User("user name", "company name", "some@email.com")
      when(mockHttpClient.GET[Either[UpstreamErrorResponse, User]](any, any, any)(any, any, any))
        .thenReturn(Future.successful(Right(expectedLoginUser)))

      val actualLoginUser = await(adminConnector.findEntry("user name"))
      actualLoginUser shouldBe expectedLoginUser
    }

    "returns 404 not found if user doesn't exist in entry list database" in new Setup {
      when(mockHttpClient.GET[String](any, any, any)(any, any, any)).thenReturn(Future.failed(UpstreamErrorResponse("", 404)))

      val failedLookup = adminConnector.findEntry("user name")
      val ex = await(failedLookup.failed)
      ex shouldBe a[UpstreamErrorResponse]
    }
  }

  "deleteUserEntry" should {
    "delete existing user in entry list database" in new Setup {
      when(mockHttpClient.DELETE[Either[UpstreamErrorResponse, Unit]](any, any)(any, any, any))
        .thenReturn(Future.successful(Right(HttpResponse(204, ""))))

      await(adminConnector.deleteUserEntry("user name"))
      verify(mockHttpClient).DELETE[HttpResponse](endsWith("user name"), any)(any, any, any)
    }

    "throw a UpstreamErrorResponse if user doesn't exist in entry listed database" in new Setup {
      when(mockHttpClient.DELETE[Either[UpstreamErrorResponse, Unit]](any, any)(any, any, any))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", 404)))

      intercept[UpstreamErrorResponse] {
        await(adminConnector.deleteUserEntry("user name"))
      }
    }
  }

  "addEntry" should {
    "return conflict and when user is already in the entry list" in new Setup {
      when(mockHttpClient.POST[JsObject, Either[UpstreamErrorResponse, Unit]](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("conflict", 409))))

      intercept[ConflictException] {
        await(adminConnector.addEntry(User("DuplicateUser", "DuplicateOrg", "some@email.com")))
      }
    }

    "return success when details are successfully inserted" in new Setup {
      when(mockHttpClient.POST[JsObject, Either[UpstreamErrorResponse, Unit]](any, any, any)(any, any, any, any))
        .thenReturn(Future.successful(Right(HttpResponse(201, ""))))

      private val user = User("SomeUserName", "OrgName", "some@email.com")
      val payload = Json.obj(
        "username"         -> user.username,
        "organisationName" -> user.organisationName,
        "requesterEmail"   -> user.requesterEmail
      )
      await(adminConnector.addEntry(user))

      verify(mockHttpClient)
        .POST[JsObject, Either[UpstreamErrorResponse, Unit]](endsWith("/si-protected-user-list-admin/add"), eqTo(payload), any)(any, any, any, any)
    }
  }

  "getAllEntries" should {
    "return a list of all users" in new Setup {
      val expectedUsers = List(
        User("user1", "org", "mail"),
        User("user2", "org", "mail")
      )
      when(mockHttpClient.GET[Either[UpstreamErrorResponse, List[User]]](any, any, any)(any, any, any))
        .thenReturn(Future.successful(Right(expectedUsers)))

      val actualUsers: List[User] = await(adminConnector.getAllEntries())
      actualUsers shouldBe expectedUsers
    }
  }
}
