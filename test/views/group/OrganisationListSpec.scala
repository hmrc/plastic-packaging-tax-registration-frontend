/*
 * Copyright 2025 HM Revenue & Customs
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

package views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import controllers.group.{routes => groupRoutes}
import controllers.organisation.{routes => orgRoutes}
import forms.group.AddOrganisationForm
import models.registration.group.GroupMember
import views.html.group.organisation_list

class OrganisationListSpec extends UnitViewSpec with Matchers {

  private val page = inject[organisation_list]

  private val singleMember = Seq(groupMember)

  private def createView(
    form: Form[Boolean] = AddOrganisationForm.form(),
    members: Seq[GroupMember] = Seq(groupMember)
  ): Document =
    page(form, "ACME Inc", members)(registrationJourneyRequest, messages)

  "OrganisationList View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("group.organisationList.title.multiple", "2"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(messages("group.organisationList.sectionHeader"))
    }

    "display input boxes" in {

      view must containElementWithID("addOrganisation")
      view must containElementWithID("addOrganisation-2")
    }

    "display change link for nominated org" in {
      view.getElementById("list-members-ul").children().select("a").get(0) must haveHref(
        orgRoutes.CheckAnswersController.displayPage()
      )
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
      view.getElementsByClass("hmrc-add-to-a-list__identifier").get(0).text() mustBe "ACME Inc Nominated organisation"
    }

    "display entry for added member" in {
      view.getElementsByClass("hmrc-add-to-a-list__identifier").get(
        1
      ).text() mustBe groupMember.organisationDetails.map(_.organisationName).get
    }

    "have no remove button" when {
      "group contains 2 members (nominate member and a member)" in {
        view.getElementsByClass("hmrc-add-to-a-list__remove")
          .select("a")
          .size() mustBe 0
      }

      "group contains less only the nominate member" in {
        val newView = createView(members = Seq.empty)
        newView.getElementsByClass("hmrc-add-to-a-list__remove")
          .select("a")
          .size() mustBe 0
      }
    }

    "have the remove button enabled" when {
      "group contains more than 2 members" in {
        val newView = createView(members = Seq(groupMember, groupMember, groupMember))
        newView.getElementsByClass("hmrc-add-to-a-list__remove")
          .select("a")
          .size() mustBe 3
      }
    }

    "display error if question not answered" in {
      val form = AddOrganisationForm
        .form()
        .bind(Map(AddOrganisationForm.field -> ""))
      val view = createView(form = form)

      view must haveGovukGlobalErrorSummary
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(AddOrganisationForm.form(), "Org", singleMember)(registrationJourneyRequest, messages)
    page.render(AddOrganisationForm.form(), "Org", singleMember, registrationJourneyRequest, messages)
  }

}
