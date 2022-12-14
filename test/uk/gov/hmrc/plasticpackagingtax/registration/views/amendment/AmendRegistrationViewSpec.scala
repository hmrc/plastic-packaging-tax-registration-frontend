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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{routes => amendRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner.{
  routes => partnerAmendRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.RegistrationType.{
  Group,
  Organisation,
  Partnership,
  SoleTrader
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.amend_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest
import uk.gov.hmrc.plasticpackagingtax.registration.views.utils.ViewUtils

object RegistrationType extends Enumeration {
  type RegistrationType = Value

  val Organisation, SoleTrader, Group, Partnership = Value
}

@ViewTest
class AmendRegistrationViewSpec extends UnitViewSpec with Matchers {

  private val page: amend_registration_page = inject[amend_registration_page]
  private val viewUtils: ViewUtils          = inject[ViewUtils]
  private val registration                  = aRegistration()
  private val realAppConfig                 = inject[AppConfig]
  private val countryService                = inject[CountryService]

  private val singleOrganisationRegistration = aRegistration()
  private val soleTraderRegistration         = aRegistration(withSoleTraderDetails(Some(soleTraderDetails)))
  private val groupRegistration              = aRegistration(withGroupDetail(Some(groupDetailsWithMembers)))

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )



  private def createView(registration: Registration): Html =
    page(registration)(journeyRequestWithEnrolledUser, messages)

  "Should display tax start date" in {
    createView(registration).select("dt").text() must include ("Tax start date")

  }
  "Should display value of the date" in {
    createView(registration).select("dd").text() must include ("1 April 2022")
  }

  Seq((Organisation, singleOrganisationRegistration),
      (SoleTrader, soleTraderRegistration),
      (Group, groupRegistration),
      (Partnership, partnershipRegistration)
  ).foreach {
    case (organisationType, registration) =>
      "Amend Registration Page" when {

        s"viewing $organisationType page" should {

          val view: Html = createView(registration)

          "contain timeout dialog function" in {
            containTimeoutDialogFunction(view) mustBe true
          }

          "display sign out link" in {
            displaySignOutLink(view)
          }

          "display title" in {
            view.select("title").text() must include(messages(organisationType match {
              case Organisation => "amend.organisation.title"
              case SoleTrader   => "amend.individual.title"
              case Group        => "amend.group.title"
              case Partnership  => "amend.partnership.title"
            }))
          }

          "display page heading" in {
            view.select("h1").text() must include(messages(organisationType match {
              case Organisation => "amend.organisation.title"
              case SoleTrader   => "amend.individual.title"
              case Group        => "amend.group.title"
              case Partnership  => "amend.partnership.title"
            }))
          }

          "display organisation details" in {
            view.select("h2").get(1).text() must include(messages(organisationType match {
              case Organisation => "amend.organisation.subheading"
              case SoleTrader   => "amend.individual.subheading"
              case Group        => "amend.group.subheading"
              case Partnership  => "amend.partnership.subheading"
            }))
            val descriptionList = view.select("dl").get(0).text()
            val expectedOrganisationDetails =
              Seq(registration.organisationDetails.businessName.get,
                  journeyRequestWithEnrolledUser.pptReference.get,
                  viewUtils.displayLocalDate(registration.dateOfRegistration).get,
                  registration.organisationDetails.businessRegisteredAddress.map(
                    bra => bra.addressLine1
                  ).get,
                  registration.organisationDetails.businessRegisteredAddress.map(
                    bra => bra.addressLine2.getOrElse("")
                  ).get,
                  registration.organisationDetails.businessRegisteredAddress.map(
                    bra => bra.addressLine3.getOrElse("")
                  ).get,
                  registration.organisationDetails.businessRegisteredAddress.map(
                    bra => bra.townOrCity
                  ).get,
                  registration.organisationDetails.businessRegisteredAddress.map(
                    bra => bra.maybePostcode.getOrElse("")
                  ).get,
                  countryService.tryLookupCountryName(
                    registration.organisationDetails.businessRegisteredAddress.map(
                      bra => bra.countryCode
                    ).get
                  )
              ).filter(_.nonEmpty)
            expectedOrganisationDetails.foreach(
              orgDetail => descriptionList must include(orgDetail)
            )
          }

          "display main contact details subheading" in {
            view.select("h2").get(2).text() must include(messages(organisationType match {
              case Organisation => "amend.contactDetails.organisation.subheading"
              case SoleTrader   => "amend.contactDetails.individual.subheading"
              case Group        => "amend.contactDetails.group.subheading"
              case Partnership  => "amend.contactDetails.partnership.subheading"
            }))
          }

          "display main contact details" in {
            val descriptionList = view.select("dl").get(1).text()
            val expectedContactDetails =
              organisationType match {
                case Partnership =>
                  val nominatedPartner = registration.nominatedPartner.get
                  Seq(nominatedPartner.contactDetails.get.name.get,
                      nominatedPartner.contactDetails.get.emailAddress.get,
                      nominatedPartner.contactDetails.get.phoneNumber.get,
                      nominatedPartner.contactDetails.get.address.get.addressLine1,
                      nominatedPartner.contactDetails.get.address.get.addressLine2.getOrElse(""),
                      nominatedPartner.contactDetails.get.address.get.addressLine3.getOrElse(""),
                      nominatedPartner.contactDetails.get.address.get.townOrCity,
                      nominatedPartner.contactDetails.get.address.get.maybePostcode.getOrElse(""),
                      countryService.tryLookupCountryName(
                        registration.primaryContactDetails.address.get.countryCode
                      )
                  ).filter(_.nonEmpty)
                case _ =>
                  Seq(registration.primaryContactDetails.name.get,
                      registration.primaryContactDetails.jobTitle.get,
                      registration.primaryContactDetails.email.get,
                      registration.primaryContactDetails.phoneNumber.get,
                      registration.primaryContactDetails.address.get.addressLine1,
                      registration.primaryContactDetails.address.get.addressLine2.getOrElse(""),
                      registration.primaryContactDetails.address.get.addressLine3.getOrElse(""),
                      registration.primaryContactDetails.address.get.townOrCity,
                      registration.primaryContactDetails.address.get.maybePostcode.getOrElse(""),
                      countryService.tryLookupCountryName(
                        registration.primaryContactDetails.address.get.countryCode
                      )
                  ).filter(_.nonEmpty)
              }

            expectedContactDetails.foreach(orgDetail => descriptionList must include(orgDetail))
          }

          "have change links" in {
            val contactDetailsSection = view.select("dl").get(1)
            val changeLinks           = contactDetailsSection.select("a")
            organisationType match {
              case Partnership =>
                val nominatedPartner = registration.nominatedPartner.get
                changeLinks.get(0) must haveHref(
                  partnerAmendRoutes.AmendPartnerContactDetailsController.contactName(
                    nominatedPartner.id
                  )
                )
                changeLinks.get(1) must haveHref(
                  partnerAmendRoutes.AmendPartnerContactDetailsController.jobTitle(
                    nominatedPartner.id
                  )
                )
                changeLinks.get(2) must haveHref(
                  partnerAmendRoutes.AmendPartnerContactDetailsController.emailAddress(
                    nominatedPartner.id
                  )
                )
                changeLinks.get(3) must haveHref(
                  partnerAmendRoutes.AmendPartnerContactDetailsController.phoneNumber(
                    nominatedPartner.id
                  )
                )
                changeLinks.get(4) must haveHref(
                  partnerAmendRoutes.AmendPartnerContactDetailsController.address(
                    nominatedPartner.id
                  )
                )
              case _ =>
                changeLinks.get(0) must haveHref(
                  amendRoutes.AmendContactDetailsController.contactName()
                )
                changeLinks.get(1) must haveHref(
                  amendRoutes.AmendContactDetailsController.jobTitle()
                )
                changeLinks.get(2) must haveHref(amendRoutes.AmendEmailAddressController.email())
                changeLinks.get(3) must haveHref(
                  amendRoutes.AmendContactDetailsController.phoneNumber()
                )
                changeLinks.get(4) must haveHref(
                  amendRoutes.AmendContactDetailsController.address()
                )
            }
          }

          "display the back to account page button" in {
            val element = view.getElementById("return-to-account-page")
            element must haveHref(realAppConfig.pptAccountUrl)
          }
        }
      }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration)(journeyRequest, messages)
    page.render(registration, journeyRequest, messages)
  }

}
