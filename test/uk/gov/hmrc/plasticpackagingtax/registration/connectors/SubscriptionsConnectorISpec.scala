/*
 * Copyright 2022 HM Revenue & Customs
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

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, put, urlMatching}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, OK}
import play.api.test.Injecting
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionCreateOrUpdateResponseSuccess,
  SubscriptionStatusResponse
}

class SubscriptionsConnectorISpec extends ConnectorISpec with Injecting with ScalaFutures {

  lazy val connector: SubscriptionsConnector = inject[SubscriptionsConnector]
  val incorpId                               = "uuid-id"

  "SubscriptionConnector" when {
    "calling 'getSubscriptionStatus'" should {
      val safeNumber = "123456789"

      "handle a 200 response" in {
        val validResponse = subscriptionStatus
        stubFor(
          get(urlMatching(s"/subscriptions/status/$safeNumber"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(validResponse)(SubscriptionStatusResponse.format).toString)
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

    "calling 'getSubscription'" should {
      val pptReference = "XMPPT0000000123"

      "return a registration when call to backend is successful" in {
        val activeRegistration = Registration(id = "123")
        stubFor(
          get(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(activeRegistration)(Registration.format).toString)
            )
        )

        val res = await(connector.getSubscription(pptReference))

        res mustBe activeRegistration
        getTimer("ppt.subscription.get.timer").getCount mustBe 1
      }

      "throw an exeption when call to backend fails" in {
        stubFor(
          get(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.getSubscription(pptReference))
        }
      }
    }

    "calling 'submitSubscription'" should {
      val safeNumber = "123456789"

      "handle a 200 response" in {
        val validResponse = subscriptionCreateOrUpdate
        stubFor(
          post(urlMatching(s"/subscriptions/$safeNumber"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody toJson(validResponse)(
                  SubscriptionCreateOrUpdateResponseSuccess.format
                ).toString
            )
        )

        val res = await(connector.submitSubscription(safeNumber, aRegistration()))

        res mustBe validResponse
        getTimer("ppt.subscription.submit.timer").getCount mustBe 1
      }

      "handle an unexpected success response" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          post(urlMatching(s"/subscriptions/$safeNumber"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(invalidResponse).toString)
            )
        )

        intercept[IllegalStateException] {
          await(connector.submitSubscription(safeNumber, aRegistration()))
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
          await(connector.submitSubscription(safeNumber, aRegistration()))
        }
      }

      "handle when an exception is thrown" in {
        stubFor(
          post(urlMatching(s"/subscriptions/$safeNumber"))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.submitSubscription(safeNumber, aRegistration()))
        }
      }
    }

    "calling 'updateSubscription'" should {
      val pptReference = "XMPPT0000000123"

      "handle a 200 response" in {
        val validResponse = subscriptionCreateOrUpdate
        stubFor(
          put(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody toJson(validResponse)(
                  SubscriptionCreateOrUpdateResponseSuccess.format
                ).toString
            )
        )

        val res = await(connector.updateSubscription(pptReference, aRegistration()))

        res mustBe validResponse
        getTimer("ppt.subscription.update.timer").getCount mustBe 1
      }

      "handle an unexpected success response" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          put(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(
              aResponse().withStatus(OK)
                withBody (toJson(invalidResponse).toString)
            )
        )

        intercept[IllegalStateException] {
          await(connector.updateSubscription(pptReference, aRegistration()))
        }
      }

      "handle an unexpected failed response" in {
        val invalidResponse =
          Map("invalidKey1" -> "invalidValue1", "invalidKey2" -> "invalidValue2")
        stubFor(
          put(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(
              aResponse().withStatus(BAD_REQUEST)
                withBody (toJson(invalidResponse).toString)
            )
        )

        intercept[IllegalStateException] {
          await(connector.updateSubscription(pptReference, aRegistration()))
        }
      }

      "handle when an exception is thrown" in {
        stubFor(
          put(urlMatching(s"/subscriptions/$pptReference"))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.updateSubscription(pptReference, aRegistration()))
        }
      }
    }
  }
}
