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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

import java.time.LocalDate

class LiabilityDetailsSpec extends AnyWordSpec with Matchers {

  private val belowWeightThreshold = 9999
  private val atWeightThreshold    = 10000
  private val aboveWeightThreshold = 10001

  "Liability Details TaskStatus" should {

    "be NOT_STARTED " when {
      "liability details is empty" in {
        val liabilityDetails = LiabilityDetails()
        liabilityDetails.status mustBe TaskStatus.NotStarted
      }
    }

    "be IN_PROGRESS " when {
      "liability details are partially filled" when {
        "pre-launch" in {
          val liabilityDetails = LiabilityDetails(expectedWeight =
            Some(
              LiabilityExpectedWeight(expectToExceedThresholdWeight = Some(true), totalKg = None)
            )
          )
          liabilityDetails.status mustBe TaskStatus.InProgress
        }

        "post-launch" in {
          val liabilityDetails = LiabilityDetails(exceededThresholdWeight = Some(true))
          liabilityDetails.status mustBe TaskStatus.InProgress
        }

      }
    }

    "be COMPLETED " when {
      "pre-launch" in {
        val liabilityDetails = LiabilityDetails(
          expectedWeight =
            Some(
              LiabilityExpectedWeight(expectToExceedThresholdWeight = Some(true),
                                      totalKg = Some(12000)
              )
            ),
          startDate = Some(OldDate(Some(5), Some(3), Some(2022)))
        )
        liabilityDetails.status mustBe TaskStatus.Completed
      }

      "post-launch" in {
        val liabilityDetails = LiabilityDetails(exceededThresholdWeight = Some(true),
                                                dateExceededThresholdWeight =
                                                  Some(Date(LocalDate.parse("2022-03-05"))),
                                                expectedWeightNext12m =
                                                  Some(LiabilityWeight(Some(12000))),
                                                startDate =
                                                  Some(OldDate(Some(5), Some(3), Some(2022)))
        )
        liabilityDetails.status mustBe TaskStatus.Completed
      }
    }
  }
}
