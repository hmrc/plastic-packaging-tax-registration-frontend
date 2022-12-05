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

package registration.organisation

import play.api.data.Form
import play.api.mvc.{AnyContent, Call}
import play.api.test.Helpers.POST
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType.FormMode.PartnershipType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.UK_COMPANY
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partner_type

class PartnerTypeA11ySpec extends BaseViewSpec {

  private val page = inject[partner_type]

  val partnershipRegistrationWithInflightPartner = aRegistration(
    withPartnershipDetails(
      Some(generalPartnershipDetailsWithPartners))
  )

  val customRequest: JourneyRequest[AnyContent] =
    JourneyRequest(authenticatedRequest = authenticatedRequest, registration = partnershipRegistrationWithInflightPartner, pptReference = None)


  private def render(form: Form[PartnerType] = PartnerType.form(PartnershipType), partnerId: Option[String]) = page(
    form,
    partnerId,
    Call(POST, "/submit")
  )(customRequest, messages).toString()

  "PartnerType Page" should {
    "pass accessibility test" when {
      "is a nominated partner" in {
        render(partnerId = Some("123")) must passAccessibilityChecks
      }

      "is not a nominated partner" in {
        render(partnerId = Some("456")) must passAccessibilityChecks
      }

      "there is an error" in {
        val formWithErrors = PartnerType.form(PartnershipType).withError("error", "error message")

        render(form = formWithErrors, partnerId = Some("456")) must passAccessibilityChecks
      }
    }
  }
}
