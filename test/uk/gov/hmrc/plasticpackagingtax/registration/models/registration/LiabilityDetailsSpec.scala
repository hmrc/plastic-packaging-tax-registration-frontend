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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, LiabilityWeight}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class LiabilityDetailsSpec extends AnyWordSpec with Matchers {

  "Liability Details TaskStatus" should {

    "be NOT_STARTED " when {
      "liability details is empty" in {
        val liabilityDetails = LiabilityDetails()
        liabilityDetails.status mustBe TaskStatus.NotStarted
      }
    }

    "be IN_PROGRESS " when {
      "liability details is partially filled" in {
        val liabilityDetails =
          LiabilityDetails(startDate = None, weight = Some(LiabilityWeight(Some(4000))))
        liabilityDetails.status mustBe TaskStatus.InProgress
      }
    }

    "be COMPLETED " when {
      "liability details are all filled in" in {
        val liabilityDetails = LiabilityDetails(startDate =
                                                  Some(Date(Some(1), Some(5), Some(2022))),
                                                weight = Some(LiabilityWeight(Some(4000)))
        )
        liabilityDetails.status mustBe TaskStatus.Completed
      }
    }
  }
}
