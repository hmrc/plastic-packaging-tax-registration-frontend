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

package uk.gov.hmrc.plasticpackagingtax.registration.views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_check_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnerCheckAnswersViewSpec extends UnitViewSpec with Matchers {

  private val page           = inject[partner_check_answers_page]
  private val countryService = inject[CountryService]

  private val limitedCompanyPartner = aLimitedCompanyPartner()
  private val soleTraderPartner     = aSoleTraderPartner()
  private val partnershipPartner    = aPartnershipPartner()

  private def createView(partner: Partner): Document =
    page(partner)(journeyRequest, messages)

  "Partner Check Answers View" should {

    val view = createView(limitedCompanyPartner)

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(
        commonRoutes.TaskListController.displayPage().url
      )
    }

    "display title" in {
      view.select("title").text() must include(messages("partner.check.title"))
    }

    def extractAddress(address: Address): String =
      Seq(address.addressLine1,
          address.addressLine2.getOrElse(""),
          address.addressLine3.getOrElse(""),
          address.townOrCity,
          address.postCode.getOrElse(""),
          countryService.getName(address.countryCode)
      ).filter(_.nonEmpty).mkString(" ")

    val testData = Seq(
      ("Limited Company Partner",
       limitedCompanyPartner,
       Seq(
         (messages("partner.check.orgType"),
          PartnerTypeEnum.displayName(limitedCompanyPartner.partnerType),
          None: Option[Call]
         ),
         (messages("partner.check.companyNumber"),
          limitedCompanyPartner.incorporationDetails.get.companyNumber,
          None: Option[Call]
         ),
         (messages("partner.check.orgName"),
          limitedCompanyPartner.incorporationDetails.get.companyName,
          None: Option[Call]
         ),
         (messages("partner.check.utr"),
          limitedCompanyPartner.incorporationDetails.get.ctutr,
          None: Option[Call]
         ),
         (messages("partner.check.contact.name"),
          limitedCompanyPartner.contactDetails.get.name.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.jobTitle"),
          limitedCompanyPartner.contactDetails.get.jobTitle.getOrElse(""),
          None: Option[Call]
         ),
         (messages("partner.check.contact.email"),
          limitedCompanyPartner.contactDetails.get.emailAddress.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.phone"),
          limitedCompanyPartner.contactDetails.get.phoneNumber.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.address"),
          extractAddress(limitedCompanyPartner.contactDetails.get.address.get),
          None: Option[Call]
         )
       )
      ),
      ("Sole Trader Partner",
       soleTraderPartner,
       Seq(
         (messages("partner.check.orgType"),
          PartnerTypeEnum.displayName(soleTraderPartner.partnerType),
          None: Option[Call]
         ),
         (messages("partner.check.name"),
          soleTraderPartner.soleTraderDetails.get.name,
          None: Option[Call]
         ),
         (messages("partner.check.dob"),
          soleTraderPartner.soleTraderDetails.get.dateOfBirth.get,
          None: Option[Call]
         ),
         (messages("partner.check.nino"),
          soleTraderPartner.soleTraderDetails.get.ninoOrTrn,
          None: Option[Call]
         ),
         (messages("partner.check.utr"),
          soleTraderPartner.soleTraderDetails.get.sautr.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.name"),
          soleTraderPartner.contactDetails.get.name.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.email"),
          soleTraderPartner.contactDetails.get.emailAddress.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.phone"),
          soleTraderPartner.contactDetails.get.phoneNumber.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.address"),
          extractAddress(soleTraderPartner.contactDetails.get.address.get),
          None: Option[Call]
         )
       )
      ),
      ("Partnership Partner",
       partnershipPartner,
       Seq(
         (messages("partner.check.orgType"),
          PartnerTypeEnum.displayName(partnershipPartner.partnerType),
          None: Option[Call]
         ),
         (messages("partner.check.orgName"), partnershipPartner.name, None: Option[Call]),
         (messages("partner.check.sautr"),
          partnershipPartner.partnerPartnershipDetails.get.partnershipBusinessDetails.get.sautr,
          None: Option[Call]
         ),
         (messages("partner.check.contact.name"),
          partnershipPartner.contactDetails.get.name.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.email"),
          partnershipPartner.contactDetails.get.emailAddress.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.phone"),
          partnershipPartner.contactDetails.get.phoneNumber.get,
          None: Option[Call]
         ),
         (messages("partner.check.contact.address"),
          extractAddress(partnershipPartner.contactDetails.get.address.get),
          None: Option[Call]
         )
       )
      )
    )

    testData.foreach(
      testData =>
        "display expected partner information" when {
          s"rendering ${testData._1}" in {
            val view       = createView(testData._2)
            val rowDetails = view.select("dl div")

            testData._3.zipWithIndex.foreach {
              case (expectedContent, idx) =>
                rowDetails.get(idx).text() must include(expectedContent._1)
                rowDetails.get(idx).text() must include(expectedContent._2)
                if (expectedContent._3.isDefined)
                  rowDetails.get(idx).select("a").get(0) must haveHref(expectedContent._3.get)
            }
          }
        }
    )

    "display 'Confirm and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Confirm and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(limitedCompanyPartner)(journeyRequest, messages)
    page.render(limitedCompanyPartner, journeyRequest, messages)
  }

}
