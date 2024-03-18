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
import controllers.organisation.routes
import forms.contact.Address
import forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import forms.organisation.OrgType
import forms.organisation.OrgType.{PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import models.addresslookup.CountryCode.GB
import models.registration.group.{GroupMember, OrganisationDetails => GroupMemberOrganisationDetails}
import models.registration.{GroupDetail, OrganisationDetails}
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import views.html.organisation.check_answers_page

class CheckAnswersPageViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page = inject[check_answers_page]

  private def createView(): Document =
    page()(registrationJourneyRequest, messages)

  val registrations =
    Table(
      ("Registration Type", "Registration"),
      (SINGLE_ENTITY, aRegistration()),
      (
        GROUP,
        aRegistration(
          withGroupDetail(
            Some(
              GroupDetail(
                membersUnderGroupControl = Some(true),
                members = Seq(
                  GroupMember(
                    customerIdentification1 = "ABC",
                    organisationDetails =
                      Some(GroupMemberOrganisationDetails("Uk Limited", "Subsidiary 1", Some("XP00123"))),
                    addressDetails = Address(
                      addressLine1 = "1",
                      addressLine2 = None,
                      addressLine3 = None,
                      townOrCity = "New Street",
                      maybePostcode = Some("AB12CD"),
                      countryCode = GB
                    )
                  ),
                  GroupMember(
                    customerIdentification1 = "DEF",
                    organisationDetails =
                      Some(GroupMemberOrganisationDetails("Uk Limited", "Subsidiary 2", Some("XP00123"))),
                    addressDetails = Address(
                      addressLine1 = "2",
                      addressLine2 = None,
                      addressLine3 = None,
                      townOrCity = "New Street",
                      maybePostcode = Some("AB12CD"),
                      countryCode = GB
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

  forAll(registrations) { (registrationType, registration) =>
    s"registering a $registrationType" when {
      "displaying organisation details section" when {
        def getKeyFor(index: Int, view: Document) =
          view.getElementsByClass("govuk-summary-list").get(0).getElementsByClass("govuk-summary-list__key").get(index)

        def getValueFor(index: Int, view: Document) =
          view.getElementsByClass("govuk-summary-list").get(0).getElementsByClass("govuk-summary-list__value").get(
            index
          ).text()

        def getChangeLinkFor(index: Int, view: Document) =
          view.getElementsByClass("govuk-summary-list").get(0).getElementsByClass("govuk-link").get(index)

        "registering uk company" in {

          val ukCompanyRegistration   = registration
          val ukCompanyView: Document = createView()

          getKeyFor(0, ukCompanyView) must containMessage("reviewRegistration.organisationDetails.organisationType")
          getKeyFor(1, ukCompanyView) must containMessage("reviewRegistration.organisationDetails.organisationName")
          getKeyFor(2, ukCompanyView) must containMessage(
            "reviewRegistration.organisationDetails.businessRegistrationNumber"
          )
          getKeyFor(3, ukCompanyView) must containMessage(
            "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
          )
          getKeyFor(4, ukCompanyView) must containMessage(
            "reviewRegistration.organisationDetails.registeredBusinessAddress"
          )

          getValueFor(0, ukCompanyView) mustBe OrgType.displayName(UK_COMPANY)
          getValueFor(1, ukCompanyView) mustBe ukCompanyRegistration.organisationDetails.businessName.get
          getValueFor(
            2,
            ukCompanyView
          ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.companyNumber
          getValueFor(
            3,
            ukCompanyView
          ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.ctutr.get
          getValueFor(4, ukCompanyView) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

          getChangeLinkFor(0, ukCompanyView) must haveHref(routes.OrganisationDetailsTypeController.displayPage())
        }

        "registering sole trader" in {

          val soleTraderRegistration = registration.copy(organisationDetails =
            OrganisationDetails(
              organisationType = Some(SOLE_TRADER),
              businessRegisteredAddress = Some(testBusinessAddress),
              soleTraderDetails = Some(soleTraderDetails),
              incorporationDetails = None
            )
          )
          val journeyReq     = registrationJourneyRequest.copy(registration = soleTraderRegistration)
          val soleTraderView = page()(journeyReq, messages)

          getKeyFor(0, soleTraderView) must containMessage("reviewRegistration.organisationDetails.organisationType")
          getKeyFor(1, soleTraderView) must containMessage(
            "reviewRegistration.organisationDetails.soleTrader.firstName"
          )
          getKeyFor(2, soleTraderView) must containMessage("reviewRegistration.organisationDetails.soleTrader.lastName")
          getKeyFor(3, soleTraderView) must containMessage("reviewRegistration.organisationDetails.soleTrader.dob")
          getKeyFor(4, soleTraderView) must containMessage("reviewRegistration.organisationDetails.soleTrader.nino")
          getKeyFor(5, soleTraderView) must containMessage(
            "reviewRegistration.organisationDetails.registeredBusinessAddress"
          )

          getValueFor(0, soleTraderView) mustBe OrgType.displayName(OrgType.SOLE_TRADER)
          getValueFor(
            1,
            soleTraderView
          ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.firstName
          getValueFor(
            2,
            soleTraderView
          ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.lastName
          getValueFor(
            3,
            soleTraderView
          ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.dateOfBirth.get
          getValueFor(
            4,
            soleTraderView
          ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.ninoOrTrn
          getValueFor(5, soleTraderView) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

          getChangeLinkFor(0, soleTraderView) must haveHref(routes.OrganisationDetailsTypeController.displayPage())
        }

        "registering partnership" in {
          val updatedRegistation = registrationJourneyRequest.registration.copy(organisationDetails =
            OrganisationDetails(
              organisationType = Some(PARTNERSHIP),
              businessRegisteredAddress = Some(testBusinessAddress),
              partnershipDetails = Some(generalPartnershipDetails),
              incorporationDetails = None
            )
          )
          val journeyReq      = registrationJourneyRequest.copy(registration = updatedRegistation)
          val partnershipView = page()(journeyReq, messages)

          getKeyFor(0, partnershipView) must containMessage("reviewRegistration.organisationDetails.organisationType")
          getKeyFor(1, partnershipView) must containMessage("reviewRegistration.organisationDetails.partnership.name")
          getKeyFor(2, partnershipView) must containMessage(
            "reviewRegistration.organisationDetails.registeredBusinessAddress"
          )

          getValueFor(0, partnershipView) mustBe PARTNERSHIP.toString
          getValueFor(1, partnershipView) mustBe generalPartnershipDetails.partnershipName.get
          getValueFor(2, partnershipView) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

          getChangeLinkFor(0, partnershipView) must haveHref(routes.OrganisationDetailsTypeController.displayPage())
        }

      }
    }
  }

  def getSummaryRow(view: Document, index: Int): Element =
    view.getElementsByClass("govuk-summary-list__row").get(index)

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f()(registrationJourneyRequest, messages)
    page.render(registrationJourneyRequest, messages)
  }

}
