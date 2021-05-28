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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, put}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

class RegistrationConnectorSpec
    extends ConnectorISpec with Injector with ScalaFutures with EitherValues {

  lazy val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireHost, wirePort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  "create registration" should {

    "return success response" when {

      "valid request send" in {

        givenPostToRegistrationReturns(Status.CREATED,
                                       Json.toJsObject(Registration("123")).toString
        )

        val res = await(connector.create(Registration("123")))

        res.value.id mustBe "123"
        getTimer("ppt.registration.create.timer").getCount mustBe 1
      }

      "registration already exists" in {

        givenPostToRegistrationReturns(Status.OK, Json.toJsObject(Registration("123")).toString)

        val res = await(connector.create(Registration("123")))

        res.value.id mustBe "123"

      }
    }

    "return error" when {

      "service returns non success status code" in {

        givenPostToRegistrationReturns(Status.BAD_REQUEST)

        val res = await(connector.create(Registration("123")))

        res.left.value.getMessage must include("Failed to create registration")
      }

      "service returns invalid response" in {

        givenPostToRegistrationReturns(Status.CREATED, "someRubbish")

        val res = await(connector.create(Registration("123")))

        res.left.value.getMessage must include("Failed to create registration")
      }
    }
  }

  private def givenPostToRegistrationReturns(status: Int, body: String = "") =
    stubFor(
      post("/registrations")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  "find registration" should {

    "return registration" when {

      "exists" in {

        givenGetRegistrationReturns(
          Status.OK,
          Json.toJsObject(Registration("123", Some("incorpJourneyId"))).toString()
        )

        val res = await(connector.find("123"))

        res.value.get.id mustBe "123"
        res.value.get.incorpJourneyId mustBe Some("incorpJourneyId")
        getTimer("ppt.registration.find.timer").getCount mustBe 1

      }
    }

    "return empty result" when {

      "not exists" in {

        givenGetRegistrationReturns(Status.NOT_FOUND)

        val res = await(connector.find("123"))

        res.isRight mustBe true
        res.value.isEmpty mustBe true
      }
    }

    "return error" when {

      "downstream returns error" in {

        givenGetRegistrationReturns(Status.BAD_REQUEST)

        val res = await(connector.find("123"))

        res.left.value.getMessage must include("Failed to retrieve registration")
      }
    }
  }

  private def givenGetRegistrationReturns(status: Int, body: String = "") =
    stubFor(
      get("/registrations/123")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

  "update registration" should {

    "return success response" when {

      "valid request send" in {

        givenPutToRegistrationReturns(
          Status.CREATED,
          "123",
          Json.toJsObject(Registration("123", Some("incorpId"))).toString
        )

        val res = await(connector.update(Registration("123", Some("incorpId"))))

        res.value.id mustBe "123"
        res.value.incorpJourneyId mustBe Some("incorpId")
        getTimer("ppt.registration.update.timer").getCount mustBe 1

      }

      "registration already exists" in {

        givenPutToRegistrationReturns(Status.OK,
                                      "123",
                                      Json.toJsObject(Registration("123")).toString
        )

        val res = await(connector.update(Registration("123")))

        res.value.id mustBe "123"

      }
    }

    "return error" when {

      "service returns non success status code" in {

        givenPutToRegistrationReturns(Status.BAD_REQUEST, "123")

        val res = await(connector.update(Registration("123")))

        res.left.value.getMessage must include("Failed to update registration")
      }

      "service returns invalid response" in {

        givenPutToRegistrationReturns(Status.CREATED, "123", "someRubbish")

        val res = await(connector.update(Registration("123")))

        res.left.value.getMessage must include("Failed to update registration")
      }
    }
  }

  private def givenPutToRegistrationReturns(status: Int, id: String, body: String = "") =
    stubFor(
      put(s"/registrations/$id")
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )

}
