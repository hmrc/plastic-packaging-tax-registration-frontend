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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Address, FullName}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  PrimaryContactDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.check_primary_contact_details_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckContactDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[check_primary_contact_details_page]

  private val registration = aRegistration(
    withPrimaryContactDetails(
      PrimaryContactDetails(fullName = Some(FullName("Jack", "Gatsby")),
                            jobTitle = Some("Developer"),
                            phoneNumber = Some("0203 4567 890"),
                            email = Some("test@test.com"),
                            address = Some(
                              Address(addressLine1 = "2 Scala Street",
                                      addressLine2 = Some("Soho"),
                                      townOrCity = "London",
                                      postCode = "W1T 2HN"
                              )
                            )
      )
    )
  )

  private val fullNameKey    = 0
  private val jobTitleKey    = 1
  private val addressKey     = 2
  private val phoneNumberKey = 3
  private val emailKey       = 4

  private def createView(reg: Registration = registration): Document = page(reg)(request, messages)

  "Check primary contact details answers View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("primaryContactDetails.check.title")
      messages must haveTranslationFor("primaryContactDetails.check.label")
      messages must haveTranslationFor("primaryContactDetails.check.jobTitle")
      messages must haveTranslationFor("primaryContactDetails.check.phoneNumber")
      messages must haveTranslationFor("primaryContactDetails.check.address")
      messages must haveTranslationFor("primaryContactDetails.check.email")
      messages must haveTranslationFor("site.link.change")
    }

    val view: Document = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.ContactDetailsConfirmAddressController.displayPage()
      )
    }

    "display meta title" in {

      view.select("title").text() must include(messages("primaryContactDetails.check.meta.title"))
    }

    "display title" in {

      view.getElementsByClass("govuk-label--l").first() must containMessage(
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
      getValueFor(addressKey) mustBe "2 Scala Street Soho London W1T 2HN"
      getValueFor(phoneNumberKey) mustBe registration.primaryContactDetails.phoneNumber.get
      getValueFor(emailKey) mustBe registration.primaryContactDetails.email.get

      getChangeLinkFor(fullNameKey) must haveHref(
        routes.ContactDetailsFullNameController.displayPage()
      )
      getChangeLinkFor(jobTitleKey) must haveHref(
        routes.ContactDetailsJobTitleController.displayPage()
      )
      getChangeLinkFor(addressKey) must haveHref(
        routes.ContactDetailsConfirmAddressController.displayPage()
      )
      getChangeLinkFor(phoneNumberKey) must haveHref(
        routes.ContactDetailsTelephoneNumberController.displayPage()
      )
      getChangeLinkFor(emailKey) must haveHref(
        routes.ContactDetailsEmailAddressController.displayPage()
      )
    }

    "display 'Continue' button" in {

      view.getElementsByClass("govuk-button").text() must include("Save and Continue")
    }
  }
}
