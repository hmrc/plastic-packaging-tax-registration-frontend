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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlMatching}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  PartnershipCreateJourneyRequest,
  PartnershipDetails
}

import java.util.UUID

class PartnershipConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  val connector: PartnershipConnector = app.injector.instanceOf[PartnershipConnector]
  val testJourneyStartUrl             = "/identify-your-partnership/uuid-id/sa-utr"

  "create journey" should {
    expectPartnershipIdentificationServiceToSuccessfullyCreateNewJourney()
    val createJourneyRequest = aCreateJourneyRequest()

    "obtain a journey id from the partnership identification service" when {
      "journey creation successful" in {
        val result = await(connector.createJourney(createJourneyRequest))

        result mustBe testJourneyStartUrl
      }
    }

    "be timed" in {
      val timerCount = getTimer(PartnershipConnector.CreateJourneyTimer).getCount

      await(connector.createJourney(createJourneyRequest))

      getTimer(PartnershipConnector.CreateJourneyTimer).getCount mustBe (timerCount + 1)
    }

    "throw exception" when {
      "journey creation fails" in {
        expectPartnershipIdentificationServiceToFailToCreateNewJourney(Status.INTERNAL_SERVER_ERROR)

        intercept[Exception] {
          await(connector.createJourney(createJourneyRequest))
        }
      }
    }
  }

  "get details" should {
    val journeyId                  = UUID.randomUUID().toString
    val expectedPartnershipDetails = PartnershipDetails(sautr = "1234567890", postcode = "AA1 1AA")

    "obtain partnership details from the partnership identification service" in {
      expectPartnershipIdentificationServiceToReturnPartnershipDetails(journeyId,
                                                                       expectedPartnershipDetails
      )

      val actualPartnershipDetails = await(connector.getDetails(journeyId))

      actualPartnershipDetails mustBe expectedPartnershipDetails
    }

    "throw exception" when {
      "request fails" in {
        expectPartnershipIdentificationServiceToFailToReturnPartnershipDetails()

        intercept[Exception] {
          await(connector.getDetails(journeyId))
        }
      }
    }
  }

  private def expectPartnershipIdentificationServiceToSuccessfullyCreateNewJourney() =
    stubFor(
      post("/partnership-identification/api/general-partnership/journey")
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
            .withBody(Json.obj("journeyStartUrl" -> testJourneyStartUrl).toString())
        )
    )

  private def expectPartnershipIdentificationServiceToFailToCreateNewJourney(statusCode: Int) =
    stubFor(
      post("/partnership-identification/api/general-partnership/journey")
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )

  private def expectPartnershipIdentificationServiceToReturnPartnershipDetails(
    journeyId: String,
    details: PartnershipDetails
  ) =
    stubFor(
      WireMock.get(s"/partnership-identification/api/journey/$journeyId")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(Json.toJsObject(details).toString())
        )
    )

  private def expectPartnershipIdentificationServiceToFailToReturnPartnershipDetails() =
    stubFor(
      WireMock.get(urlMatching("/partnership-identification/api/journey/.*"))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

  private def aCreateJourneyRequest() =
    PartnershipCreateJourneyRequest(continueUrl = "callbackUrl",
                                    optServiceName = Some("PPT"),
                                    deskProServiceId = "xxx",
                                    signOutUrl = "signoutUrl",
                                    enableSautrCheck = true
    )

}
