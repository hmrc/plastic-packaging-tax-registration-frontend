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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Flash
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.amend_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class AmendRegistrationViewSpec extends UnitViewSpec with Matchers {

  private val page: amend_registration_page = instanceOf[amend_registration_page]
  private val registration                  = aRegistration()
  private val realAppConfig                 = instanceOf[AppConfig]
  private val countryService                = instanceOf[CountryService]

  private def createView(flash: Flash = new Flash(Map.empty)): Html =
    page(registration)(journeyRequest, messages)

  "Amend Registration Page view" should {

    val view: Html = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(
        messages("amendRegistration.organisationDetails.title")
      )
    }

    "display back link to ppt home" in {
      view.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
    }

    "display page heading" in {
      view.select("h1").text() must include(messages("amendRegistration.organisationDetails.title"))
    }

    "display organisation details" in {
      view.select("h2").get(0).text() must include(
        messages("reviewRegistration.organisationDetails.check.label.singleEntity")
      )
      val descriptionList = view.select("dl").get(0).text()
      val expectedOrganisationDetails =
        Seq(registration.organisationDetails.organisationType.map(OrgType.displayName).get,
            registration.organisationDetails.businessName.get,
            registration.organisationDetails.incorporationDetails.map(id => id.companyNumber).get,
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
            ),
            registration.organisationDetails.incorporationDetails.map(id => id.ctutr).get
        ).filter(_.nonEmpty)
      expectedOrganisationDetails.foreach(orgDetail => descriptionList must include(orgDetail))
    }

    "display main contact details" in {
      view.select("h2").get(1).text() must include(messages("primaryContactDetails.check.label"))
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
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration)(journeyRequest, messages)
    page.render(registration, journeyRequest, messages)
  }

}
