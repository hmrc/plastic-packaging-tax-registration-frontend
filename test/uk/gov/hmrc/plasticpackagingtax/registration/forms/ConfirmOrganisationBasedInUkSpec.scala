/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfirmOrganisationBasedInUkSpec extends AnyWordSpec with Matchers {
  "Confirm Organisation Based In Uk" should {
    "correctly apply" when {
      "'yes' is provided" in {
        val confirmCompanyBasedInUk = ConfirmOrganisationBasedInUk.apply("yes")
        confirmCompanyBasedInUk.answer mustBe Some(true)
      }

      "'no' is provided" in {
        val confirmCompanyBasedInUk = ConfirmOrganisationBasedInUk.apply("no")
        confirmCompanyBasedInUk.answer mustBe Some(false)
      }

      " neither 'yes' or 'no' are provided" in {
        val confirmCompanyBasedInUk = ConfirmOrganisationBasedInUk.apply("maybe")
        confirmCompanyBasedInUk.answer mustBe None
      }

      " string is empty" in {
        val confirmCompanyBasedInUk = ConfirmOrganisationBasedInUk.apply("")
        confirmCompanyBasedInUk.answer mustBe None
      }
    }

    "correctly unapply" when {
      "answer is 'Some(true)'" in {
        val confirmCompanyBasedInUk =
          ConfirmOrganisationBasedInUk.unapply(ConfirmOrganisationBasedInUk(Some(true)))
        confirmCompanyBasedInUk mustBe Some("yes")
      }

      "answer is 'Some(false)'" in {
        val confirmCompanyBasedInUk =
          ConfirmOrganisationBasedInUk.unapply(ConfirmOrganisationBasedInUk(Some(false)))
        confirmCompanyBasedInUk mustBe Some("no")
      }

      "answer is None" in {
        val confirmCompanyBasedInUk =
          ConfirmOrganisationBasedInUk.unapply(ConfirmOrganisationBasedInUk(None))
        confirmCompanyBasedInUk mustBe None
      }
    }
  }
}
