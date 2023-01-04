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

package models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import forms.liability.LiabilityWeight
import forms.{Date, OldDate}
import views.viewmodels.TaskStatus

import java.time.LocalDate

class LiabilityDetailsSpec extends AnyWordSpec with Matchers {

  private val completedDetails = LiabilityDetails(
    expectToExceedThresholdWeight = Some(true),
    dateExceededThresholdWeight = Some(Date(LocalDate.parse("2022-03-05"))),
    expectedWeightNext12m = Some(LiabilityWeight(Some(12000))),
    startDate = Some(OldDate(Some(5), Some(3), Some(2022))),
    newLiabilityStarted = Some(NewLiability),
    newLiabilityFinished = Some(NewLiability)
  )

  "Liability Details TaskStatus" should {

    "be NOT_STARTED " when {
      "liability details is empty" in {
        val liabilityDetails = LiabilityDetails()
        liabilityDetails.status mustBe TaskStatus.NotStarted
      }
      "liability details have flags (somehow) but no answers" in {
        val liabilityDetails = LiabilityDetails(
          newLiabilityStarted = Some(NewLiability)
        )
        liabilityDetails.status mustBe TaskStatus.NotStarted
      }
    }

    "be IN_PROGRESS " when {
      "liability details are partially filled" in {
        val liabilityDetails = LiabilityDetails(
          expectToExceedThresholdWeight = Some(true),
          newLiabilityStarted = Some(NewLiability)
        )
        liabilityDetails.status mustBe TaskStatus.InProgress
      }

      "liability details are partially filled with forwards answer" in {
        val liabilityDetails = LiabilityDetails(
          expectToExceedThresholdWeight = Some(true),
        )
        liabilityDetails.status mustBe TaskStatus.InProgress
      }

      "liability details are partially filled with backwards answer" in {
        val liabilityDetails = LiabilityDetails(
          exceededThresholdWeight = Some(true),
        )
        liabilityDetails.status mustBe TaskStatus.InProgress
      }

      "liability details are partially filled with expectedWeight answer" in {
        val liabilityDetails = LiabilityDetails(
          expectedWeightNext12m = Some(LiabilityWeight(Some(6969))),
        )
        liabilityDetails.status mustBe TaskStatus.InProgress
      }
    }

    "be COMPLETED " when {

      "post-launch" in {
        // TODO this test needs updating?
        completedDetails.status mustBe TaskStatus.Completed
      }
    }

    "clear some of the previous questions" in {
      val fullyCompleteddetails = completedDetails.copy(
        // TODO missing fields        
      )
      val updatedDetails = fullyCompleteddetails.clearOldLiabilityAnswers mustBe LiabilityDetails(
        expectToExceedThresholdWeight = None,
        dateExceededThresholdWeight = None,
        expectedWeightNext12m = Some(LiabilityWeight(Some(12000))),
        startDate = None,
        newLiabilityStarted = Some(NewLiability),
        newLiabilityFinished = Some(NewLiability)
      )
    }
  }
}
