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

package uk.gov.hmrc.plasticpackagingtax.registration.views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisationForm
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_list

class OrganisationListSpec extends UnitViewSpec with Matchers {

  private val page = inject[organisation_list]

  private val singleMember = Seq(groupMember)

  private def createView(): Document =
    page(AddOrganisationForm.form(), "ACME Inc", singleMember)(journeyRequest, messages)

  "OrganisationList View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.TaskListController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(
        messages("group.organisationList.title.multiple", "2")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("group.organisationList.sectionHeader")
      )
    }

    "display visually hidden labels" in {

      view.getElementsByClass("govuk-visually-hidden").get(1).text() must include(
        messages("site.back.hiddenText")
      )
    }

    "display input boxes" in {

      view must containElementWithID("addOrganisation")
      view must containElementWithID("addOrganisation-2")
    }

    "display change link for nominated org" in {
      view.getElementById("list-members-ul").children().select("a").get(0) must haveHref(orgRoutes.CheckAnswersController.displayPage())
    }

    "display change links for group members" in {
      view.getElementById("list-members-ul").children().select("a").get(1) must haveHref(
        groupRoutes.ContactDetailsCheckAnswersController.displayPage(groupMember.id)
      )
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display entry for nominated organisation" in {
      view.getElementsByClass("hmrc-add-to-a-list__identifier").get(
        0
      ).text() mustBe "ACME Inc Nominated organisation"
    }

    "display entry for added member" in {
      view.getElementsByClass("hmrc-add-to-a-list__identifier").get(
        1
      ).text() mustBe groupMember.organisationDetails.map(_.organisationName).get
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(AddOrganisationForm.form(), "Org", singleMember)(journeyRequest, messages)
    page.render(AddOrganisationForm.form(), "Org", singleMember, journeyRequest, messages)
  }

}
