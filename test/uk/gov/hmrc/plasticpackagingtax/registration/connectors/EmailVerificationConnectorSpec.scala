/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors

import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlMatching}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  CreateEmailVerificationRequest,
  Email,
  VerificationStatus
}

class EmailVerificationConnectorSpec
    extends ConnectorISpec with Injector with ScalaFutures with EitherValues {

  lazy val connector: EmailVerificationConnector =
    app.injector.instanceOf[EmailVerificationConnector]

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  val testJourneyStartUrl  = "/verify-email/uuid-id"
  val testDeskProServiceId = "plastic-packaging-tax"

  "create email verification" should {

    "return success response" when {

      "valid request send" in {

        givenPostToEmailVerificationReturns(Status.CREATED,
                                            Json.obj("redirectUri" -> testJourneyStartUrl).toString
        )

        val res = await(
          connector.create(
            CreateEmailVerificationRequest(credId = "credId",
                                           continueUrl = "http://continue",
                                           origin = "origin",
                                           accessibilityStatementUrl = "http://accessibility",
                                           email = Email(address = "test@hmrc.com",
                                                         enterUrl = "hhtp://enterUrl"
                                           ),
                                           backUrl = "http://back",
                                           pageTitle = "PPT Title",
                                           deskproServiceName = "ppt"
            )
          )
        )

        res.right.get mustBe testJourneyStartUrl
        getTimer("ppt.email.verification.create.timer").getCount mustBe 1
      }
    }

    "return error" when {

      "service returns non success status code" in {

        givenPostToEmailVerificationReturns(Status.BAD_REQUEST,
                                            Json.obj("redirectUri" -> testJourneyStartUrl).toString
        )

        val res = await(
          connector.create(
            CreateEmailVerificationRequest(credId = "credId",
                                           continueUrl = "http://continue",
                                           origin = "origin",
                                           accessibilityStatementUrl = "http://accessibility",
                                           email = Email(address = "test@hmrc.com",
                                                         enterUrl = "hhtp://enterUrl"
                                           ),
                                           backUrl = "http://back",
                                           pageTitle = "PPT Title",
                                           deskproServiceName = "ppt"
            )
          )
        )

        res.left.value.getMessage must include("Failed to create email verification")
      }

      "service returns invalid response" in {

        givenPostToEmailVerificationReturns(Status.CREATED, "someRubbish")

        val res = await(
          connector.create(
            CreateEmailVerificationRequest(credId = "credId",
                                           continueUrl = "http://continue",
                                           origin = "origin",
                                           accessibilityStatementUrl = "http://accessibility",
                                           email = Email(address = "test@hmrc.com",
                                                         enterUrl = "hhtp://enterUrl"
                                           ),
                                           backUrl = "http://back",
                                           pageTitle = "PPT Title",
                                           deskproServiceName = "ppt"
            )
          )
        )

        res.left.value.getMessage must include("Error while verifying email")
      }
    }
  }

  "getStatus" when {
    val credId = "credId"

    "returns valid email details" in {
      val validResponse = emailVerification
      giveGetEmailVerificationDetailsReturns(
        OK,
        credId,
        toJson(validResponse)(VerificationStatus.format).toString
      )

      val res = await(connector.getStatus(credId))

      res.value.get mustBe validResponse
      getTimer("ppt.email.verification.getStatus.timer").getCount mustBe 1
    }

    "service returns non success status code" in {
      val validResponse = emailVerification
      giveGetEmailVerificationDetailsReturns(
        Status.BAD_REQUEST,
        credId,
        toJson(validResponse)(VerificationStatus.format).toString
      )

      val res = await(connector.getStatus(credId))

      res.left.value.getMessage must include("Failed to retrieve email verification status")
    }

    "service returns not found status code" in {
      giveGetEmailVerificationDetailsReturns(Status.NOT_FOUND, credId)

      val res = await(connector.getStatus(credId))

      res.value mustBe None
    }

    "service returns invalid response" in {
      giveGetEmailVerificationDetailsReturns(Status.OK, credId, "someRubbish")

      val res = await(connector.getStatus(credId))

      res.left.value.getMessage must include("Failed to retrieve email verification status")
    }
  }

  private def giveGetEmailVerificationDetailsReturns(
    status: Int,
    credId: String,
    body: String = ""
  ) =
    stubFor(
      get(urlMatching(s"/verification-status/$credId"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenPostToEmailVerificationReturns(status: Int, body: String = "") =
    stubFor(
      post("/verify-email")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
