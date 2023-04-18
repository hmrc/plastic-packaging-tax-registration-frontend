/*
 * Copyright 2023 HM Revenue & Customs
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

package views.address

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.contact.Address
import forms.contact.Address.UKAddress
import services.CountryService
import views.html.address.address_page

class AddressViewSpec extends UnitViewSpec with Matchers {

  private val page           = inject[address_page]
  private val countryService = inject[CountryService]
  private val updateLink  = Call("PUT", "/update")
  private val headingKey  = "addressLookup.partner.lookup.heading"
  private val contactName = Some("the contact")

  private def createView(form: Form[Address] = Address.form()): Document =
    page(form, countryService.getAll(), updateLink, headingKey, contactName)(registrationJourneyRequest, messages)

  "Address View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("addressLookup.partner.lookup.heading", contactName.get))
    }

    "display header" in {
      view.getElementsByClass("govuk-caption-l").text() must include(messages("primaryContactDetails.sectionHeader"))
    }

    "display hint" in {
      view.getElementsByClass("govuk-body").get(0).text() must include(messages("primaryContactDetails.address.hint"))
    }

    "display input boxes" in {
      view must containElementWithID("addressLine1")
      view must containElementWithID("addressLine2")
      view must containElementWithID("addressLine3")
      view must containElementWithID("townOrCity")
      view must containElementWithID("postCode")
      view must containElementWithID("countryCode")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Email address view when filled" should {
    "display data" in {
      val anAddress =
        Address(
          addressLine1 = "Address Line 1",
          addressLine2 = Some("Address Line 2"),
          addressLine3 = Some("Address Line 3"),
          townOrCity = "townOrCity",
          maybePostcode = Some("LS3 3UJ"),
          countryCode = "GB"
        )

      val form = Address
        .form()
        .fill(anAddress)
      val view = createView(form)

      view.getElementById("addressLine1").attr("value") mustBe "Address Line 1"
      view.getElementById("addressLine2").attr("value") mustBe "Address Line 2"
      view.getElementById("addressLine3").attr("value") mustBe "Address Line 3"
      view.getElementById("townOrCity").attr("value") mustBe "townOrCity"
      view.getElementById("postCode").attr("value") mustBe "LS3 3UJ"
      view.select("select#countryCode option[selected]").text() mustBe "United Kingdom"
    }
  }

  "display error" when {

    "mandatory address fields have not been submitted" in {
      val anInvalidAddress =
        Address(addressLine1 = "", addressLine2 = None, addressLine3 = None, townOrCity = "", maybePostcode = Some(""), countryCode = "")

      val form = Address
        .form()
        .fillAndValidate(anInvalidAddress)
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("addressLine1", "Enter address line 1")
      view must haveGovukFieldError("townOrCity", "Enter a town or city")
      view must haveGovukFieldError("postCode", "Enter a postcode")
      view must haveGovukFieldError("countryCode", "Select a country")
    }

    "address fields are not valid" in {
      val anInvalidAddress =
        UKAddress(
          addressLine1 = "*&%^",
          addressLine2 = Some("Address Line 2*&%^"),
          addressLine3 = Some("Address Line 3*&%^"),
          townOrCity = "*&%^",
          postCode = "*&%^"
        )

      val form = Address
        .form()
        .fillAndValidate(anInvalidAddress)
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("addressLine1", "Enter an address in the correct format")
      view must haveGovukFieldError("addressLine2", "Enter an address in the correct format")
      view must haveGovukFieldError("addressLine3", "Enter an address in the correct format")
      view must haveGovukFieldError("townOrCity", "Enter a town or city in the correct format")
      view must haveGovukFieldError("postCode", "Enter a postcode in the correct format")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(Address.form(), countryService.getAll(), updateLink, headingKey, contactName, None)(registrationJourneyRequest, messages)
    page.render(Address.form(), countryService.getAll(), updateLink, headingKey, contactName, None, registrationJourneyRequest, messages)
  }

}
