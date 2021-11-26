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
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact.{routes => contactRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType.{GROUP, SINGLE_ENTITY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupMember,
  OrganisationDetails => GroupMemberOrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  GroupDetail,
  LiabilityDetails,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.gdsPageHeading
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReviewRegistrationViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page           = instanceOf[review_registration_page]
  private val countryService = instanceOf[CountryService]

  private val liabilitySection      = 0
  private val organisationSection   = 1
  private val contactDetailsSection = 2

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
                                                           organisationDetails = Some(
                                                             GroupMemberOrganisationDetails(
                                                               "Uk Limited",
                                                               "Subsidiary 1",
                                                               Some("XP00123")
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
                                                           organisationDetails = Some(
                                                             GroupMemberOrganisationDetails(
                                                               "Uk Limited",
                                                               "Subsidiary 2",
                                                               Some("XP00123")
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
                                     startDate = Some(Date(Some(1), Some(11), Some(2022))),
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
                                     startDate = Some(Date(Some(1), Some(11), Some(2022))),
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
                "reviewRegistration.organisationDetails.registeredBusinessAddress"
              )
              getKeyFor(organisationSection, 4, ukCompanyView) must containMessage(
                "reviewRegistration.organisationDetails.uniqueTaxpayerReference"
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
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
              getValueFor(organisationSection,
                          4,
                          ukCompanyView
              ) mustBe ukCompanyRegistration.organisationDetails.incorporationDetails.get.ctutr

            }

            "registering sole trader" in {

              val soleTraderRegistration = registration.copy(organisationDetails =
                OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                    businessRegisteredAddress = Some(testBusinessAddress),
                                    soleTraderDetails = Some(soleTraderIncorporationDetails),
                                    incorporationDetails = None
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
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
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

            "registering partnership" in {

              val partnershipRegistration = registration.copy(organisationDetails =
                OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                    businessRegisteredAddress = Some(testBusinessAddress),
                                    partnershipDetails = Some(partnershipDetails),
                                    incorporationDetails = None
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
              ) mustBe "2 Scala Street Soho London W1T 2HN United Kingdom"
              getValueFor(organisationSection, 2, partnershipView) mustBe PARTNERSHIP.toString
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
                    val groupMemberContent =
                      groupMembersView.getElementById(s"group-members-content-${idx + 1}").text()
                    groupMemberContent must include(
                      messages("reviewRegistration.organisationDetails.organisationName")
                    )
                    groupMemberContent must include(
                      messages("reviewRegistration.organisationDetails.registeredBusinessAddress")
                    )

                    groupMemberContent must include(member.organisationDetails.get.organisationName)
                    groupMemberContent must include(member.addressDetails.addressLine1)
                    groupMemberContent must include(member.addressDetails.townOrCity)
                    groupMemberContent must include(member.addressDetails.postCode.get)
                    groupMemberContent must include(
                      countryService.getName(member.addressDetails.countryCode)
                    )
                }
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

  override def exerciseGeneratedRenderingMethods() = {
    val registration = aRegistration()
    page.f(registration, liabilityStartLink)(journeyRequest, messages)
    page.render(registration, liabilityStartLink, journeyRequest, messages)
  }

}
