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

package registration.group

import play.api.data.Form
import play.api.mvc.Call
import support.BaseViewSpec
import forms.contact.EmailAddress
import views.html.group.member_email_address_page

class ContactDetailsEmailAddressViewA11ySpec extends BaseViewSpec {

  private val page = inject[member_email_address_page]

  private val updateLink = Call("GET", "/update")

  private val contactName = Some("Test")

  private def createView(form: Form[EmailAddress] = EmailAddress.form()): String =
    page(form, contactName, updateLink)(journeyRequest, messages).toString()

  "Email Address View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress(""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
