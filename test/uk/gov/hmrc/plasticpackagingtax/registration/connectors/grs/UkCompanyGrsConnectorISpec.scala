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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs

import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, urlMatching}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  GrsIncorporationDetails,
  IncorporationDetails,
  UkCompanyGrsCreateRequest
}

class UkCompanyGrsConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  lazy val connector: UkCompanyGrsConnector = app.injector.instanceOf[UkCompanyGrsConnector]
  val incorpId                              = "uuid-id"

  "createJourney" should {
    "call the test only route to stub the journey" in {
      val testJourneyConfig =
        UkCompanyGrsCreateRequest(continueUrl = "/plastic-packaging-tax/registration",
                                  deskProServiceId = "plastic-packaging-tax",
                                  signOutUrl = "/feedback/plastic-packaging-tax"
        )
      val testJourneyStartUrl  = "/identify-your-incorporated-business/uuid-id/company-number"
      val testDeskProServiceId = "plastic-packaging-tax"

      stubFor(
        post("/incorporated-entity-identification/api/journey")
          .willReturn(
            aResponse()
              .withStatus(Status.CREATED)
              .withBody(
                Json.obj("journeyStartUrl"  -> testJourneyStartUrl,
                         "deskProServiceId" -> testDeskProServiceId
                ).toString
              )
          )
      )

      val res = await(connector.createJourney(testJourneyConfig))

      res mustBe testJourneyStartUrl
      getTimer("ppt.incorpId.create.journey.timer").getCount mustBe 1
    }

    "throw exception if http status is not 'CREATED'" in {
      val testJourneyConfig =
        UkCompanyGrsCreateRequest(continueUrl = "/plastic-packaging-tax/registration",
                                  deskProServiceId = "plastic-packaging-tax",
                                  signOutUrl = "/feedback/plastic-packaging-tax"
        )

      stubFor(
        post("/incorporated-entity-identification/api/journey")
          .willReturn(
            aResponse()
              .withStatus(Status.INTERNAL_SERVER_ERROR)
          )
      )

      intercept[Exception] {
        await(connector.createJourney(testJourneyConfig))
      }
    }
  }

  "getDetails" when {
    val testJourneyId = "testJourneyId"

    "incorp ID returns valid incorporation details" in {
      val validResponse = grsIncorporationDetails
      stubFor(
        get(urlMatching(s"/incorporated-entity-identification/api/journey/$testJourneyId"))
          .willReturn(
            aResponse().withStatus(OK)
              withBody (toJson(validResponse)(GrsIncorporationDetails.format).toString)
          )
      )

      val res = await(connector.getDetails(testJourneyId))

      res mustBe IncorporationDetails(validResponse)
      getTimer("ppt.incorpId.get.details.timer").getCount mustBe 1
    }

    "incorp ID returns invalid incorporation details" in {
      val invalidResponse = Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
      stubFor(
        get(urlMatching(s"/incorporated-entity-identification/api/journey/$testJourneyId"))
          .willReturn(
            aResponse().withStatus(OK)
              withBody (toJson(invalidResponse).toString)
          )
      )

      intercept[Exception] {
        await(connector.getDetails(testJourneyId))
      }
    }

    "incorp ID cannot found incorporation details" in {
      stubFor(
        get(urlMatching(s"/incorporated-entity-identification/api/journey/$testJourneyId"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      intercept[Exception] {
        await(connector.getDetails(testJourneyId))
      }
    }

    "throws an exception" in {
      stubFor(
        get(urlMatching(s"/incorporated-entity-identification/api/journey/$testJourneyId"))
          .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
      )

      intercept[Exception] {
        await(connector.getDetails(testJourneyId))
      }
    }
  }
}
