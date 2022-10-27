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

package registration.group

import play.api.data.Form
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisationForm
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_list

class OrganisationListViewA11ySpec extends BaseViewSpec {

  private val page = inject[organisation_list]

  private def createView
  (
    form: Form[Boolean] = AddOrganisationForm.form(),
    members: Seq[GroupMember] = Seq(groupMember, groupMember, groupMember)
  ): String =
    page(form, "ACME Inc", members)(journeyRequest, messages).toString()

  "OrganisationList View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = AddOrganisationForm
        .form()
        .bind(Map(AddOrganisationForm.field -> ""))
      val view = createView(form = form)

      view must passAccessibilityChecks
    }
  }
}
