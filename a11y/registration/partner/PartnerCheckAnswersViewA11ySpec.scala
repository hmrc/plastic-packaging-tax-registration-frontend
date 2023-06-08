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

import support.BaseViewSpec
import models.genericregistration.Partner
import views.html.partner.partner_check_answers_page

class PartnerCheckAnswersViewA11ySpec extends BaseViewSpec {

  private val page = inject[partner_check_answers_page]

  private def render(partner: Partner): String =
  page(partner)(registrationJourneyRequest, messages).toString()

  "PartnerCheckAnswersViewA11ySpec" should {
    "pass accessibility checks" when {
      "a limited Company partner" in {
        render(aLimitedCompanyPartner) must passAccessibilityChecks
      }

      "a Sole trader partner" in {
        render(aSoleTraderPartner) must passAccessibilityChecks
      }

      "a Partnership Partner" in {
        render(aPartnershipPartner) must passAccessibilityChecks
      }
    }
  }
}
