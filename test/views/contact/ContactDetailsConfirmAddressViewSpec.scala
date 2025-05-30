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

package views.contact

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.contact.ConfirmAddress
import models.genericregistration.IncorporationAddressDetails
import views.html.contact.confirm_address

class ContactDetailsConfirmAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[confirm_address]

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

  private def createView(form: Form[ConfirmAddress] = ConfirmAddress.form(), isGroup: Boolean = false): Document =
    page(form, addressConversionUtils.toPptAddress(incorporationAddressDetails), isGroup)(
      registrationJourneyRequest,
      messages
    )

  "Confirm Address View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("primaryContactDetails.confirmAddress.title"))
    }

    "display section header" when {
      "Single organisation" in {
        view.getElementsByClass("govuk-caption-l").text() must include(messages("primaryContactDetails.sectionHeader"))
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view.getElementsByClass("govuk-caption-l").text() must include("Representative member details")
        view.getElementsByClass("govuk-caption-l").text() must include(
          messages("primaryContactDetails.group.sectionHeader")
        )

      }
    }

    "display radio inputs" in {

      view must containElementWithID("useRegisteredAddress")
      view must containElementWithID("useRegisteredAddress-2")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Confirm address view when filled" should {
    "display data" in {

      val confirmAddress =
        ConfirmAddress(Some(true))

      val form = ConfirmAddress
        .form()
        .fill(confirmAddress)
      val view = createView(form)

      view.getElementById("useRegisteredAddress") must haveAttribute("checked")
      view.getElementById("useRegisteredAddress-2") must not(haveAttribute("checked"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(ConfirmAddress.form(), addressConversionUtils.toPptAddress(incorporationAddressDetails), false)(
      registrationJourneyRequest,
      messages
    )

    page.render(
      ConfirmAddress.form(),
      addressConversionUtils.toPptAddress(incorporationAddressDetails),
      false,
      registrationJourneyRequest,
      messages
    )
  }

}
