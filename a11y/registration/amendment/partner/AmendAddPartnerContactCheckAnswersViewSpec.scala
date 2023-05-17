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
import models.genericregistration.Partner
import views.html.amendment.partner.amend_add_partner_contact_check_answers_page

class AmendAddPartnerContactCheckAnswersViewSpec extends BaseViewSpec {

  private val page           = inject[amend_add_partner_contact_check_answers_page]

  private val limitedCompanyPartner = aLimitedCompanyPartner()
  private val soleTraderPartner     = aSoleTraderPartner()
  private val partnershipPartner    = aPartnershipPartner()

  private def createView(partner: Partner): String =
    page(partner)(registrationJourneyRequest, messages).toString()

  "Amend Add Partner Check Answers View" should {

    "pass accessibility checks without error as a limited partner" in {
      val view = createView(limitedCompanyPartner)
      view must passAccessibilityChecks
    }

    "pass accessibility checks without error as a sole trader partner" in {
      val view = createView(soleTraderPartner)
      view must passAccessibilityChecks
    }

    "pass accessibility checks without error as a partnership partner" in {
      val view = createView(partnershipPartner)
      view must passAccessibilityChecks
    }
  }
}
