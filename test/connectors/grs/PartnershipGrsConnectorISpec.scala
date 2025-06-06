/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.grs

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlMatching}
import models.genericregistration._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.util.UUID

class PartnershipGrsConnectorISpec extends ConnectorISpec with Injecting with ScalaFutures {

  val connector: PartnershipGrsConnector =
    app.injector.instanceOf[PartnershipGrsConnector]

  val testJourneyStartUrl = "/identify-your-partnership/uuid-id/sa-utr"

  val partnershipBaseUrl           = "http://localhost:20001/partnership-identification/api"
  val generalPartnershipJourneyUrl = s"$partnershipBaseUrl/general-partnership-journey"

  "create journey" should {
    expectPartnershipIdentificationServiceToSuccessfullyCreateNewJourney()
    val createJourneyRequest = aCreateJourneyRequest()

    "obtain a journey id from the partnership identification service" when {
      "journey creation successful" in {
        val result =
          await(connector.createJourney(createJourneyRequest, generalPartnershipJourneyUrl))

        result mustBe testJourneyStartUrl
      }
    }

    "be timed" in {
      val timerCount = getTimer(GeneralPartnershipGrsConnector.CreateJourneyTimer).getCount

      await(connector.createJourney(createJourneyRequest, generalPartnershipJourneyUrl))

      getTimer(GeneralPartnershipGrsConnector.CreateJourneyTimer).getCount mustBe (timerCount + 1)
    }

    "throw exception" when {
      "journey creation fails" in {
        expectPartnershipIdentificationServiceToFailToCreateNewJourney(Status.INTERNAL_SERVER_ERROR)

        intercept[Exception] {
          await(connector.createJourney(createJourneyRequest, generalPartnershipJourneyUrl))
        }
      }
    }
  }

  "get details" should {
    val journeyId = UUID.randomUUID().toString
    val grsGeneralPartnershipDetails = GrsPartnershipBusinessDetails(
      sautr = "1234567890",
      postcode = "AA1 1AA",
      identifiersMatch = true,
      businessVerification = Some(GrsBusinessVerification("PASS")),
      registration = GrsRegistration(
        registrationStatus = "REGISTERED",
        registeredBusinessPartnerId = Some("123")
      ),
      companyProfile = None
    )

    "obtain partnership details from the partnership identification service" in {
      expectPartnershipIdentificationServiceToReturnPartnershipDetails(journeyId, grsGeneralPartnershipDetails)

      val actualPartnershipDetails = await(connector.getDetails(journeyId))

      actualPartnershipDetails mustBe PartnershipBusinessDetails(grsGeneralPartnershipDetails)
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
      post("/partnership-identification/api/general-partnership-journey")
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
            .withBody(Json.obj("journeyStartUrl" -> testJourneyStartUrl).toString())
        )
    )

  private def expectPartnershipIdentificationServiceToFailToCreateNewJourney(statusCode: Int) =
    stubFor(
      post("/partnership-identification/api/general-partnership-journey")
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )

  private def expectPartnershipIdentificationServiceToReturnPartnershipDetails(
    journeyId: String,
    grsPartnershipBusinessDetails: GrsPartnershipBusinessDetails
  ) =
    stubFor(
      WireMock.get(s"/partnership-identification/api/journey/$journeyId")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(Json.toJsObject(grsPartnershipBusinessDetails).toString())
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
    PartnershipGrsCreateRequest(
      continueUrl = "callbackUrl",
      optServiceName = Some("PPT"),
      deskProServiceId = "xxx",
      signOutUrl = "signoutUrl",
      accessibilityUrl = "accessibilityUrl",
      enableSautrCheck = true
    )

}
