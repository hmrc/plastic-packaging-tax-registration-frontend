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

package registration.contact

import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{PrimaryContactDetails, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.check_primary_contact_details_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckContactDetailsAnswersViewA11ySpec extends BaseViewSpec {

  private val page = inject[check_primary_contact_details_page]

  private val registration = aRegistration(
    withPrimaryContactDetails(
      PrimaryContactDetails(name = Some("Jack Gatsby"),
                            jobTitle = Some("Developer"),
                            phoneNumber = Some("0203 4567 890"),
                            email = Some("test@test.com"),
                            address = Some(
                              Address(addressLine1 = "2 Scala Street",
                                      addressLine2 = Some("Soho"),
                                      addressLine3 = None,
                                      townOrCity = "London",
                                      maybePostcode = Some("W1T 2HN"),
                                     countryCode = "GB"
                              )
                            )
      )
    )
  )


  private def createView(reg: Registration = registration): String =
    page(reg)(journeyRequest, messages).toString()

  "Check primary contact details answers View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}