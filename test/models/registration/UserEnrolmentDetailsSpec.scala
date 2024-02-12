/*
 * Copyright 2024 HM Revenue & Customs
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
import spec.PptTestData
import forms.enrolment.IsUkAddress

class UserEnrolmentDetailsSpec extends AnyWordSpec with Matchers with PptTestData {

  private val completeAnswers = userEnrolmentDetails

  "UserEnrolmentDetails" should {
    "be complete" when {
      "all answers are provided" in {
        completeAnswers.isComplete mustBe true
      }
      "postcode is missing for non-uk organisation" in {
        completeAnswers.copy(isUkAddress = Some(IsUkAddress(Some(false))),
                             postcode = None
        ).isComplete mustBe true
      }
    }
    "not be complete" when {
      "no answers are provided" in {
        UserEnrolmentDetails().isComplete mustBe false
      }
      "PPT reference is missing" in {
        completeAnswers.copy(pptReference = None).isComplete mustBe false
      }
      "Is uk address is missing" in {
        completeAnswers.copy(isUkAddress = None).isComplete mustBe false
      }
      "post code is missing when organisation is uk based" in {
        completeAnswers.copy(postcode = None).isComplete mustBe false
      }
      "registration date is missing when organisation is uk based" in {
        completeAnswers.copy(registrationDate = None).isComplete mustBe false
      }
    }
  }

}
