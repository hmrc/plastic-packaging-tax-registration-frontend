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

package registration.contact

import play.api.data.Form
import play.api.mvc.Call
import support.BaseViewSpec
import forms.contact.FullName
import views.html.contact.full_name_page

class ContactDetailsFullNameViewA11ySpec extends BaseViewSpec {

  private val page = inject[full_name_page]
  private val updateLink = Call("GET", "/update")

  private def createView(form: Form[FullName] = FullName.form(), isGroup: Boolean = false): String =
    page(form, updateLink, isGroup)(registrationJourneyRequest, messages).toString()

  "Primary Contact Details Full Name View" should {

    val view = createView()

    "pass accessibility checks without error" when {
      "Single organisation" in {
        view must passAccessibilityChecks
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view must passAccessibilityChecks
      }
    }

    "pass accessibility checks with error" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName(""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }

}
