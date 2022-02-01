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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum

class PartnershipDetailsSpec extends AnyWordSpec with Matchers {
  "PartnershipDetails " should {
    "serialise " when {
      "premises is provided" in {
        val nominatePartner =
          Partner(id = "3534345",
                  partnerType = PartnerTypeEnum.UK_COMPANY,
                  partnerPartnershipDetails = Some(
                    PartnerPartnershipDetails(partnershipName = Some("A named partnership"),
                                              partnershipBusinessDetails =
                                                Some(
                                                  PartnershipBusinessDetails("123456789",
                                                                             "AA11AA",
                                                                             None,
                                                                             Some(
                                                                               RegistrationDetails(
                                                                                 identifiersMatch =
                                                                                   true,
                                                                                 verificationStatus =
                                                                                   Some("PASS"),
                                                                                 registrationStatus =
                                                                                   "REGISTERED",
                                                                                 registeredBusinessPartnerId =
                                                                                   Some(
                                                                                     "XXPPTP123456789"
                                                                                   )
                                                                               )
                                                                             )
                                                  )
                                                )
                    )
                  )
          )

        val partnershipDetails = PartnershipDetails(partnershipType =
                                                      PartnerTypeEnum.GENERAL_PARTNERSHIP,
                                                    partnershipName = Some("Company 1"),
                                                    partnershipBusinessDetails =
                                                      Some(
                                                        PartnershipBusinessDetails("123456780",
                                                                                   "AA11AB",
                                                                                   None,
                                                                                   Some(
                                                                                     RegistrationDetails(
                                                                                       identifiersMatch =
                                                                                         true,
                                                                                       verificationStatus =
                                                                                         Some(
                                                                                           "PASS"
                                                                                         ),
                                                                                       registrationStatus =
                                                                                         "REGISTERED",
                                                                                       registeredBusinessPartnerId =
                                                                                         Some(
                                                                                           "XXPPTP123456780"
                                                                                         )
                                                                                     )
                                                                                   )
                                                        )
                                                      ),
                                                    partners = Seq(nominatePartner)
        )
        partnershipDetails.nominatedPartner.get.partnerPartnershipDetails.get.partnershipName mustBe Some(
          "A named partnership"
        )
      }
    }
  }
}
