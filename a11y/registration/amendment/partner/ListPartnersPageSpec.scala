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
import forms.group.AddOrganisationForm
import forms.group.AddOrganisationForm.form
import models.registration.Registration
import views.html.amendment.partner.list_partners_page

class ListPartnersPageSpec extends BaseViewSpec {

  private val page: list_partners_page = inject[list_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def createView(registration: Registration): String =
    page(AddOrganisationForm.form(), registration)(amendsJourneyRequest, messages).toString()

  val view = createView(partnershipRegistration)

  "Amend List Partnership Page" when {

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val errorForm = form.withError("foo", "site.button.tryAgain")
      val view = page(errorForm, partnershipRegistration)(amendsJourneyRequest, messages).toString()

      view must passAccessibilityChecks
    }
  }
}
