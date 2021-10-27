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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment

import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlMatching}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment.RegistrationConnector.UserEnrolmentTimer
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment._
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.{
  EnrolmentFailureCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails

class RegistrationConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with EitherValues {

  lazy val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  private val enrolmentUrl       = "/enrolment"
  private val pptReferenceNumber = "XPPT000123456789"
  private val successfulResponse = UserEnrolmentSuccessResponse(pptReferenceNumber)

  private val verificationFailedResponse =
    UserEnrolmentFailedResponse(pptReferenceNumber, EnrolmentFailureCode.VerificationFailed)

  "Registration Connector " when {
    "enrol " should {

      "handle a 200 response" in {
        val validResponse = successfulResponse
        stubFor(
          post(urlMatching(enrolmentUrl))
            .willReturn(
              aResponse().withStatus(OK)
                withBody toJson(validResponse)(UserEnrolmentSuccessResponse.format).toString
            )
        )

        val res = await(
          connector.enrol(
            UserEnrolmentDetails(pptReference = Some(PptReference(pptReferenceNumber)),
                                 isUkAddress = Some(IsUkAddress(Some(true))),
                                 postcode = Some(Postcode("AB1 2CD")),
                                 registrationDate =
                                   Some(RegistrationDate(DateData("01", "10", "2021")))
            )
          )
        )

        res mustBe validResponse
        getTimer(UserEnrolmentTimer).getCount mustBe 1
      }

//      "handle verification failed response" in {
//
//        val invalidResponse = verificationFailedResponse
//
//        stubFor(
//          post(urlMatching(enrolmentUrl))
//            .willReturn(
//              aResponse().withStatus(BAD_REQUEST)
//                withBody toJson(invalidResponse).toString
//            )
//        )
//
//        val res = await(connector.enrol(userEnrolmentDetails))
//
//        res mustBe invalidResponse
//      }

      "handle an unexpected success response" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          post(urlMatching(enrolmentUrl))
            .willReturn(
              aResponse().withStatus(OK)
                withBody toJson(invalidResponse).toString
            )
        )

        intercept[IllegalStateException] {
          await(connector.enrol(userEnrolmentDetails))
        }
      }

      "handle an unexpected failed response" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          post(urlMatching(s"/subscriptions/$safeNumber"))
            .willReturn(
              aResponse().withStatus(BAD_REQUEST)
                withBody (toJson(invalidResponse).toString)
            )
        )

        intercept[IllegalStateException] {
          await(connector.enrol(userEnrolmentDetails))
        }
      }

      "handle when an exception is thrown" in {
        stubFor(
          post(urlMatching(enrolmentUrl))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.enrol(userEnrolmentDetails))
        }
      }
    }
  }
}
