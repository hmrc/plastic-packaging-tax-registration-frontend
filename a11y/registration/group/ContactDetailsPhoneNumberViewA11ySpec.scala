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
import play.api.mvc.Call
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_phone_number_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsPhoneNumberViewA11ySpec extends BaseViewSpec {

  private val page = inject[member_phone_number_page]

  private val updateLink = Call("GET", "/update")

  private val contactName = Some("Test")

  private def createView(form: Form[PhoneNumber] = PhoneNumber.form()): String =
    page(form, contactName, updateLink)(journeyRequest, messages).toString()

  "Phone Number View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
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
