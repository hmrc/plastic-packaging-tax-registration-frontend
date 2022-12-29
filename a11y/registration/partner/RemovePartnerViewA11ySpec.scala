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

package registration.partner

import play.api.data.Form
import support.BaseViewSpec
import forms.partner.RemovePartner
import views.html.partner.remove_partner_page
import java.util.UUID


class RemovePartnerViewA11ySpec extends BaseViewSpec{

  private val page = inject[remove_partner_page]

  private val partnerName = "Plastic Packaging Partner"
  private val partnerId   = UUID.randomUUID.toString

  private def createView(form: Form[RemovePartner] = RemovePartner.form()): String =
    page(form, partnerName, partnerId)(journeyRequest, messages).toString()

  "Remove Partner View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = RemovePartner
        .form()
        .bind(Map("value" -> ""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
