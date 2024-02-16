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

package forms.liability

import forms.YesNoValues
import org.scalatestplus.play.PlaySpec
import play.api.data.Form

class ExceededThresholdWeightSpec extends PlaySpec {

  val sut: Form[Boolean] = new ExceededThresholdWeight().apply()

  "The form" must {

    "bind correctly" when {
      "yes is provided" in {
        val boundForm = sut.bind(Map("value" -> YesNoValues.YES))

        boundForm.value mustBe Some(true)
        boundForm.errors mustBe Nil
      }

      "no is provided" in {
        val boundForm = sut.bind(Map("value" -> YesNoValues.NO))

        boundForm.value mustBe Some(false)
        boundForm.errors mustBe Nil
      }
    }

    "error correctly" when {
      "answer empty" in {
        val boundForm = sut.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.exceededThresholdWeight.question.empty.error")
      }

      "answer is trash" in {
        val boundForm = sut.bind(Map("answer" -> "trash"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.exceededThresholdWeight.question.empty.error")
      }

    }
  }

}
