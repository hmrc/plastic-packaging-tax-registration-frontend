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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  CreateEmailVerificationRequest,
  Email,
  EmailStatus,
  VerificationStatus,
  VerifyPasscodeRequest
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
      val validResponse = VerificationStatus(Seq(EmailStatus("mail@mail.com", true, false)))
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
      val validResponse = Seq(EmailStatus("mail@mail.com", true, false))
      giveGetEmailVerificationDetailsReturns(Status.BAD_REQUEST,
                                             credId,
                                             toJson(validResponse).toString
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

  "submitPasscode" when {
    val journeyId             = "credId"
    val verifyPasscodeRequest = VerifyPasscodeRequest("passcode", "email")

    "returns complete" in {
      val validResponse = "{\"status\":\"complete\"}"
      givenPostToSubmitPascode(OK, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe "complete"
      getTimer("ppt.email.verification.verify.passcode.timer").getCount mustBe 1
    }

    "returns bad request" in {
      val validResponse = "{\"status\":\"incorrectPasscode\"}"
      givenPostToSubmitPascode(BAD_REQUEST, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe "incorrectPasscode"
    }

    "returns forbidden" in {
      val validResponse = "{\"status\":\"tooManyAttempts\"}"
      givenPostToSubmitPascode(FORBIDDEN, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe "tooManyAttempts"
    }

    "returns not found" in {
      val validResponse = "{\"status\":\"passcodeNotFound\"}"
      givenPostToSubmitPascode(FORBIDDEN, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe "passcodeNotFound"
    }

    "returns internal server error" in {
      givenPostToSubmitPascode(INTERNAL_SERVER_ERROR, journeyId)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.left.value.getMessage must include("Failed to verify passcode, status: 500, error: ")
    }
  }

  private def giveGetEmailVerificationDetailsReturns(
    status: Int,
    credId: String,
    body: String = ""
  ) =
    stubFor(
      get(urlMatching(s"/email-verification/verification-status/$credId"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenPostToEmailVerificationReturns(status: Int, body: String = "") =
    stubFor(
      post("/email-verification/verify-email")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenPostToSubmitPascode(status: Int, journeyId: String, body: String = "") =
    stubFor(
      post(s"/email-verification/journey/$journeyId/passcode")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
