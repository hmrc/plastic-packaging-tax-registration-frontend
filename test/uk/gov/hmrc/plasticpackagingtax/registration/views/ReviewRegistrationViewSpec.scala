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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  PartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[review_registration_page]

  private val registration = aRegistration()

  private val organisationSection    = 0
  private val organisationAddressKey = 0
  private val organisationTypeKey    = 1
  private val organisationCnrKey     = 2
  private val organisationUtrKey     = 3

  private val contactDetailsSection        = 1
  private val contactDetailsFullNameKey    = 0
  private val contactDetailsJobTitleKey    = 1
  private val contactDetailsAddressKey     = 2
  private val contactDetailsPhoneNumberKey = 3
  private val contactDetailsEmailKey       = 4

  private val liabilitySection   = 2
  private val liabilityDateKey   = 0
  private val liabilityWeightKey = 1

  private def createView(
    reg: Registration = registration,
    ukCompanyDetails: Option[IncorporationDetails] = None,
    soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
    partnershipDetails: Option[PartnershipDetails] = None
  ): Document =
    page(reg, ukCompanyDetails, soleTraderDetails)(request, messages)

  "Review registration View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("reviewRegistration.organisationDetails.title")

      messages must haveTranslationFor("reviewRegistration.organisationDetails.check.label")
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

      messages must haveTranslationFor("reviewRegistration.organisationDetails.partnership.name")
    }

    val view: Document = createView(ukCompanyDetails = Some(incorporationDetails))

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

      def getKeyFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-summary-list__key"
        ).get(index)
      def getValueFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-summary-list__value"
        ).get(index).text()
      def getChangeLinkFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass(
          "govuk-link"
        ).get(index)

      "displaying organisation details section for uk company" in {

        val ukCompanyView: Document = createView(ukCompanyDetails = Some(incorporationDetails))

        getKeyFor(organisationSection, organisationAddressKey, ukCompanyView) must containMessage(
          "reviewRegistration.organisationDetails.registeredBusinessAddress"
        )
        getKeyFor(organisationSection, organisationTypeKey, ukCompanyView) must containMessage(
          "reviewRegistration.organisationDetails.organisationType"
        )
        getKeyFor(organisationSection, organisationCnrKey, ukCompanyView) must containMessage(
          "reviewRegistration.organisationDetails.businessRegistrationNumber"
        )
        getKeyFor(organisationSection, organisationUtrKey, ukCompanyView) must containMessage(
          "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
        )

        getValueFor(organisationSection,
                    organisationAddressKey,
                    ukCompanyView
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(organisationSection, organisationTypeKey) mustBe UK_COMPANY.toString
        getValueFor(organisationSection,
                    organisationCnrKey,
                    ukCompanyView
        ) mustBe incorporationDetails.companyNumber
        getValueFor(organisationSection,
                    organisationUtrKey,
                    ukCompanyView
        ) mustBe incorporationDetails.ctutr

      }

      "displaying organisation details section for sole trader" in {

        val soleTraderView = createView(
          reg = aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                  businessRegisteredAddress = Some(testBusinessAddress)
              )
            )
          ),
          soleTraderDetails = Some(soleTraderIncorporationDetails)
        )

        getKeyFor(organisationSection, 0, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.soleTrader.firstName"
        )

        getKeyFor(organisationSection, 1, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.soleTrader.lastName"
        )

        getKeyFor(organisationSection, 2, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.registeredBusinessAddress"
        )
        getKeyFor(organisationSection, 3, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.organisationType"
        )
        getKeyFor(organisationSection, 4, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.soleTrader.dob"
        )
        getKeyFor(organisationSection, 5, soleTraderView) must containMessage(
          "reviewRegistration.organisationDetails.soleTrader.nino"
        )

        getValueFor(organisationSection,
                    0,
                    soleTraderView
        ) mustBe soleTraderIncorporationDetails.firstName
        getValueFor(organisationSection,
                    1,
                    soleTraderView
        ) mustBe soleTraderIncorporationDetails.lastName
        getValueFor(organisationSection,
                    2,
                    soleTraderView
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(organisationSection, 3, soleTraderView) mustBe SOLE_TRADER.toString
        getValueFor(organisationSection,
                    4,
                    soleTraderView
        ) mustBe soleTraderIncorporationDetails.dateOfBirth
        getValueFor(organisationSection,
                    5,
                    soleTraderView
        ) mustBe soleTraderIncorporationDetails.nino

      }

      "displaying organisation details section for partnership" in {

        val partnershipView = createView(
          reg = aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                  businessRegisteredAddress = Some(testBusinessAddress)
              )
            )
          ),
          partnershipDetails = Some(partnershipDetails)
        )

        getKeyFor(organisationSection, 0, partnershipView) must containMessage(
          "reviewRegistration.organisationDetails.partnership.name"
        )
        getKeyFor(organisationSection, 1, partnershipView) must containMessage(
          "reviewRegistration.organisationDetails.registeredBusinessAddress"
        )
        getKeyFor(organisationSection, 2, partnershipView) must containMessage(
          "reviewRegistration.organisationDetails.organisationType"
        )

        getValueFor(organisationSection, 0, partnershipView) mustBe "TODO"
        getValueFor(organisationSection,
                    1,
                    partnershipView
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(organisationSection, 2, partnershipView) mustBe PARTNERSHIP.toString
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
