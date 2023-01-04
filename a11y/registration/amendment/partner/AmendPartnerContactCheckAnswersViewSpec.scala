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

package registration.amendment.partner

import support.BaseViewSpec
import views.html.amendment.partner.amend_partner_contact_check_answers_page

class AmendPartnerContactCheckAnswersViewSpec extends BaseViewSpec {

  private val page = inject[amend_partner_contact_check_answers_page]

  private val partner = aLimitedCompanyPartner()

  "Amend Partner Contact Check Answers View" should {

    val view: String = page(partner)(journeyRequest, messages).toString()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
