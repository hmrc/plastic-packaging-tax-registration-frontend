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

package views.amendment.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import controllers.amendment.partner.routes
import forms.contact.Address
import services.CountryService
import views.html.amendment.partner.amend_partner_contact_check_answers_page

class AmendPartnerContactCheckAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page           = inject[amend_partner_contact_check_answers_page]
  private val countryService = inject[CountryService]

  private val partner = aLimitedCompanyPartner

  "Amend Partner Contact Check Answers View" should {

    val view: Document = page(partner)(registrationJourneyRequest, messages)

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("partner.check.title"))
    }

    "display expected group member information" in {
      val rowDetails = view.select("dl div")

      def extractAddress(address: Address) =
        Seq(
          address.addressLine1,
          address.addressLine2.getOrElse(""),
          address.addressLine3.getOrElse(""),
          address.townOrCity,
          address.maybePostcode.getOrElse(""),
          countryService.tryLookupCountryName(address.countryCode)
        ).filter(_.nonEmpty).mkString(" ")

      val expectedContent = Seq(
        (messages("partner.check.orgType"), messages(s"organisationDetails.type.${partner.partnerType.toString}"), None),
        (messages("partner.check.orgName"), partner.name, None),
        (messages("partner.check.contact.name"), partner.contactDetails.get.name.get, Some(routes.AmendPartnerContactDetailsController.contactName(partner.id).url)),
        (messages("partner.check.contact.email"), partner.contactDetails.get.emailAddress.get, Some(routes.AmendPartnerContactDetailsController.emailAddress(partner.id).url)),
        (messages("partner.check.contact.phone"), partner.contactDetails.get.phoneNumber.get, Some(routes.AmendPartnerContactDetailsController.phoneNumber(partner.id).url)),
        (
          messages("partner.check.contact.address"),
          extractAddress(partner.contactDetails.get.address.get),
          Some(routes.AmendPartnerContactDetailsController.address(partner.id).url)
        )
      )

      expectedContent.zipWithIndex.foreach {
        case (expectedContent, idx) =>
          rowDetails.get(idx).text() must include(expectedContent._1)
          rowDetails.get(idx).text() must include(expectedContent._2)
          if (expectedContent._3.isDefined)
            rowDetails.get(idx).select("a").get(0) must haveHref(expectedContent._3.get)
      }
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(partner)(registrationJourneyRequest, messages)
    page.render(partner, registrationJourneyRequest, messages)
  }

}
