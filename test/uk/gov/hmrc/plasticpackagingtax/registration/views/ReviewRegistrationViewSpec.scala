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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact.{routes => contactRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OldDate
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{OrgType, PartnerTypeEnum}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupMember,
  GroupMemberContactDetails,
  OrganisationDetails => GroupMemberOrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  GroupDetail,
  LiabilityDetails,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.gdsPageHeading
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page = inject[review_registration_page]

  private val liabilitySection        = 0
  private val organisationSection     = 1
  private val contactDetailsSection   = 2
  private val nominatedPartnerSection = 2

  private val liabilityStartLink = Call("GET", "/liabilityStartLink")

  private def createView(reg: Registration): Document =
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
      view.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
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

      val registrations =
        Table(("Registration Type", "Registration"),
              (SINGLE_ENTITY, aRegistration()),
              (GROUP,
               aRegistration(
                 withGroupDetail(
                   Some(
                     GroupDetail(membersUnderGroupControl = Some(true),
                                 members = Seq(GroupMember(customerIdentification1 = "ABC",
                                                           customerIdentification2 = Some("123"),
                                                           organisationDetails = Some(
                                                             GroupMemberOrganisationDetails(
                                                               "UkCompany",
                                                               "Subsidiary 1",
                                                               Some("XP00123")
                                                             )
                                                           ),
                                                           contactDetails = Some(
                                                             GroupMemberContactDetails(
                                                               firstName = "Test",
                                                               lastName = "User",
                                                               phoneNumber = Some("077123"),
                                                               email = Some("test@test.com"),
                                                               address = Some(
                                                                 Address(addressLine1 = "1",
                                                                         townOrCity = "New Street",
                                                                         postCode = Some("AB12CD"),
                                                                         countryCode =
                                                                           "GB"
                                                                 )
                                                               )
                                                             )
                                                           ),
                                                           addressDetails =
                                                             Address(addressLine1 = "1",
                                                                     townOrCity = "New Street",
                                                                     postCode = Some("AB12CD"),
                                                                     countryCode =
                                                                       "GB"
                                                             )
                                               ),
                                               GroupMember(customerIdentification1 = "DEF",
                                                           customerIdentification2 = Some("456"),
                                                           organisationDetails = Some(
                                                             GroupMemberOrganisationDetails(
                                                               "UkCompany",
                                                               "Subsidiary 2",
                                                               Some("XP00456")
                                                             )
                                                           ),
                                                           contactDetails = Some(
                                                             GroupMemberContactDetails(
                                                               firstName = "Test2",
                                                               lastName = "User2",
                                                               phoneNumber = Some("077124"),
                                                               email = Some("test2@test.com"),
                                                               address = Some(
                                                                 Address(addressLine1 = "2",
                                                                         townOrCity = "New Street",
                                                                         postCode = Some("AB12CE"),
                                                                         countryCode =
                                                                           "GB"
                                                                 )
                                                               )
                                                             )
                                                           ),
                                                           addressDetails =
                                                             Address(addressLine1 = "2",
                                                                     townOrCity = "New Street",
                                                                     postCode = Some("AB12CD"),
                                                                     countryCode =
                                                                       "GB"
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

            "preLaunch" in {
              val liabilityView =
                page(
                  registration = registration.copy(liabilityDetails =
                    LiabilityDetails(
                      expectedWeight =
                        Some(LiabilityExpectedWeight(Some(true), totalKg = Some(11000))),
                      isLiable = Some(true)
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

            "postLaunch and <10,000kg but expect to exceed" in {
              val liabilityView =
                page(
                  registration = registration.copy(liabilityDetails =
                    LiabilityDetails(weight = Some(LiabilityWeight(totalKg = Some(1000))),
                                     startDate = Some(OldDate(Some(1), Some(11), Some(2022))),
                                     expectToExceedThresholdWeight = Some(true)
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
                liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
              )
              getChangeLinkFor(liabilitySection, 2, liabilityView) must haveHref(
                liabilityRoutes.LiabilityStartDateController.displayPage()
              )
            }

            "postLaunch and >10,000kg" in {
              val liabilityView =
                page(
                  registration = registration.copy(liabilityDetails =
                    LiabilityDetails(weight = Some(LiabilityWeight(totalKg = Some(11000))),
                                     startDate = Some(OldDate(Some(1), Some(11), Some(2022))),
                                     expectToExceedThresholdWeight = None
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
                liabilityRoutes.LiabilityStartDateController.displayPage()
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
              getValueFor(organisationSection,
                          1
              ) mustBe ukCompanyRegistration.organisationDetails.businessName.get
              getValueFor(organisationSection,
                          2,
                          ukCompanyView
              ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.companyNumber
              getValueFor(organisationSection,
                          3,
                          ukCompanyView
              ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.ctutr
              getValueFor(organisationSection,
                          4,
                          ukCompanyView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

            }

            "registering sole trader" in {

              val soleTraderRegistration = registration.copy(organisationDetails =
                OrganisationDetails(organisationType = Some(SOLE_TRADER),
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

              getValueFor(organisationSection, 0, soleTraderView) mustBe OrgType.displayName(
                OrgType.SOLE_TRADER
              )
              getValueFor(organisationSection,
                          1,
                          soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.firstName
              getValueFor(organisationSection,
                          2,
                          soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.lastName
              getValueFor(organisationSection,
                          3,
                          soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.dateOfBirth.get
              getValueFor(organisationSection,
                          4,
                          soleTraderView
              ) mustBe soleTraderRegistration.organisationDetails.soleTraderDetails.get.ninoOrTrn
              getValueFor(organisationSection,
                          5,
                          soleTraderView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

            }

            "registering partnership" in {

              val partnershipRegistration = registration.copy(organisationDetails =
                OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                    businessRegisteredAddress = Some(testBusinessAddress),
                                    partnershipDetails =
                                      Some(generalPartnershipDetailsWithPartners),
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
              getValueFor(organisationSection,
                          1,
                          partnershipView
              ) mustBe generalPartnershipDetailsWithPartners.partnershipOrCompanyName.get
              getValueFor(organisationSection,
                          2,
                          partnershipView
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
            }

          }

          "displaying primary contact details section" in {

            getKeyFor(contactDetailsSection, 0) must containMessage(
              "primaryContactDetails.check.fullName"
            )
            getKeyFor(contactDetailsSection, 1) must containMessage(
              "primaryContactDetails.check.jobTitle"
            )
            getKeyFor(contactDetailsSection, 2) must containMessage(
              "primaryContactDetails.check.email"
            )
            getKeyFor(contactDetailsSection, 3) must containMessage(
              "primaryContactDetails.check.phoneNumber"
            )
            getKeyFor(contactDetailsSection, 4) must containMessage(
              "primaryContactDetails.check.address"
            )

            getValueFor(contactDetailsSection, 0) mustBe "Jack Gatsby"
            getValueFor(contactDetailsSection,
                        1
            ) mustBe registration.primaryContactDetails.jobTitle.get
            getValueFor(contactDetailsSection,
                        2
            ) mustBe registration.primaryContactDetails.email.get
            getValueFor(contactDetailsSection,
                        3
            ) mustBe registration.primaryContactDetails.phoneNumber.get
            getValueFor(contactDetailsSection,
                        4
            ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"

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
                registration.groupDetail.get.members.zipWithIndex.foreach {
                  case (member, idx) =>
                    groupMembersView.getElementById(
                      s"group-members-heading-${idx + 1}"
                    ).text() must include(member.businessName)

                    val groupMemberRows =
                      groupMembersView.select(s"div#group-members-content-${idx + 1} dl div")

                    verifyRowContent(groupMemberRows.get(0),
                                     messages("reviewRegistration.member.orgName"),
                                     member.organisationDetails.get.organisationName,
                                     None
                    )
                    verifyRowContent(groupMemberRows.get(1),
                                     messages("reviewRegistration.member.orgType"),
                                     "UK limited company",
                                     Some(
                                       groupRoutes.OrganisationDetailsTypeController.displayPageAmendMember(
                                         member.id
                                       )
                                     )
                    )
                    verifyRowContent(groupMemberRows.get(2),
                                     messages("reviewRegistration.member.companyNumber"),
                                     member.customerIdentification1,
                                     None
                    )
                    verifyRowContent(groupMemberRows.get(3),
                                     messages("reviewRegistration.member.utr"),
                                     member.customerIdentification2.get,
                                     None
                    )
                    verifyRowContent(groupMemberRows.get(4),
                                     messages("reviewRegistration.member.contact.name"),
                                     member.contactDetails.get.groupMemberName,
                                     Some(
                                       groupRoutes.ContactDetailsNameController.displayPage(
                                         member.id
                                       )
                                     )
                    )
                    verifyRowContent(groupMemberRows.get(5),
                                     messages("reviewRegistration.member.contact.email"),
                                     member.contactDetails.get.email.get,
                                     Some(
                                       groupRoutes.ContactDetailsEmailAddressController.displayPage(
                                         member.id
                                       )
                                     )
                    )
                    verifyRowContent(groupMemberRows.get(6),
                                     messages("reviewRegistration.member.contact.phone"),
                                     member.contactDetails.get.phoneNumber.get,
                                     Some(
                                       groupRoutes.ContactDetailsTelephoneNumberController.displayPage(
                                         member.id
                                       )
                                     )
                    )
                    verifyRowContent(groupMemberRows.get(7),
                                     messages("reviewRegistration.member.contact.address"),
                                     member.contactDetails.get.address.get.addressLine1,
                                     Some(
                                       groupRoutes.ContactDetailsConfirmAddressController.displayPage(
                                         member.id
                                       )
                                     )
                    )
                }
            }
          }

          "displaying nominated partner details" in {
            val partnershipRegistration = registration.copy(organisationDetails =
              OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                  businessRegisteredAddress = Some(testBusinessAddress),
                                  partnershipDetails =
                                    Some(generalPartnershipDetailsWithPartners),
                                  incorporationDetails = None
              )
            )
            val journeyRequestWithEnrolledUser: JourneyRequest[AnyContent] =
              JourneyRequest(authenticatedRequest =
                               new AuthenticatedRequest(FakeRequest(), userWithPPTEnrolment),
                             registration = partnershipRegistration,
                             appConfig = appConfig
              )
            val partnershipView =
              page(partnershipRegistration, liabilityStartLink)(journeyRequestWithEnrolledUser,
                                                                messages
              )

            getKeyFor(nominatedPartnerSection, 0, partnershipView) must containMessage(
              "reviewRegistration.partner.orgType"
            )
            getKeyFor(nominatedPartnerSection, 1, partnershipView) must containMessage(
              "reviewRegistration.partner.name"
            )
            getKeyFor(nominatedPartnerSection, 2, partnershipView) must containMessage(
              "reviewRegistration.partner.dob"
            )
            getKeyFor(nominatedPartnerSection, 3, partnershipView) must containMessage(
              "reviewRegistration.partner.nino"
            )
            getKeyFor(nominatedPartnerSection, 4, partnershipView) must containMessage(
              "reviewRegistration.partner.utr"
            )
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
            getValueFor(nominatedPartnerSection,
                        0,
                        partnershipView
            ) mustBe PartnerTypeEnum.displayName(nominatedPartner.partnerType)
            getValueFor(nominatedPartnerSection, 1, partnershipView) mustBe nominatedPartner.name
            getValueFor(nominatedPartnerSection,
                        2,
                        partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.dateOfBirth.get
            getValueFor(nominatedPartnerSection,
                        3,
                        partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.ninoOrTrn
            getValueFor(nominatedPartnerSection,
                        4,
                        partnershipView
            ) mustBe nominatedPartner.soleTraderDetails.get.sautr.get
            getValueFor(nominatedPartnerSection,
                        5,
                        partnershipView
            ) mustBe nominatedPartner.contactDetails.get.name.get
            getValueFor(nominatedPartnerSection,
                        6,
                        partnershipView
            ) mustBe nominatedPartner.contactDetails.get.jobTitle.get
            getValueFor(nominatedPartnerSection,
                        7,
                        partnershipView
            ) mustBe nominatedPartner.contactDetails.get.emailAddress.get
            getValueFor(nominatedPartnerSection,
                        8,
                        partnershipView
            ) mustBe nominatedPartner.contactDetails.get.phoneNumber.get
            getValueFor(nominatedPartnerSection,
                        9,
                        partnershipView
            ) mustBe "1 High Street Leeds LS1 1AA United Kingdom"

          }

          "displaying other partner details" in {
            val partnershipRegistration = registration.copy(organisationDetails =
              OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                  businessRegisteredAddress = Some(testBusinessAddress),
                                  partnershipDetails =
                                    Some(
                                      generalPartnershipDetails.copy(partners =
                                        Seq(aSoleTraderPartner(), aLimitedCompanyPartner())
                                      )
                                    ),
                                  incorporationDetails = None
              )
            )
            val journeyRequestWithEnrolledUser: JourneyRequest[AnyContent] =
              JourneyRequest(authenticatedRequest =
                               new AuthenticatedRequest(FakeRequest(), userWithPPTEnrolment),
                             registration = partnershipRegistration,
                             appConfig = appConfig
              )
            val otherPartnersView =
              page(partnershipRegistration, liabilityStartLink)(journeyRequestWithEnrolledUser,
                                                                messages
              )

            partnershipRegistration.otherPartners.zipWithIndex.foreach {
              case (partner, idx) =>
                otherPartnersView.getElementById(
                  s"other-partners-heading-${idx + 1}"
                ).text() must include(partner.name)

                val otherPartnerRows =
                  otherPartnersView.select(s"div#other-partners-content-${idx + 1} dl div")

                verifyRowContent(otherPartnerRows.get(0),
                                 messages("reviewRegistration.partner.orgType"),
                                 PartnerTypeEnum.displayName(partner.partnerType),
                                 None
                )
                verifyRowContent(otherPartnerRows.get(1),
                                 messages("reviewRegistration.partner.companyNumber"),
                                 partner.incorporationDetails.get.companyNumber,
                                 None
                )
                verifyRowContent(otherPartnerRows.get(4),
                                 messages("reviewRegistration.partner.contact.name"),
                                 partner.contactDetails.get.name.get,
                                 Some(
                                   partnerRoutes.PartnerContactNameController.displayExistingPartner(
                                     partner.id
                                   )
                                 )
                )
                verifyRowContent(otherPartnerRows.get(5),
                                 messages("reviewRegistration.partner.contact.email"),
                                 partner.contactDetails.get.emailAddress.get,
                                 Some(
                                   partnerRoutes.PartnerEmailAddressController.displayExistingPartner(
                                     partner.id
                                   )
                                 )
                )
                verifyRowContent(otherPartnerRows.get(6),
                                 messages("reviewRegistration.partner.contact.phone"),
                                 partner.contactDetails.get.phoneNumber.get,
                                 Some(
                                   partnerRoutes.PartnerPhoneNumberController.displayExistingPartner(
                                     partner.id
                                   )
                                 )
                )
                verifyRowContent(otherPartnerRows.get(7),
                                 messages("reviewRegistration.partner.contact.address"),
                                 partner.contactDetails.get.address.get.addressLine1,
                                 Some(
                                   partnerRoutes.PartnerContactAddressController.captureExistingPartner(
                                     partner.id
                                   )
                                 )
                )

            }
          }
        }
      }
    }

    "display 'Accept and send' button" in {

      view.getElementsByClass("govuk-button").text() must include(
        messages("site.button.acceptAndSend")
      )
    }
  }

  private def verifyRowContent(
    row: Element,
    label: String,
    content: String,
    changeLink: Option[Call]
  ) = {
    row.text() must include(label)
    row.text() must include(content)

    changeLink match {
      case Some(changeLink) => row.select("a").get(0) must haveHref(changeLink.url)
      case None             =>
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    val registration = aRegistration()
    page.f(registration, liabilityStartLink)(journeyRequest, messages)
    page.render(registration, liabilityStartLink, journeyRequest, messages)
  }

}
