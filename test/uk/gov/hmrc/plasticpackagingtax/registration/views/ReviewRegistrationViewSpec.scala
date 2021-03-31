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
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[review_registration_page]

  private val registration = aRegistration()

  private val incorporationDetails =
    IncorporationDetails("123456789", "Example Limited", "0123456789")

  private val organisationSection    = 0
  private val organisationNameKey    = 0
  private val organisationAddressKey = 1
  private val organisationTypeKey    = 2
  private val organisationCnrKey     = 3
  private val organisationUtrKey     = 4

  private val contactDetailsSection        = 1
  private val contactDetailsFullNameKey    = 0
  private val contactDetailsJobTitleKey    = 1
  private val contactDetailsAddressKey     = 2
  private val contactDetailsPhoneNumberKey = 3
  private val contactDetailsEmailKey       = 4

  private val liabilitySection   = 2
  private val liabilityDateKey   = 0
  private val liabilityWeightKey = 1

  private def createView(reg: Registration = registration): Document =
    page(reg, incorporationDetails)(request, messages)

  "Review registration View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("reviewRegistration.organisationDetails.title")

      messages must haveTranslationFor("reviewRegistration.organisationDetails.check.label")
      messages must haveTranslationFor("reviewRegistration.organisationDetails.businessName")
      messages must haveTranslationFor(
        "reviewRegistration.organisationDetails.registeredBusinessAddress"
      )
      messages must haveTranslationFor("reviewRegistration.organisationDetails.organisationType")
      messages must haveTranslationFor(
        "reviewRegistration.organisationDetails.businessRegistrationNumber"
      )
      messages must haveTranslationFor(
        "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
      )

      messages must haveTranslationFor("primaryContactDetails.check.title")
      messages must haveTranslationFor("primaryContactDetails.check.label")
      messages must haveTranslationFor("primaryContactDetails.check.jobTitle")
      messages must haveTranslationFor("primaryContactDetails.check.phoneNumber")
      messages must haveTranslationFor("primaryContactDetails.check.address")
      messages must haveTranslationFor("primaryContactDetails.check.email")

      messages must haveTranslationFor("checkLiabilityDetailsAnswers.check.label")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.date")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.weight")

      messages must haveTranslationFor("reviewRegistration.sendYourApplication.title")
      messages must haveTranslationFor("reviewRegistration.sendYourApplication.body")

      messages must haveTranslationFor("site.button.acceptAndSend")
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

      view.getElementById("back-link") must haveHref(routes.RegistrationController.displayPage())
    }

    "display meta title" in {

      view.select("title").text() must include(
        messages("reviewRegistration.organisationDetails.title")
      )
    }

    "display title" in {

      view.getElementsByClass("govuk-label--l").first() must containMessage(
        "reviewRegistration.organisationDetails.title"
      )
    }

    "display labels, values and change links" when {

      def getKeyFor(section: Int, index: Int) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-summary-list__key"
        ).get(index)
      def getValueFor(section: Int, index: Int) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-summary-list__value"
        ).get(index).text()
      def getChangeLinkFor(section: Int, index: Int) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-link"
        ).get(index)

      "displaying organisation details section" in {

        getKeyFor(organisationSection, organisationNameKey) must containMessage(
          "reviewRegistration.organisationDetails.businessName"
        )
        getKeyFor(organisationSection, organisationAddressKey) must containMessage(
          "reviewRegistration.organisationDetails.registeredBusinessAddress"
        )
        getKeyFor(organisationSection, organisationTypeKey) must containMessage(
          "reviewRegistration.organisationDetails.organisationType"
        )
        getKeyFor(organisationSection, organisationCnrKey) must containMessage(
          "reviewRegistration.organisationDetails.businessRegistrationNumber"
        )
        getKeyFor(organisationSection, organisationUtrKey) must containMessage(
          "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
        )

        getValueFor(organisationSection,
                    organisationNameKey
        ) mustBe incorporationDetails.companyName
        getValueFor(organisationSection,
                    organisationAddressKey
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(organisationSection, organisationTypeKey) mustBe "UK Company"
        getValueFor(organisationSection,
                    organisationCnrKey
        ) mustBe incorporationDetails.companyNumber
        getValueFor(organisationSection, organisationUtrKey) mustBe incorporationDetails.ctutr

      }

      "displaying primary contact details section" in {

        getKeyFor(contactDetailsSection, contactDetailsFullNameKey) must containMessage(
          "primaryContactDetails.check.fullName"
        )
        getKeyFor(contactDetailsSection, contactDetailsJobTitleKey) must containMessage(
          "primaryContactDetails.check.jobTitle"
        )
        getKeyFor(contactDetailsSection, contactDetailsAddressKey) must containMessage(
          "primaryContactDetails.check.address"
        )
        getKeyFor(contactDetailsSection, contactDetailsPhoneNumberKey) must containMessage(
          "primaryContactDetails.check.phoneNumber"
        )
        getKeyFor(contactDetailsSection, contactDetailsEmailKey) must containMessage(
          "primaryContactDetails.check.email"
        )

        getValueFor(contactDetailsSection, contactDetailsFullNameKey) mustBe "Jack Gatsby"
        getValueFor(contactDetailsSection,
                    contactDetailsJobTitleKey
        ) mustBe registration.primaryContactDetails.jobTitle.get
        getValueFor(contactDetailsSection,
                    contactDetailsAddressKey
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(contactDetailsSection,
                    contactDetailsPhoneNumberKey
        ) mustBe registration.primaryContactDetails.phoneNumber.get
        getValueFor(contactDetailsSection,
                    contactDetailsEmailKey
        ) mustBe registration.primaryContactDetails.email.get

        getChangeLinkFor(contactDetailsSection, contactDetailsFullNameKey) must haveHref(
          routes.ContactDetailsFullNameController.displayPage()
        )
        getChangeLinkFor(contactDetailsSection, contactDetailsJobTitleKey) must haveHref(
          routes.ContactDetailsJobTitleController.displayPage()
        )
        getChangeLinkFor(contactDetailsSection, contactDetailsAddressKey) must haveHref(
          routes.ContactDetailsConfirmAddressController.displayPage()
        )
        getChangeLinkFor(contactDetailsSection, contactDetailsPhoneNumberKey) must haveHref(
          routes.ContactDetailsTelephoneNumberController.displayPage()
        )
        getChangeLinkFor(contactDetailsSection, contactDetailsEmailKey) must haveHref(
          routes.ContactDetailsEmailAddressController.displayPage()
        )
      }

      "displaying liability details section" in {

        getKeyFor(liabilitySection, liabilityDateKey) must containMessage(
          "checkLiabilityDetailsAnswers.date"
        )
        getKeyFor(liabilitySection, liabilityWeightKey) must containMessage(
          "checkLiabilityDetailsAnswers.weight"
        )

        getValueFor(liabilitySection, liabilityDateKey) mustBe "01 Apr 2022"
        getValueFor(liabilitySection, liabilityWeightKey) mustBe "1000 kg"

        getChangeLinkFor(liabilitySection, liabilityDateKey) must haveHref(
          routes.LiabilityStartDateController.displayPage()
        )
        getChangeLinkFor(liabilitySection, liabilityWeightKey) must haveHref(
          routes.LiabilityWeightController.displayPage()
        )
      }
    }

    "display 'Accept and send' button" in {

      view.getElementsByClass("govuk-button").text() must include(
        messages("site.button.acceptAndSend")
      )
    }
  }
}
