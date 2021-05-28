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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlMatching}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus

class SubscriptionsConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  lazy val connector: SubscriptionsConnector = app.injector.instanceOf[SubscriptionsConnector]
  val incorpId                               = "uuid-id"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  "SubscriptionConnector" when {
    "calling 'getSubscriptionStatus'" should {
      val safeNumber = "123456789"

      "handle a 200 response" in {
        val validResponse = subscriptionStatus
        stubFor(
          get(urlMatching(s"/subscriptions/status/$safeNumber"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(validResponse)(SubscriptionStatus.format).toString)
            )
        )

        val res = await(connector.getSubscriptionStatus(safeNumber))

        res mustBe validResponse
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle an invalid payload" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          get(urlMatching(s"/subscriptions/status/$safeNumber"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(invalidResponse).toString)
            )
        )

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
      }

      "handle when an exception is thrown" in {
        stubFor(
          get(urlMatching(s"/subscriptions/status/$safeNumber"))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
      }
    }
  }
}
