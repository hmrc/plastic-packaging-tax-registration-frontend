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
import forms.group.RemoveMember
import views.html.group.remove_group_member_page
import java.util.UUID


class RemoveGroupMemberViewA11ySpec extends BaseViewSpec {

  private val page = inject[remove_group_member_page]

  private val memberName = "Plastic Packaging Subsidiary"
  private val memberId   = UUID.randomUUID.toString

  private def createView(form: Form[RemoveMember] = RemoveMember.form()): String =
    page(form, memberName, memberId)(journeyRequest, messages).toString()

  "Remove Group Member View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = RemoveMember
        .form()
        .bind(Map("value" -> ""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
