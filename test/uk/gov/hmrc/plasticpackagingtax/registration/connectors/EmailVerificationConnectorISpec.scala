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

import akka.util.Helpers.Requiring
import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlMatching}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  JOURNEY_NOT_FOUND,
  TOO_MANY_ATTEMPTS
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification._

class EmailVerificationConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with EitherValues {

  lazy val connector: EmailVerificationConnector =
    app.injector.instanceOf[EmailVerificationConnector]

  val emailVerificationRequest = CreateEmailVerificationRequest(credId = "credId",
                                                                continueUrl = "http://continue",
                                                                origin = "origin",
                                                                accessibilityStatementUrl =
                                                                  "http://accessibility",
                                                                email =
                                                                  Email(address = "test@hmrc.com",
                                                                        enterUrl = "hhtp://enterUrl"
                                                                  ),
                                                                backUrl = "http://back",
                                                                pageTitle = "PPT Title",
                                                                deskproServiceName = "ppt"
  )

  val testJourneyStartUrl  = "/verify-email/uuid-id"
  val testDeskProServiceId = "plastic-packaging-tax"

  "create email verification" should {

    "return success response" when {

      "valid request send" in {

        givenPostToEmailVerificationReturns(Status.CREATED,
                                            Json.obj("redirectUri" -> testJourneyStartUrl).toString
        )

        val res = await(connector.create(emailVerificationRequest))

        res.right.get mustBe testJourneyStartUrl
        getTimer("ppt.email.verification.create.timer").getCount mustBe 1
      }
    }

    "return error" when {

      "service returns non success status code" in {

        givenPostToEmailVerificationReturns(Status.BAD_REQUEST,
                                            Json.obj("redirectUri" -> testJourneyStartUrl).toString
        )

        val res = await(connector.create(emailVerificationRequest))

        res.left.value.getMessage must include("Failed to create email verification")
      }

      "service returns invalid response" in {

        givenPostToEmailVerificationReturns(Status.CREATED, "someRubbish")

        val res = await(connector.create(emailVerificationRequest))

        res.left.value.isInstanceOf[DownstreamServiceError]
      }
    }
  }

  "getStatus" when {
    val credId = "credId"

    "returns valid email details" in {
      val validResponse = VerificationStatus(Seq(EmailStatus("mail@mail.com", true, false)))
      givenGetEmailVerificationDetailsReturns(
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
      givenGetEmailVerificationDetailsReturns(Status.BAD_REQUEST,
                                              credId,
                                              toJson(validResponse).toString
      )
      val res = await(connector.getStatus(credId))

      res.left.value.isInstanceOf[DownstreamServiceError]
    }

    "service returns not found status code" in {
      givenGetEmailVerificationDetailsReturns(Status.NOT_FOUND, credId, "")

      val res = await(connector.getStatus(credId))

      res.value mustBe None
    }

    "service returns invalid response" in {
      givenGetEmailVerificationDetailsReturns(Status.OK, credId, "someRubbish")

      val res = await(connector.getStatus(credId))

      res.left.value.isInstanceOf[DownstreamServiceError]
    }
  }

  "submitPasscode" when {
    val journeyId             = "credId"
    val verifyPasscodeRequest = VerifyPasscodeRequest("passcode", "email")

    "returns complete" in {
      val validResponse = "{\"status\":\"complete\"}"
      givenPostToSubmitPasscode(OK, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe COMPLETE
      getTimer("ppt.email.verification.verify.passcode.timer").getCount mustBe 1
    }

    "returns bad request" in {
      val validResponse = "{\"status\":\"incorrectPasscode\"}"
      givenPostToSubmitPasscode(BAD_REQUEST, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe INCORRECT_PASSCODE
    }

    "returns forbidden" in {
      val validResponse = "{\"status\":\"tooManyAttempts\"}"
      givenPostToSubmitPasscode(FORBIDDEN, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe TOO_MANY_ATTEMPTS
    }

    "returns not found" in {
      val validResponse = "{\"status\":\"passcodeNotFound\"}"
      givenPostToSubmitPasscode(NOT_FOUND, journeyId, validResponse)

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.value mustBe JOURNEY_NOT_FOUND
    }

    "returns internal server error" in {
      givenPostToSubmitPasscode(INTERNAL_SERVER_ERROR, journeyId, "")

      val res = await(connector.verifyPasscode(journeyId, verifyPasscodeRequest))

      res.left.value.isInstanceOf[DownstreamServiceError]
    }
  }

  "getTestOnlyPasscode" when {

    "returns passcode" in {
      val validResponse =
        "{\"passcodes\":\"[{\"email\" : \"ppt@mail.com\", \"passcode\" : \"HDDDYX\", }]\"}"
      givenGetTestOnlyPasscode(OK, validResponse)

      val res = await(connector.getTestOnlyPasscode())

      res.right.value mustBe validResponse
    }

    "throws exception for errors" in {
      val validResponse =
        "{\"code\": \"UNEXPECTED_ERROR\",\n    \"message\": \"Bearer token expired\"}"
      givenGetTestOnlyPasscode(INTERNAL_SERVER_ERROR, validResponse)

      val res: Either[ServiceError, String] = await(connector.getTestOnlyPasscode())

      res.left.value.isInstanceOf[DownstreamServiceError]
    }
  }

  private def givenGetEmailVerificationDetailsReturns(status: Int, credId: String, body: String) =
    stubFor(
      get(urlMatching(s"/email-verification/verification-status/$credId"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenPostToEmailVerificationReturns(status: Int, body: String) =
    stubFor(
      post("/email-verification/verify-email")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenPostToSubmitPasscode(status: Int, journeyId: String, body: String) =
    stubFor(
      post(s"/email-verification/journey/$journeyId/passcode")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  private def givenGetTestOnlyPasscode(status: Int, body: String) =
    stubFor(
      get(s"/test-only/passcodes")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
