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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

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
        "and 'isPreLaunch' flag is enabled" when {
          "and only 'isLiable' has been answered" in {
            val liabilityDetails =
              LiabilityDetails(isLiable = Some(true), weight = None)
            liabilityDetails.status mustBe TaskStatus.InProgress
          }

          "and only liability weight has been answered" in {
            val liabilityDetails =
              LiabilityDetails(startDate = None,
                               weight = Some(LiabilityWeight(Some(aboveWeightThreshold)))
              )
            liabilityDetails.status mustBe TaskStatus.InProgress
          }

          "and 'is liable' has been answered with false" in {
            val liabilityDetails =
              LiabilityDetails(isLiable = Some(false),
                               weight = Some(LiabilityWeight(Some(aboveWeightThreshold)))
              )
            liabilityDetails.status mustBe TaskStatus.InProgress
          }

          "and weight is less than limit" in {
            val liabilityDetails =
              LiabilityDetails(isLiable = Some(true),
                               weight = Some(LiabilityWeight(Some(belowWeightThreshold)))
              )
            liabilityDetails.status mustBe TaskStatus.InProgress
          }
        }

        "and 'isPreLaunch' flag is disabled" when {
          "and only 'startDate' has been answered" in {
            val liabilityDetails =
              LiabilityDetails(startDate = Some(Date(Some(1), Some(4), Some(2022))), weight = None)
            liabilityDetails.status mustBe TaskStatus.InProgress
          }

          "and only liability weight has been answered" in {
            val liabilityDetails =
              LiabilityDetails(startDate = None,
                               weight = Some(LiabilityWeight(Some(aboveWeightThreshold)))
              )
            liabilityDetails.status mustBe TaskStatus.InProgress
          }
        }

      }
    }

    "be COMPLETED " when {
      "and 'isPreLaunch' flag is enabled" when {
        "and liability details are all correctly filled in" in {
          val liabilityDetails =
            LiabilityDetails(startDate =
                               Some(Date(Some(1), Some(5), Some(2022))),
                             expectedWeight =
                               Some(LiabilityExpectedWeight(Some(true), Some(atWeightThreshold))),
                             isLiable = Some(true)
            )
          liabilityDetails.status mustBe TaskStatus.Completed
        }
      }

      "and 'isPreLaunch' flag is disabled" when {
        "and liability details are all correctly filled in for weight existing exceeding" in {
          val liabilityDetails = LiabilityDetails(startDate =
                                                    Some(Date(Some(1), Some(5), Some(2022))),
                                                  weight =
                                                    Some(LiabilityWeight(Some(atWeightThreshold)))
          )
          liabilityDetails.status mustBe TaskStatus.Completed
        }
        "and liability details are all correctly filled in for weight expected to exceed" in {
          val liabilityDetails =
            LiabilityDetails(startDate =
                               Some(Date(Some(1), Some(5), Some(2022))),
                             weight = Some(LiabilityWeight(Some(belowWeightThreshold))),
                             expectToExceedThresholdWeight = Some(true)
            )
          liabilityDetails.status mustBe TaskStatus.Completed
        }
      }
    }
  }
}
