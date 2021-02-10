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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorpIdCreateRequest

class IncorpIdConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  lazy val connector: IncorpIdConnector = app.injector.instanceOf[IncorpIdConnector]
  val incorpId                          = "uuid-id"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  "createJourney" should {
    "call the test only route to stub the journey" in {
      val testJourneyConfig =
        IncorpIdCreateRequest(continueUrl = "/plastic-packaging-tax/registration",
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
    }

    "throw exception if http status is not 'CREATED'" in {
      val testJourneyConfig =
        IncorpIdCreateRequest(continueUrl = "/plastic-packaging-tax/registration",
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

}
