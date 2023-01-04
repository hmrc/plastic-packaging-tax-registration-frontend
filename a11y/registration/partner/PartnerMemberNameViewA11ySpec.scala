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

package registration.partner

import play.api.data.Form
import play.api.mvc.Call
import support.BaseViewSpec
import forms.group.MemberName
import views.html.partner.partner_member_name_page

class PartnerMemberNameViewA11ySpec extends BaseViewSpec {

  private val page             = inject[partner_member_name_page]
  private val updateLink       = Call("GET", "/update")
  private val organisationName = "Organisation"
  private val nominated        = true
  private val notNominated     = false

  private def createViewNom(form: Form[MemberName] = MemberName.form()): String =
    page(form, organisationName, nominated, updateLink)(journeyRequest, messages).toString()

  private def createViewOther(form: Form[MemberName] = MemberName.form()): String =
    page(form, organisationName, notNominated, updateLink)(journeyRequest, messages).toString()

  "Member name viewNom" should {

    val viewNom = createViewNom()
    val viewOther = createViewOther()

    "pass accessibility checks without error" when {
      "Nominated partner" in {
        viewNom must passAccessibilityChecks
      }
      "Other partner" in {
        viewOther must passAccessibilityChecks
      }
    }

    "pass accessibility checks with error" when {
      "Nominated partner" in {
        val form = MemberName
          .form()
          .fillAndValidate(MemberName("", "last"))
        val viewNom = createViewNom(form)

        viewNom must passAccessibilityChecks
      }
      "Other partner" in {
        val form = MemberName
          .form()
          .fillAndValidate(MemberName("first", ""))
        val viewOther = createViewOther(form)

        viewOther must passAccessibilityChecks
      }
    }
  }
}
