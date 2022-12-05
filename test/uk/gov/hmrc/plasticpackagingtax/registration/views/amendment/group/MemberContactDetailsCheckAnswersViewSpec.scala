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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.member_contact_check_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class MemberContactDetailsCheckAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page           = inject[member_contact_check_answers_page]
  private val countryService = inject[CountryService]

  private def createView(): Document =
    page(groupMember)(journeyRequest, messages)

  "Amendment Contact Details Check Answers View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("contactDetails.member.check.title"))
    }

    "display expected group member information" in {
      val rowDetails = view.select("dl div")

      def extractAddress(address: Address) =
        Seq(address.addressLine1,
            address.addressLine2.getOrElse(""),
            address.addressLine3.getOrElse(""),
            address.townOrCity,
            address.maybePostcode.getOrElse(""),
            countryService.tryLookupCountryName(address.countryCode)
        ).filter(_.nonEmpty).mkString(" ")

      val expectedContent = Seq(
        (messages("contactDetails.member.check.orgType"),
         messages(
           s"organisationDetails.type.${OrgType.withNameOpt(groupMember.businessType.get).get}"
         ),
         None
        ),
        (messages("contactDetails.member.check.companyNumber"),
         groupMember.customerIdentification1,
         None
        ),
        (messages("contactDetails.member.check.orgName"), groupMember.businessName, None),
        (messages("contactDetails.member.check.utr"),
         groupMember.customerIdentification2.get,
         None
        ),
        (messages("contactDetails.member.check.contact.name"),
         groupMember.contactDetails.get.groupMemberName,
         Some(routes.AmendMemberContactDetailsController.contactName(groupMember.id).url)
        ),
        (messages("contactDetails.member.check.contact.email"),
         groupMember.contactDetails.get.email.get,
         Some(routes.AmendMemberContactDetailsController.email(groupMember.id).url)
        ),
        (messages("contactDetails.member.check.contact.phone"),
         groupMember.contactDetails.get.phoneNumber.get,
         Some(routes.AmendMemberContactDetailsController.phoneNumber(groupMember.id).url)
        ),
        (messages("contactDetails.member.check.contact.address"),
         extractAddress(groupMember.contactDetails.get.address.get),
         Some(routes.AmendMemberContactDetailsController.address(groupMember.id).url)
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
    page.f(groupMember)(journeyRequest, messages)
    page.render(groupMember, journeyRequest, messages)
  }

}
