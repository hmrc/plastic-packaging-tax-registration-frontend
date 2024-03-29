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
import models.registration.Registration
import views.html.amendment.partner.manage_partners_page

class ManagePartnersPageSpec extends BaseViewSpec {

  private val page: manage_partners_page = inject[manage_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def createView(registration: Registration): String =
    page(registration)(amendsJourneyRequest, messages).toString()

  val view = createView(partnershipRegistration)

  "Amend Registration Page" when {

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
