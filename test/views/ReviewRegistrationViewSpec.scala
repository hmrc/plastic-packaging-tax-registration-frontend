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

package views

import base.unit.UnitViewSpec
import controllers.contact.{routes => contactRoutes}
import controllers.group.{routes => groupRoutes}
import controllers.liability.{routes => liabilityRoutes}
import controllers.partner.{routes => partnerRoutes}
import controllers.routes
import forms.contact.Address
import forms.liability.LiabilityWeight
import forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import forms.organisation.OrgType.{PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import forms.organisation.{OrgType, PartnerTypeEnum}
import forms.{Date, OldDate}
import models.addresslookup.CountryCode.GB
import models.registration.group.{GroupMember, GroupMemberContactDetails, OrganisationDetails => GroupMemberOrganisationDetails}
import models.registration.{GroupDetail, LiabilityDetails, OrganisationDetails, Registration}
import models.request.JourneyRequest
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContent, Call}
import views.components.Styles.gdsPageHeading
import views.html.review_registration_page

import java.time.LocalDate

class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page = inject[review_registration_page]

  private val liabilitySection        = 0
  private val organisationSection     = 1
  private val contactDetailsSection   = 2
  private val nominatedPartnerSection = 2

  private def createView(reg: Registration): Document =
    page(reg)(registrationJourneyRequest, messages)

  "Review registration View" should {

    val view: Document = createView(aRegistration())

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
    }

    "display meta title" in {
      view.select("title").text() must include(messages("reviewRegistration.organisationDetails.title"))
    }

    "display title" in {
      view.getElementsByClass(gdsPageHeading).first() must containMessage(
        "reviewRegistration.organisationDetails.title"
      )
    }

    "display labels, values and change links" when {

      def getKeyFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass("govuk-summary-list__key").get(
          index
        )

      def getValueFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass("govuk-summary-list__value").get(
          index
        ).text()

      def getChangeLinkFor(section: Int, index: Int, view: Document = view) =
        view.getElementsByClass("govuk-summary-list").get(section).getElementsByClass("govuk-link").get(index)

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
                        customerIdentification2 = Some("123"),
                        organisationDetails =
                          Some(GroupMemberOrganisationDetails("UkCompany", "Subsidiary 1", Some("XP00123"))),
                        contactDetails = Some(
                          GroupMemberContactDetails(
                            firstName = "Test",
                            lastName = "User",
                            phoneNumber = Some("077123"),
                            email = Some("test@test.com"),
                            address = Some(
                              Address(
                                addressLine1 = "1",
                                addressLine2 = None,
                                addressLine3 = None,
                                townOrCity = "New Street",
                                maybePostcode = Some("AB12CD"),
                                countryCode = GB
                              )
                            )
                          )
                        ),
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
                        customerIdentification2 = Some("456"),
                        organisationDetails =
                          Some(GroupMemberOrganisationDetails("UkCompany", "Subsidiary 2", Some("XP00456"))),
                        contactDetails = Some(
                          GroupMemberContactDetails(
                            firstName = "Test2",
                            lastName = "User2",
                            phoneNumber = Some("077124"),
                            email = Some("test2@test.com"),
                            address = Some(
                              Address(
                                addressLine1 = "2",
                                addressLine2 = None,
                                addressLine3 = None,
                                townOrCity = "New Street",
                                maybePostcode = Some("AB12CE"),
                                countryCode = GB
                              )
                            )
                          )
                        ),
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

          "displaying liability details section" when {

            "already exceeded threshold" in {
              val liabilityView =
                page(registration =
                  registration.copy(liabilityDetails =
                    LiabilityDetails(
                      exceededThresholdWeight = Some(true),
                      dateExceededThresholdWeight = Some(Date(LocalDate.parse("2022-03-05"))),
                      expectedWeightNext12m = Some(LiabilityWeight(Some(12000))),
                      startDate = Some(OldDate(Some(1), Some(4), Some(2022)))
                    )
                  )
                )(registrationJourneyRequest, messages = messages)

              getKeyFor(liabilitySection, 0, liabilityView) must containMessage(
                "liability.checkAnswers.exceededThreshold"
              )
              getKeyFor(liabilitySection, 1, liabilityView) must containMessage(
                "liability.checkAnswers.dateExceededThreshold"
              )
              getKeyFor(liabilitySection, 2, liabilityView) must containMessage("liability.checkAnswers.startDate")

              getValueFor(liabilitySection, 0, liabilityView) mustBe "Yes"
              getValueFor(liabilitySection, 1, liabilityView) mustBe "5 March 2022"
              getValueFor(liabilitySection, 2, liabilityView) mustBe "1 April 2022"

              getChangeLinkFor(liabilitySection, 0, liabilityView) must haveHref(
                controllers.liability.routes.ExceededThresholdWeightController.displayPage.url
              )
              getChangeLinkFor(liabilitySection, 1, liabilityView) must haveHref(
                liabilityRoutes.ExceededThresholdWeightDateController.displayPage()
              )
            }

            "not already exceeded threshold but expect to in next 30d" in {
              val liabilityView =
                page(registration =
                  registration.copy(liabilityDetails =
                    LiabilityDetails(
                      exceededThresholdWeight = Some(false),
                      expectToExceedThresholdWeight = Some(true),
                      dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.parse("2022-03-06"))),
                      expectedWeightNext12m = Some(LiabilityWeight(Some(12000))),
                      startDate = Some(OldDate(Some(6), Some(3), Some(2022)))
                    )
                  )
                )(registrationJourneyRequest, messages = messages)

              getKeyFor(liabilitySection, 0, liabilityView) must containMessage(
                "liability.checkAnswers.expectToExceededThreshold"
              )
              getKeyFor(liabilitySection, 1, liabilityView) must containMessage(
                "liability.checkAnswers.dateRealisedExpectToExceededThreshold"
              )
              getKeyFor(liabilitySection, 2, liabilityView) must containMessage(
                "liability.checkAnswers.exceededThreshold"
              )
              getKeyFor(liabilitySection, 3, liabilityView) must containMessage("liability.checkAnswers.startDate")

              getValueFor(liabilitySection, 0, liabilityView) mustBe "Yes"
              getValueFor(liabilitySection, 1, liabilityView) mustBe "6 March 2022"
              getValueFor(liabilitySection, 2, liabilityView) mustBe "No"
              getValueFor(liabilitySection, 3, liabilityView) mustBe "6 March 2022"

              getChangeLinkFor(liabilitySection, 0, liabilityView) must haveHref(
                liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage().url
              )
              getChangeLinkFor(liabilitySection, 1, liabilityView) must haveHref(
                liabilityRoutes.ExpectToExceedThresholdWeightDateController.displayPage
              )
              getChangeLinkFor(liabilitySection, 2, liabilityView) must haveHref(
                liabilityRoutes.ExceededThresholdWeightController.displayPage
              )
            }

          }

          "displaying organisation details section" when {

            "registering uk company" in {

              val ukCompanyRegistration   = registration
              val ukCompanyView: Document = createView(ukCompanyRegistration)

              getKeyFor(organisationSection, 0, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.organisationType"
              )
              getKeyFor(organisationSection, 1, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.organisationName"
              )
              getKeyFor(organisationSection, 2, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.businessRegistrationNumber"
              )
              getKeyFor(organisationSection, 3, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
              )
              getKeyFor(organisationSection, 4, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.registeredBusinessAddress"
              )

              getValueFor(organisationSection, 0) mustBe OrgType.displayName(UK_COMPANY)
              getValueFor(organisationSection, 1) mustBe ukCompanyRegistration.organisationDetails.businessName.get
              getValueFor(
                organisationSection,
                2,
                ukCompanyView
              ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.companyNumber
              getValueFor(
                organisationSection,
                3,
                ukCompanyView
              ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.ctutr.get
              getValueFor(
                organisationSection,
                4,
                ukCompanyView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

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
              val soleTraderView = createView(soleTraderRegistration)

              getKeyFor(organisationSection, 0, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.organisationType"
              )
              getKeyFor(organisationSection, 1, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.soleTrader.firstName"
              )
              getKeyFor(organisationSection, 2, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.soleTrader.lastName"
              )
              getKeyFor(organisationSection, 3, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.soleTrader.dob"
              )
              getKeyFor(organisationSection, 4, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.soleTrader.nino"
              )
              getKeyFor(organisationSection, 5, soleTraderView) must containMessage(
                "reviewRegistration.organisationDetails.registeredBusinessAddress"
              )

              getValueFor(organisationSection, 0, soleTraderView) mustBe OrgType.displayName(OrgType.SOLE_TRADER)
              getValueFor(
                organisationSection,
                1,
                soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.firstName
              getValueFor(
                organisationSection,
                2,
                soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.lastName
              getValueFor(
                organisationSection,
                3,
                soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.dateOfBirth.get
              getValueFor(
                organisationSection,
                4,
                soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.ninoOrTrn
              getValueFor(
                organisationSection,
                5,
                soleTraderView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

            }

            "registering partnership" in {

              val partnershipRegistration = registration.copy(organisationDetails =
                OrganisationDetails(
                  organisationType = Some(PARTNERSHIP),
                  businessRegisteredAddress = Some(testBusinessAddress),
                  partnershipDetails = Some(generalPartnershipDetailsWithPartners),
                  incorporationDetails = None
                )
              )
              val partnershipView = createView(partnershipRegistration)

              getKeyFor(organisationSection, 0, partnershipView) must containMessage(
                "reviewRegistration.organisationDetails.organisationType"
              )
              getKeyFor(organisationSection, 1, partnershipView) must containMessage(
                "reviewRegistration.organisationDetails.partnership.name"
              )
              getKeyFor(organisationSection, 2, partnershipView) must containMessage(
                "reviewRegistration.organisationDetails.registeredBusinessAddress"
              )

              getValueFor(organisationSection, 0, partnershipView) mustBe PARTNERSHIP.toString
              getValueFor(
                organisationSection,
                1,
                partnershipView
              ) mustBe generalPartnershipDetailsWithPartners.partnershipOrCompanyName.get
              getValueFor(
                organisationSection,
                2,
                partnershipView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
            }

          }

          "displaying primary contact details section" in {

            getKeyFor(contactDetailsSection, 0) must containMessage("primaryContactDetails.check.fullName")
            getKeyFor(contactDetailsSection, 1) must containMessage("primaryContactDetails.check.jobTitle")
            getKeyFor(contactDetailsSection, 2) must containMessage("primaryContactDetails.check.email")
            getKeyFor(contactDetailsSection, 3) must containMessage("primaryContactDetails.check.phoneNumber")
            getKeyFor(contactDetailsSection, 4) must containMessage("primaryContactDetails.check.address")

            getValueFor(contactDetailsSection, 0) mustBe "Jack Gatsby"
            getValueFor(contactDetailsSection, 1) mustBe registration.primaryContactDetails.jobTitle.get
            getValueFor(contactDetailsSection, 2) mustBe registration.primaryContactDetails.email.get
            getValueFor(contactDetailsSection, 3) mustBe registration.primaryContactDetails.phoneNumber.get
            getValueFor(contactDetailsSection, 4) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

            getChangeLinkFor(contactDetailsSection, 0) must haveHref(
              contactRoutes.ContactDetailsFullNameController.displayPage()
            )
            getChangeLinkFor(contactDetailsSection, 1) must haveHref(
              contactRoutes.ContactDetailsJobTitleController.displayPage()
            )
            getChangeLinkFor(contactDetailsSection, 2) must haveHref(
              contactRoutes.ContactDetailsEmailAddressController.displayPage()
            )
            getChangeLinkFor(contactDetailsSection, 3) must haveHref(
              contactRoutes.ContactDetailsTelephoneNumberController.displayPage()
            )
            getChangeLinkFor(contactDetailsSection, 4) must haveHref(
              contactRoutes.ContactDetailsConfirmAddressController.displayPage()
            )
          }

          "displaying group member details" in {
            val groupMembersView = createView(registration)

            registrationType match {
              case SINGLE_ENTITY =>
                groupMembersView.getElementsByClass(
                  "govuk-summary-list"
                ).size mustBe 3 // Group member section not displayed!
              case GROUP =>
                registration.groupDetail.get.members.zipWithIndex.foreach { case (member, idx) =>
                  groupMembersView.getElementById(s"group-members-heading-${idx + 1}").text() must include(
                    member.businessName
                  )

                  val groupMemberRows =
                    groupMembersView.select(s"div#group-members-content-${idx + 1} dl div")

                  verifyRowContent(
                    groupMemberRows.get(0),
                    messages("reviewRegistration.member.orgName"),
                    member.organisationDetails.get.organisationName,
                    None
                  )
                  verifyRowContent(
                    groupMemberRows.get(1),
                    messages("reviewRegistration.member.orgType"),
                    "UK limited company",
                    Some(groupRoutes.OrganisationDetailsTypeController.displayPageAmendMember(member.id))
                  )
                  verifyRowContent(
                    groupMemberRows.get(2),
                    messages("reviewRegistration.member.companyNumber"),
                    member.customerIdentification1,
                    None
                  )
                  verifyRowContent(
                    groupMemberRows.get(3),
                    messages("reviewRegistration.member.utr"),
                    member.customerIdentification2.get,
                    None
                  )
                  verifyRowContent(
                    groupMemberRows.get(4),
                    messages("reviewRegistration.member.contact.name"),
                    member.contactDetails.get.groupMemberName,
                    Some(groupRoutes.ContactDetailsNameController.displayPage(member.id))
                  )
                  verifyRowContent(
                    groupMemberRows.get(5),
                    messages("reviewRegistration.member.contact.email"),
                    member.contactDetails.get.email.get,
                    Some(groupRoutes.ContactDetailsEmailAddressController.displayPage(member.id))
                  )
                  verifyRowContent(
                    groupMemberRows.get(6),
                    messages("reviewRegistration.member.contact.phone"),
                    member.contactDetails.get.phoneNumber.get,
                    Some(groupRoutes.ContactDetailsTelephoneNumberController.displayPage(member.id))
                  )
                  verifyRowContent(
                    groupMemberRows.get(7),
                    messages("reviewRegistration.member.contact.address"),
                    member.contactDetails.get.address.get.addressLine1,
                    Some(groupRoutes.ContactDetailsConfirmAddressController.displayPage(member.id))
                  )
                }
              case other => throw new IllegalStateException(s"Invalid registration type: $other")
            }
          }

          "displaying nominated partner details" in {
            val partnershipRegistration = registration.copy(organisationDetails =
              OrganisationDetails(
                organisationType = Some(PARTNERSHIP),
                businessRegisteredAddress = Some(testBusinessAddress),
                partnershipDetails = Some(generalPartnershipDetailsWithPartners),
                incorporationDetails = None
              )
            )
            val journeyRequestWithEnrolledUser: JourneyRequest[AnyContent] =
              registrationJourneyRequest.copy(registration = partnershipRegistration)
            val partnershipView =
              page(partnershipRegistration)(journeyRequestWithEnrolledUser, messages)

            getKeyFor(nominatedPartnerSection, 0, partnershipView) must containMessage(
              "reviewRegistration.partner.orgType"
            )
            getKeyFor(nominatedPartnerSection, 1, partnershipView) must containMessage(
              "reviewRegistration.partner.name"
            )
            getKeyFor(nominatedPartnerSection, 2, partnershipView) must containMessage("reviewRegistration.partner.dob")
            getKeyFor(nominatedPartnerSection, 3, partnershipView) must containMessage(
              "reviewRegistration.partner.nino"
            )
            getKeyFor(nominatedPartnerSection, 4, partnershipView) must containMessage("reviewRegistration.partner.utr")
            getKeyFor(nominatedPartnerSection, 5, partnershipView) must containMessage(
              "reviewRegistration.partner.contact.name"
            )
            getKeyFor(nominatedPartnerSection, 6, partnershipView) must containMessage(
              "reviewRegistration.partner.contact.jobTitle"
            )
            getKeyFor(nominatedPartnerSection, 7, partnershipView) must containMessage(
              "reviewRegistration.partner.contact.email"
            )
            getKeyFor(nominatedPartnerSection, 8, partnershipView) must containMessage(
              "reviewRegistration.partner.contact.phone"
            )
            getKeyFor(nominatedPartnerSection, 9, partnershipView) must containMessage(
              "reviewRegistration.partner.contact.address"
            )
            val nominatedPartner = partnershipRegistration.nominatedPartner.get
            getValueFor(nominatedPartnerSection, 0, partnershipView) mustBe PartnerTypeEnum.displayName(
              nominatedPartner.partnerType
            )
            getValueFor(nominatedPartnerSection, 1, partnershipView) mustBe nominatedPartner.name
            getValueFor(
              nominatedPartnerSection,
              2,
              partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.dateOfBirth.get
            getValueFor(
              nominatedPartnerSection,
              3,
              partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.ninoOrTrn
            getValueFor(
              nominatedPartnerSection,
              4,
              partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.sautr.get
            getValueFor(nominatedPartnerSection, 5, partnershipView) mustBe nominatedPartner.contactDetails.get.name.get
            getValueFor(
              nominatedPartnerSection,
              6,
              partnershipView
            ) mustBe nominatedPartner.contactDetails.get.jobTitle.get
            getValueFor(
              nominatedPartnerSection,
              7,
              partnershipView
            ) mustBe nominatedPartner.contactDetails.get.emailAddress.get
            getValueFor(
              nominatedPartnerSection,
              8,
              partnershipView
            ) mustBe nominatedPartner.contactDetails.get.phoneNumber.get
            getValueFor(
              nominatedPartnerSection,
              9,
              partnershipView
            ) mustBe "1 High Street Cloverfield Leeds LS1 1AA United Kingdom"

          }

          "displaying other partner details" in {
            val partnershipRegistration = registration.copy(organisationDetails =
              OrganisationDetails(
                organisationType = Some(PARTNERSHIP),
                businessRegisteredAddress = Some(testBusinessAddress),
                partnershipDetails = Some(
                  generalPartnershipDetails.copy(partners = Seq(aSoleTraderPartner, aLimitedCompanyPartner))
                ),
                incorporationDetails = None
              )
            )
            val journeyRequestWithEnrolledUser: JourneyRequest[AnyContent] =
              registrationJourneyRequest.copy(registration = partnershipRegistration)
            val otherPartnersView =
              page(partnershipRegistration)(journeyRequestWithEnrolledUser, messages)

            partnershipRegistration.otherPartners.zipWithIndex.foreach { case (partner, idx) =>
              otherPartnersView.getElementById(s"other-partners-heading-${idx + 1}").text() must include(partner.name)

              val otherPartnerRows =
                otherPartnersView.select(s"div#other-partners-content-${idx + 1} dl div")

              verifyRowContent(
                otherPartnerRows.get(0),
                messages("reviewRegistration.partner.orgType"),
                PartnerTypeEnum.displayName(partner.partnerType),
                None
              )
              verifyRowContent(
                otherPartnerRows.get(1),
                messages("reviewRegistration.partner.companyNumber"),
                partner.incorporationDetails.get.companyNumber,
                None
              )
              verifyRowContent(
                otherPartnerRows.get(4),
                messages("reviewRegistration.partner.contact.name"),
                partner.contactDetails.get.name.get,
                Some(partnerRoutes.PartnerContactNameController.displayExistingPartner(partner.id))
              )
              verifyRowContent(
                otherPartnerRows.get(5),
                messages("reviewRegistration.partner.contact.email"),
                partner.contactDetails.get.emailAddress.get,
                Some(partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partner.id))
              )
              verifyRowContent(
                otherPartnerRows.get(6),
                messages("reviewRegistration.partner.contact.phone"),
                partner.contactDetails.get.phoneNumber.get,
                Some(partnerRoutes.PartnerPhoneNumberController.displayExistingPartner(partner.id))
              )
              verifyRowContent(
                otherPartnerRows.get(7),
                messages("reviewRegistration.partner.contact.address"),
                partner.contactDetails.get.address.get.addressLine1,
                Some(partnerRoutes.PartnerContactAddressController.captureExistingPartner(partner.id))
              )

            }
          }
        }
      }
    }

    "display 'Accept and send' button" in {

      view.getElementsByClass("govuk-button").text() must include(messages("site.button.acceptAndSend"))
    }
  }

  private def verifyRowContent(row: Element, label: String, content: String, changeLink: Option[Call]) = {
    row.text() must include(label)
    row.text() must include(content)

    changeLink match {
      case Some(changeLink) => row.select("a").get(0) must haveHref(changeLink.url)
      case None             =>
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    val registration = aRegistration()
    page.f(registration)(registrationJourneyRequest, messages)
    page.render(registration, registrationJourneyRequest, messages)
  }

}
