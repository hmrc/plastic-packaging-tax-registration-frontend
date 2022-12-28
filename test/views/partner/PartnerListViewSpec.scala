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

package views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import forms.partner.AddPartner
import models.genericregistration.Partner
import models.registration.Registration
import views.html.partner.partner_list_page

class PartnerListViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[partner_list_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def createView(
    otherPartner: Seq[Partner] = getOtherPartners(partnershipRegistration)
  ): Document =
    page(AddPartner.form(), getNominatedPartner(partnershipRegistration), otherPartner)(
      journeyRequest,
      messages
    )

  "Partner List View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(
        messages("partnership.partnerList.title", partnerCount(partnershipRegistration))
      )
    }

    "display page heading" in {
      view.select("h1").text() must include(
        messages("partnership.partnerList.title", partnerCount(partnershipRegistration))
      )
    }

    "display nominated partner label" in {
      view.getElementById("list-members-ul").children().get(0).text() must include(
        messages("partnership.partnerList.nominatedPartner")
      )
    }

    "display nominated partner name" in {
      view.getElementById("list-members-ul").children().get(0).text() must include(
        getNominatedPartner(partnershipRegistration).name
      )
    }

    "display other partners names" in {
      val otherPartnerItems = view.getElementById("list-members-ul").children()
      getOtherPartners(partnershipRegistration).zipWithIndex.foreach {
        case (partner, idx) => otherPartnerItems.get(idx + 1).text() must include(partner.name)
      }
    }

    "not display the remove partner button" when {
      "there are two partner only" in {
        val newView = createView(Seq(aPartnershipPartner()))

        newView.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 0
      }

      "there is one partner only" in {
        val newView = createView(Seq.empty)

        newView.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 0
      }
    }

    "display the remove partner button" when {
      "there are more than 2 partner" in {

        view.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 3
      }
    }
    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(AddPartner.form(),
           getNominatedPartner(partnershipRegistration),
           getOtherPartners(partnershipRegistration)
    )(journeyRequest, messages)
    page.render(AddPartner.form(),
                getNominatedPartner(partnershipRegistration),
                getOtherPartners(partnershipRegistration),
                journeyRequest,
                messages
    )
  }

  private def getOtherPartners(registration: Registration) =
    registration.organisationDetails.partnershipDetails.map(_.otherPartners).getOrElse(
      throw new IllegalStateException("Other partners absent")
    )

  private def getNominatedPartner(registration: Registration) =
    registration.organisationDetails.partnershipDetails.flatMap(_.nominatedPartner).getOrElse(
      throw new IllegalStateException("Nominated partner absent")
    )

  private def partnerCount(registration: Registration) = 1 + getOtherPartners(registration).size
}
