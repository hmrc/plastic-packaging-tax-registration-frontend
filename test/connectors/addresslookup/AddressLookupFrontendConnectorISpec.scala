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

package connectors.addresslookup

import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status
import play.api.test.Helpers.await
import play.api.test.Injecting
import models.addresslookup._

class AddressLookupFrontendConnectorISpec extends ConnectorISpec with Injecting with ScalaFutures {

  lazy val connector: AddressLookupFrontendConnector =
    inject[AddressLookupFrontendConnector]

  val initialiseRequest = AddressLookupConfigV2(
    options = JourneyOptions(continueUrl = "/continue", signOutHref = "/signout", serviceHref = "/service"),
    labels = JourneyLabels(en =
      LanguageLabels(
        appLevelLabels = AppLevelLabels(navTitle = "Title"),
        selectPageLabels = SelectPageLabels("Select address", "Select address"),
        lookupPageLabels = LookupPageLabels("Lookup address", "Lookup address"),
        confirmPageLabels = ConfirmPageLabels("Confirm address", "Confirm address"),
        editPageLabels = EditPageLabels("Edit address", "Edit address")
      )
    )
  )

  "address lookup" should {

    "call initialise" in {

      givenInitialiseSuccess("/on-ramp")

      val res = await(connector.initialiseJourney(initialiseRequest))

      res.redirectUrl mustBe "/on-ramp"
      getTimer("ppt.addresslookup.initialise.timer").getCount mustBe 1
    }

    "get address" in {

      givenGetAddressSuccess("12345")

      val res = await(connector.getAddress("12345"))

      res.address.postcode mustBe Some("ZZ1 1ZZ")
      getTimer("ppt.addresslookup.getaddress.timer").getCount mustBe 1
    }

  }

  private def givenInitialiseSuccess(onRamp: String) =
    stubFor(
      post("/api/init")
        .willReturn(
          aResponse()
            .withStatus(Status.ACCEPTED)
            .withHeader(LOCATION, onRamp)
        )
    )

  private def givenGetAddressSuccess(id: String) =
    stubFor(
      get(s"/api/confirmed?id=$id")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody("""
                |{
                |    "auditRef" : "bed4bd24-72da-42a7-9338-f43431b7ed72",
                |    "id" : "GB990091234524",
                |    "address" : {
                |        "lines" : [ "10 Other Place", "Some District", "Anytown" ],
                |        "postcode" : "ZZ1 1ZZ",
                |        "country" : {
                |            "code" : "GB",
                |            "name" : "United Kingdom"
                |        }
                |    }
                |}
                |""".stripMargin)
        )
    )

}
