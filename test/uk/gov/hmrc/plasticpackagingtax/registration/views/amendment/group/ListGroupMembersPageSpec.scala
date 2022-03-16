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
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.Injecting
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.list_group_members_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.{
  ListGroupMembersViewModel,
  ListMember
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisationForm
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisationForm._

class ListGroupMembersPageSpec extends UnitViewSpec with Matchers with Injecting {

  val view: list_group_members_page = inject[list_group_members_page]
  val realAppConfig: AppConfig      = inject[AppConfig]

  object fakeViewModel extends ListGroupMembersViewModel(aRegistration()) {
    override val groupMemberCount: Int = 1

    override def listMembers(implicit messages: Messages): Seq[ListMember] =
      Seq(ListMember("testName"))

  }

  val form: Form[Boolean]        = AddOrganisationForm.form()
  val sut: HtmlFormat.Appendable = view(form, fakeViewModel)(journeyRequest, messages)

  "manage_group_members_page" must {
    "have the back button" in {
      sut.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
    }

    "contain title" in {
      sut.select("title").text() must include(messages("amend.group.listMembers.title", 1))
    }

    "contain heading" in {
      sut.select("h1").text() mustBe messages("amend.group.listMembers.title", 1)
    }

    "list the members" in {
      sut.select("ul.hmrc-add-to-a-list").size() mustBe 1
    }

    "display add member form" in {
      sut.select("form").attr("method") mustBe "POST"
      sut.select("form").attr("action") mustBe routes.GroupMembersListController.onSubmit().url
      sut.select("legend").text() mustBe messages("addOrganisation.add.heading")
      sut.getElementById("addOrganisation").attr("value") mustBe YES
      sut.getElementById("addOrganisation-2").attr("value") mustBe NO

      withClue("should have the save button") {
        sut.select("form > div.govuk-button-group").size() mustBe 1
      }
    }

    "include the error summary" when {
      "the form has errors" in {
        val errorForm = form.withError("foo", "site.button.tryAgain")
        val sut       = view(errorForm, fakeViewModel)(journeyRequest, messages)

        sut.select("#error-summary-title").size() mustBe 1
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    view.f(form, fakeViewModel)(journeyRequest, messages)
    view.render(form, fakeViewModel, journeyRequest, messages)
  }

}
