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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.GENERAL_PARTNERSHIP
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{routes => amendRoutes}
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

  private val page: amend_registration_page = instanceOf[amend_registration_page]
  private val viewUtils: ViewUtils          = instanceOf[ViewUtils]
  private val registration                  = aRegistration()
  private val realAppConfig                 = instanceOf[AppConfig]
  private val countryService                = instanceOf[CountryService]

  private val singleOrganisationRegistration = aRegistration()
  private val soleTraderRegistration         = aRegistration(withSoleTraderDetails(Some(soleTraderDetails)))
  private val groupRegistration              = aRegistration(withGroupDetail(Some(groupDetailsWithMembers)))

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(partnershipDetailsWithBusinessAddress(GENERAL_PARTNERSHIP)))
  )

  private def createView(registration: Registration): Html =
    page(registration)(journeyRequestWithEnrolledUser, messages)

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

          "display back link to ppt home" in {
            view.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
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
            view.select("h2").get(0).text() must include(messages(organisationType match {
              case Organisation => "amend.organisation.subheading"
              case SoleTrader   => "amend.individual.subheading"
              case Group        => "amend.group.subheading"
              case Partnership  => "amend.partnership.subheading"
            }))
            val descriptionList = view.select("dl").get(0).text()
            val expectedOrganisationDetails =
              Seq(registration.organisationDetails.businessName.get,
                  journeyRequestWithEnrolledUser.pptReference.get,
                  viewUtils.displayDate(registration.liabilityDetails.startDate).get,
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
                    bra => bra.postCode.getOrElse("")
                  ).get,
                  countryService.getName(
                    registration.organisationDetails.businessRegisteredAddress.map(
                      bra => bra.countryCode
                    ).get
                  )
              ).filter(_.nonEmpty)
            expectedOrganisationDetails.foreach(
              orgDetail => descriptionList must include(orgDetail)
            )
          }

          "display main contact details" in {
            view.select("h2").get(1).text() must include(messages(organisationType match {
              case Organisation => "amend.contactDetails.organisation.subheading"
              case SoleTrader   => "amend.contactDetails.individual.subheading"
              case Group        => "amend.contactDetails.group.subheading"
              case Partnership  => "amend.contactDetails.partnership.subheading"
            }))
            val descriptionList = view.select("dl").get(1).text()
            val expectedContactDetails = Seq(registration.primaryContactDetails.name.get,
                                             registration.primaryContactDetails.jobTitle.get,
                                             registration.primaryContactDetails.email.get,
                                             registration.primaryContactDetails.phoneNumber.get,
                                             registration.primaryContactDetails.address.map(
                                               a => a.addressLine1
                                             ).get,
                                             registration.primaryContactDetails.address.map(
                                               a => a.addressLine2.getOrElse("")
                                             ).get,
                                             registration.primaryContactDetails.address.map(
                                               a => a.addressLine3.getOrElse("")
                                             ).get,
                                             registration.primaryContactDetails.address.map(
                                               a => a.townOrCity
                                             ).get,
                                             registration.primaryContactDetails.address.map(
                                               a => a.postCode.getOrElse("")
                                             ).get,
                                             countryService.getName(
                                               registration.primaryContactDetails.address.map(
                                                 a => a.countryCode
                                               ).get
                                             )
            ).filter(_.nonEmpty)
            expectedContactDetails.foreach(orgDetail => descriptionList must include(orgDetail))
          }

          "have change links" in {
            val contactDetailsSection = view.select("dl").get(1)
            val changeLinks           = contactDetailsSection.select("a")
            changeLinks.get(0) must haveHref(
              amendRoutes.AmendContactDetailsController.contactName()
            )
            changeLinks.get(1) must haveHref(amendRoutes.AmendContactDetailsController.jobTitle())
            changeLinks.get(2) must haveHref(amendRoutes.AmendEmailAddressController.email())
            changeLinks.get(3) must haveHref(
              amendRoutes.AmendContactDetailsController.phoneNumber()
            )
            changeLinks.get(4) must haveHref(amendRoutes.AmendContactDetailsController.address())
          }
        }
      }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration)(journeyRequest, messages)
    page.render(registration, journeyRequest, messages)
  }

}
