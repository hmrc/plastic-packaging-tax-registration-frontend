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
import forms.contact.{JobTitle, PhoneNumber}
import views.html.contact.{job_title_page, phone_number_page}

class ContactDetailsPhoneNumberViewA11ySpec extends BaseViewSpec {

  private val page = inject[phone_number_page]
  private val updateLink = Call("GET", "/update")

  private def createView(form: Form[PhoneNumber] = PhoneNumber.form(), isGroup: Boolean = false): String =
    page(form, updateLink, isGroup)(journeyRequest, messages).toString()

  "Primary Contact Details Phone Number View" should {

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
      val form = PhoneNumber
        .form()
        .fillAndValidate(PhoneNumber(""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }

}
