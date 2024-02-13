/*
 * Copyright 2024 HM Revenue & Customs
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

package views.organisation

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import models.genericregistration.IncorporationAddressDetails
import views.html.organisation.confirm_business_address

class ConfirmBusinessAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[confirm_business_address]

  private val incorporationAddressDetails = IncorporationAddressDetails(
    address_line_1 = Some("testLine1"),
    address_line_2 = Some("testLine2"),
    locality = Some("test town"),
    care_of = Some("test name"),
    po_box = Some("123"),
    postal_code = Some("AA11AA"),
    premises = Some("1"),
    country = Some("United Kingdom")
  )

  private def createView(): Document =
    page(addressConversionUtils.toPptAddress(incorporationAddressDetails), "company name", "url")(registrationJourneyRequest, messages)

  "Confirm Address View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("company.confirmAddress.title", "company name"))
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(addressConversionUtils.toPptAddress(incorporationAddressDetails), "company name", "url")(registrationJourneyRequest, messages)

    page.render(addressConversionUtils.toPptAddress(incorporationAddressDetails), "company name", "url", registrationJourneyRequest, messages)
  }

}
