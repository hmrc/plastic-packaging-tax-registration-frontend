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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails

import java.time.LocalDate

class TaxStartDateServiceSpec extends PlaySpec {

  private val dateExceededThresholdWeight = LocalDate.of(2022, 4, 30)
  "return 1st day of the month following the date capture" when {
    "plastic package limit are exceeded" in {
      new TaxStartDateServiceImpl()
        .calculateTaxStartDate(createLiabilityDetailsForBreachedThreshold) mustBe
        Some(LocalDate.of(2022, 5, 1))
    }
  }

  "return the capture date" when {
    "plastic package limit will be exceed in the next 30 days" in {
      new TaxStartDateServiceImpl()
        .calculateTaxStartDate(createLiabilityDetailsForThresholdWouldBeBreached) mustBe
        Some(dateExceededThresholdWeight)
    }
  }

  "return an error" when {
    "no date is set" in {
      new TaxStartDateServiceImpl()
        .calculateTaxStartDate(LiabilityDetails()) mustBe None
    }
  }

  private def createLiabilityDetailsForThresholdWouldBeBreached =
    LiabilityDetails(dateRealisedExpectedToExceedThresholdWeight =
      Some(forms.Date(LocalDate.of(2022, 4, 30)))
    )

  private def createLiabilityDetailsForBreachedThreshold =
    LiabilityDetails(dateExceededThresholdWeight = Some(forms.Date(dateExceededThresholdWeight)))

}
