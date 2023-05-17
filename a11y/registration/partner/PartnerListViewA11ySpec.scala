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
import forms.partner.AddPartner
import models.genericregistration.Partner
import models.registration.Registration
import views.html.partner.partner_list_page

class PartnerListViewA11ySpec extends BaseViewSpec {

  private val page = inject[partner_list_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private def getOtherPartners(registration: Registration) =
    registration.organisationDetails.partnershipDetails.map(_.otherPartners).getOrElse(
      throw new IllegalStateException("Other partners absent")
    )

  private def getNominatedPartner(registration: Registration) =
    registration.organisationDetails.partnershipDetails.flatMap(_.nominatedPartner).getOrElse(
      throw new IllegalStateException("Nominated partner absent")
    )

  private def createView(
    otherPartner: Seq[Partner] = getOtherPartners(partnershipRegistration)
  ): String =
    page(AddPartner.form(), getNominatedPartner(partnershipRegistration), otherPartner)(
      registrationJourneyRequest,
      messages
    ).toString()

  "Partner List View" should {

    val view = createView()

    "pass accessibility checks without error" in {

      view must passAccessibilityChecks
    }
  }
}
