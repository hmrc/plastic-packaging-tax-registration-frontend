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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  CHARITY_OR_NOT_FOR_PROFIT,
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}

class OrganisationTypeSpec extends AnyWordSpec with Matchers {
  "Organisation Type" should {
    "correctly apply" when {
      "'UK_COMPANY' is provided" in {
        val organisationType = OrganisationType.apply(UK_COMPANY.toString)
        organisationType.answer mustBe Some(UK_COMPANY)
      }

      "'SOLE_TRADER' is provided" in {
        val organisationType = OrganisationType.apply(SOLE_TRADER.toString)
        organisationType.answer mustBe Some(SOLE_TRADER)
      }

      "'PARTNERSHIP' is provided" in {
        val organisationType = OrganisationType.apply(PARTNERSHIP.toString)
        organisationType.answer mustBe Some(PARTNERSHIP)
      }

      "'CHARITY_OR_NOT_FOR_PROFIT' is provided" in {
        val organisationType = OrganisationType.apply(CHARITY_OR_NOT_FOR_PROFIT.toString)
        organisationType.answer mustBe Some(CHARITY_OR_NOT_FOR_PROFIT)
      }

      "invalid value is provided" in {
        val organisationType = OrganisationType.apply("maybe")
        organisationType.answer mustBe None
      }

      "answer is empty" in {
        val organisationType = OrganisationType.apply("")
        organisationType.answer mustBe None
      }
    }

    "correctly unapply" when {
      "answer is valid" in {
        val organisationType = OrganisationType.unapply(OrganisationType(Some(UK_COMPANY)))
        organisationType mustBe Some(UK_COMPANY.toString)
      }

      "answer is empty" in {
        val organisationType = OrganisationType.unapply(OrganisationType(""))
        organisationType mustBe None
      }
    }
  }
}
