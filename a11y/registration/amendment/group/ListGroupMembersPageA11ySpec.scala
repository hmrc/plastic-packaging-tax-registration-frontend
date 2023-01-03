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

package registration.amendment.group

import play.api.data.Form
import play.api.i18n.Messages
import support.BaseViewSpec
import config.AppConfig
import forms.group.AddOrganisationForm
import views.html.amendment.group.list_group_members_page
import views.viewmodels.{ListGroupMembersViewModel, ListMember}

class ListGroupMembersPageA11ySpec extends BaseViewSpec {

  val page: list_group_members_page = inject[list_group_members_page]
  val realAppConfig: AppConfig      = inject[AppConfig]

  object fakeViewModel extends ListGroupMembersViewModel(aRegistration()) {

    override def listMembers(implicit messages: Messages): Seq[ListMember] =
      Seq(ListMember("testName"), ListMember("testName 1"))
  }

  val form: Form[Boolean]        = AddOrganisationForm.form()

  private def createView(): String =
    page(form, fakeViewModel)(journeyRequest, messages).toString()

  val view = createView()

  "list_group_members_page" must {

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val errorForm = form.withError("foo", "site.button.tryAgain")
      val view = page(errorForm, fakeViewModel)(journeyRequest, messages).toString()

      view must passAccessibilityChecks
    }
  }
}
