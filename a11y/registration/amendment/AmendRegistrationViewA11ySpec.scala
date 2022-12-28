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

package registration.amendment

import support.BaseViewSpec
import models.registration.Registration
import views.amendment.RegistrationType.{Group, Organisation, Partnership, SoleTrader}
import views.html.amendment.amend_registration_page
object RegistrationType extends Enumeration {
  type RegistrationType = Value

  val Organisation, SoleTrader, Group, Partnership = Value
}


class AmendRegistrationViewA11ySpec extends BaseViewSpec {

  private val page: amend_registration_page = inject[amend_registration_page]

  private val singleOrganisationRegistration = aRegistration()
  private val soleTraderRegistration         = aRegistration(withSoleTraderDetails(Some(soleTraderDetails)))
  private val groupRegistration              = aRegistration(withGroupDetail(Some(groupDetailsWithMembers)))
  private val partnershipRegistration        = aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))

  private def createView(registration: Registration): String =
    page(registration)(journeyRequestWithEnrolledUser, messages).toString()

  Seq((Organisation, singleOrganisationRegistration),
    (SoleTrader, soleTraderRegistration),
    (Group, groupRegistration),
    (Partnership, partnershipRegistration)
  ).foreach {
    case (organisationType, registration) =>
    "Amend Registration Page" when {

      s"viewing $organisationType page" should {

        val view = createView(registration)

        "pass accessibility checks without error" in {
          view must passAccessibilityChecks
        }
      }
    }
  }
}
