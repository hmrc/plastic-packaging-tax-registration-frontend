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
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{
  Date,
  LiabilityExpectedWeight,
  LiabilityWeight,
  OrgType
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.gdsPageHeading
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[review_registration_page]

  private val registration = aRegistration()

  private val organisationSection    = 1
  private val organisationAddressKey = 0
  private val organisationTypeKey    = 1
  private val organisationCnrKey     = 2
  private val organisationUtrKey     = 3

  private val contactDetailsSection        = 2
  private val contactDetailsFullNameKey    = 0
  private val contactDetailsJobTitleKey    = 1
  private val contactDetailsAddressKey     = 2
  private val contactDetailsPhoneNumberKey = 3
  private val contactDetailsEmailKey       = 4

  private val liabilitySection = 0

  private val liabilityStartLink = Call("GET", "/liabilityStartLink")

  private def createView(reg: Registration = registration): Document =
    page(reg, liabilityStartLink)(journeyRequest, messages)

  "Review registration View" should {

    val view: Document = createView(aRegistration())

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

      view.getElementsByClass(gdsPageHeading).first() must containMessage(
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

        val ukCompanyRegistration   = aRegistration()
        val ukCompanyView: Document = createView(ukCompanyRegistration)

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
        getValueFor(organisationSection, organisationTypeKey) mustBe OrgType.displayName(UK_COMPANY)
        getValueFor(organisationSection,
                    organisationCnrKey,
                    ukCompanyView
        ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.companyNumber
        getValueFor(organisationSection,
                    organisationUtrKey,
                    ukCompanyView
        ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.ctutr

      }

      "displaying organisation details section for sole trader" in {

        val soleTraderRegistration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                businessRegisteredAddress = Some(testBusinessAddress),
                                soleTraderDetails = Some(soleTraderIncorporationDetails),
                                incorporationDetails = None
            )
          )
        )
        val soleTraderView = createView(soleTraderRegistration)

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
        ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.firstName
        getValueFor(organisationSection,
                    1,
                    soleTraderView
        ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.lastName
        getValueFor(organisationSection,
                    2,
                    soleTraderView
        ) mustBe "2 Scala Street Soho London W1T 2HN"
        getValueFor(organisationSection, 3, soleTraderView) mustBe SOLE_TRADER.toString
        getValueFor(organisationSection,
                    4,
                    soleTraderView
        ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.dateOfBirth
        getValueFor(organisationSection,
                    5,
                    soleTraderView
        ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.nino

      }

      "displaying organisation details section for partnership" in {

        val partnershipRegistration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                businessRegisteredAddress = Some(testBusinessAddress),
                                partnershipDetails = Some(partnershipDetails),
                                incorporationDetails = None
            )
          )
        )
        val partnershipView = createView(partnershipRegistration)

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

      "displaying liability details section during preLaunch" in {
        val liabilityView =
          page(
            registration =
              aRegistration(
                withLiabilityDetails(
                  LiabilityDetails(
                    expectedWeight =
                      Some(LiabilityExpectedWeight(Some(true), totalKg = Some(11000))),
                    isLiable = Some(true)
                  )
                )
              ),
            liabilityStartLink = liabilityStartLink
          )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> true)),
            messages = messages
          )

        getKeyFor(liabilitySection, 0, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.weight"
        )

        getValueFor(liabilitySection, 0, liabilityView) mustBe "11000 kg"
        getChangeLinkFor(liabilitySection, 0, liabilityView) must haveHref(liabilityStartLink)
      }

      "displaying liability details section during postLaunch when <10,000kg but expect to exceed" in {
        val liabilityView =
          page(
            registration =
              aRegistration(
                withLiabilityDetails(
                  LiabilityDetails(weight = Some(LiabilityWeight(totalKg = Some(1000))),
                                   startDate = Some(Date(Some(1), Some(11), Some(2022))),
                                   expectToExceedThresholdWeight = Some(true)
                  )
                )
              ),
            liabilityStartLink = liabilityStartLink
          )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> false)),
            messages = messages
          )

        getKeyFor(liabilitySection, 0, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.weight"
        )
        getKeyFor(liabilitySection, 1, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.future.exceed"
        )
        getKeyFor(liabilitySection, 2, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.date"
        )

        getValueFor(liabilitySection, 0, liabilityView) mustBe "1000 kg"
        getValueFor(liabilitySection, 1, liabilityView) mustBe "Yes"
        getValueFor(liabilitySection, 2, liabilityView) mustBe "01 Nov 2022"

        getChangeLinkFor(liabilitySection, 0, liabilityView) must haveHref(liabilityStartLink)
        getChangeLinkFor(liabilitySection, 1, liabilityView) must haveHref(
          routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
        )
        getChangeLinkFor(liabilitySection, 2, liabilityView) must haveHref(
          routes.LiabilityStartDateController.displayPage()
        )
      }

      "displaying liability details section during postLaunch when >10,000kg" in {
        val liabilityView =
          page(
            registration =
              aRegistration(
                withLiabilityDetails(
                  LiabilityDetails(weight = Some(LiabilityWeight(totalKg = Some(11000))),
                                   startDate = Some(Date(Some(1), Some(11), Some(2022))),
                                   expectToExceedThresholdWeight = None
                  )
                )
              ),
            liabilityStartLink = liabilityStartLink
          )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> false)),
            messages = messages
          )

        getKeyFor(liabilitySection, 0, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.weight"
        )
        getKeyFor(liabilitySection, 1, liabilityView) must containMessage(
          "checkLiabilityDetailsAnswers.date"
        )

        getValueFor(liabilitySection, 0, liabilityView) mustBe "11000 kg"
        getValueFor(liabilitySection, 1, liabilityView) mustBe "01 Nov 2022"

        getChangeLinkFor(liabilitySection, 0, liabilityView) must haveHref(liabilityStartLink)
        getChangeLinkFor(liabilitySection, 1, liabilityView) must haveHref(
          routes.LiabilityStartDateController.displayPage()
        )
      }
    }

    "display 'Accept and send' button" in {

      view.getElementsByClass("govuk-button").text() must include(
        messages("site.button.acceptAndSend")
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration, liabilityStartLink)(journeyRequest, messages)
    page.render(registration, liabilityStartLink, journeyRequest, messages)
  }

}
