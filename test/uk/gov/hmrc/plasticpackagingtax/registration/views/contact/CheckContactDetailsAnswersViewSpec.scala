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

package uk.gov.hmrc.plasticpackagingtax.registration.views.contact

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact.{routes => contactRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  PrimaryContactDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.gdsPageHeading
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.check_primary_contact_details_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckContactDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[check_primary_contact_details_page]

  private val registration = aRegistration(
    withPrimaryContactDetails(
      PrimaryContactDetails(name = Some("Jack Gatsby"),
                            jobTitle = Some("Developer"),
                            phoneNumber = Some("0203 4567 890"),
                            email = Some("test@test.com"),
                            address = Some(
                              Address(addressLine1 = "2 Scala Street",
                                      addressLine2 = Some("Soho"),
                                      addressLine3 = None,
                                      townOrCity = "London",
                                      maybePostcode = Some("W1T 2HN"),
                                     countryCode = "GB"
                              )
                            )
      )
    )
  )

  private val fullNameKey    = 0
  private val jobTitleKey    = 1
  private val emailKey       = 2
  private val phoneNumberKey = 3
  private val addressKey     = 4

  private def createView(reg: Registration = registration): Document =
    page(reg)(journeyRequest, messages)

  "Check primary contact details answers View" should {

    val view: Document = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display meta title" in {

      view.select("title").text() must include(messages("primaryContactDetails.check.title"))
    }

    "display title" in {

      view.getElementsByClass(gdsPageHeading).first() must containMessage(
        "primaryContactDetails.check.title"
      )
    }

    "display labels, values and change links" in {

      def getKeyFor(index: Int) = view.getElementsByClass("govuk-summary-list__key").get(index)
      def getValueFor(index: Int) =
        view.getElementsByClass("govuk-summary-list__value").get(index).text()
      def getChangeLinkFor(index: Int) =
        view.getElementsByClass("govuk-summary-list").first.getElementsByClass("govuk-link").get(
          index
        )

      getKeyFor(fullNameKey) must containMessage("primaryContactDetails.check.fullName")
      getKeyFor(jobTitleKey) must containMessage("primaryContactDetails.check.jobTitle")
      getKeyFor(addressKey) must containMessage("primaryContactDetails.check.address")
      getKeyFor(phoneNumberKey) must containMessage("primaryContactDetails.check.phoneNumber")
      getKeyFor(emailKey) must containMessage("primaryContactDetails.check.email")

      getValueFor(fullNameKey) mustBe "Jack Gatsby"
      getValueFor(jobTitleKey) mustBe registration.primaryContactDetails.jobTitle.get
      getValueFor(addressKey) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
      getValueFor(phoneNumberKey) mustBe registration.primaryContactDetails.phoneNumber.get
      getValueFor(emailKey) mustBe registration.primaryContactDetails.email.get

      getChangeLinkFor(fullNameKey) must haveHref(
        contactRoutes.ContactDetailsFullNameController.displayPage()
      )
      getChangeLinkFor(jobTitleKey) must haveHref(
        contactRoutes.ContactDetailsJobTitleController.displayPage()
      )
      getChangeLinkFor(addressKey) must haveHref(
        contactRoutes.ContactDetailsConfirmAddressController.displayPage()
      )
      getChangeLinkFor(phoneNumberKey) must haveHref(
        contactRoutes.ContactDetailsTelephoneNumberController.displayPage()
      )
      getChangeLinkFor(emailKey) must haveHref(
        contactRoutes.ContactDetailsEmailAddressController.displayPage()
      )
    }

    "display 'Continue' button" in {

      view.getElementsByClass("govuk-button").text() must include("Save and continue")
    }

    "display a caption" when {
      "is group organisation" in {
        val view = createView(aRegistration(withGroupDetail(Some(groupDetailsWithMembers))))

        view.getElementById("section-header").text() mustBe "Representative member details"
        view.getElementById("section-header").text() mustBe messages("primaryContactDetails.group.sectionHeader")
      }

      "is single organisation" in {
        view.getElementById("section-header").text() mustBe "Contact details"
        view.getElementById("section-header").text() mustBe messages("primaryContactDetails.sectionHeader")
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration)(request, messages)
    page.render(registration, request, messages)
  }

}
