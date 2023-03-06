/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import controllers.amendment.partner.routes
import forms.group.AddOrganisationForm
import forms.group.AddOrganisationForm.form
import forms.partner.AddPartner.{NO, YES}
import models.registration.Registration
import views.html.amendment.partner.list_partners_page

class ListPartnersPageSpec extends UnitViewSpec with Matchers {

  private val page: list_partners_page = inject[list_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def createView(registration: Registration): Html =
    page(AddOrganisationForm.form(), registration)(journeyRequestWithEnrolledUser, messages)

  val view: Html = createView(partnershipRegistration)

  "Amend Registration Page" when {

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(
        messages("amend.partner.listPartners.title", 1 + partnershipRegistration.otherPartners.size)
      )
    }

    "display page heading" in {
      view.select("h1").text() must include(
        messages("amend.partner.listPartners.title", 1 + partnershipRegistration.otherPartners.size)
      )
    }

    "display nominated partner" in {
      val dataList = view.select("main").select("ul")
      dataList.select("li").get(0).text() must include(messages("amend.partner.manage.nominated"))
      dataList.select("li").get(0).text() must include(
        partnershipRegistration.nominatedPartner.map(_.name).get
      )
    }

    "display others partners with change and remove links" in {
      val dataList = view.select("main").select("ul")
      partnershipRegistration.otherPartners.zipWithIndex.foreach {
        case (otherPartner, idx) =>
          val row = dataList.select("li").get(idx + 1)
          row.text() must include(otherPartner.name)
          val links = row.select("a")
          links.get(0) must haveHref(
            routes.PartnerContactDetailsCheckAnswersController.displayPage(otherPartner.id)
          )
          links.get(1) must haveHref(
            routes.ConfirmRemovePartnerController.displayPage(otherPartner.id)
          )
      }
    }

    "display add another partner form" in {
      view.select("form").attr("method") mustBe "POST"
      view.select("form").attr("action") mustBe routes.PartnersListController.submit().url
      view.select("legend").text() mustBe messages("amend.partner.listPartners.question")
      view.getElementById("addOrganisation").attr("value") mustBe YES
      view.getElementById("addOrganisation-2").attr("value") mustBe NO

      withClue("should have the save button") {
        view.select("form > div.govuk-button-group").size() mustBe 1
      }
    }

    "include the error summary" when {
      "the form has errors" in {
        val errorForm = form.withError("foo", "site.button.tryAgain")
        val view =
          page(errorForm, partnershipRegistration)(journeyRequestWithEnrolledUser, messages)

        view.select(".govuk-error-summary__title").size() mustBe 1
      }
    }

    "not display the remove partner button" when {
      "there are two partner only" in {
        val regWithTwoPartner = aRegistration(
          withPartnershipDetails(
            Some(
              generalPartnershipDetailsWithPartners.copy(partners =
                Seq(aPartnershipPartner, aLimitedCompanyPartner)
              )
            )
          )
        )

        val newView = createView(regWithTwoPartner)

        newView.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 0
      }

      "there is one partner only" in {
        val regWithOnePartner = aRegistration(
          withPartnershipDetails(
            Some(generalPartnershipDetailsWithPartners.copy(partners = Seq(aPartnershipPartner)))
          )
        )

        val newView = createView(regWithOnePartner)

        newView.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 0
      }
    }

    "display the remove partner button" when {
      "there are more than 2 partner" in {
        view.getElementsByClass("hmrc-add-to-a-list__remove").size() mustBe 3
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(AddOrganisationForm.form(), partnershipRegistration)(journeyRequest, messages)
    page.render(AddOrganisationForm.form(), partnershipRegistration, journeyRequest, messages)
  }

}
