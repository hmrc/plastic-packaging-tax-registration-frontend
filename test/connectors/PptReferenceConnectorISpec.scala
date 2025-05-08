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

package connectors

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.api.test.Injecting

class PptReferenceConnectorISpec extends ConnectorISpec with Injecting with ScalaFutures {

  lazy val connector: PptReferenceConnector = inject[PptReferenceConnector]
  val validInternalId                       = "Int-123"
  val validPPTReference                     = "123456"

  "PptReferenceConnector" when {
    "calling 'get'" should {

      "handle a 200 response" in {
        stubFor(
          get(urlMatching(s"/plastic-packaging-tax/ppt-reference"))
            .willReturn(
              aResponse().withStatus(Status.OK)
                withBody (Json.obj("clientPPT" -> validPPTReference).toString())
            )
        )

        val res = await(connector.get(hc))

        res mustBe validPPTReference
        getTimer(PPTReference.pptReferenceTimer).getCount mustBe 1
      }

      "handle a 404 response" in {
        stubFor(
          get(urlMatching(s"/plastic-packaging-tax/ppt-reference"))
            .willReturn(
              aResponse().withStatus(Status.NOT_FOUND)
            )
        )

        intercept[Exception] {
          await(connector.get(hc))
        }
      }

      "handle a 500 response" in {
        stubFor(
          get(urlMatching(s"/plastic-packaging-tax/ppt-reference"))
            .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
        )

        intercept[Exception] {
          await(connector.get(hc))
        }
      }
    }

  }
}
