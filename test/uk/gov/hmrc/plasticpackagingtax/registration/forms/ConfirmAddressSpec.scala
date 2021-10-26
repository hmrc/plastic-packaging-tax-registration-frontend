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
import play.api.data.FormError

class ConfirmAddressSpec extends AnyWordSpec with Matchers {

  "Confirm Address Form" should {

    "return success" when {

      "yes is selected" in {
        val input = Map(ConfirmAddress.field -> ConfirmAddress.YES)

        val form = ConfirmAddress.form().bind(input)
        form.errors.size mustBe 0
      }

      "no is selected" in {
        val input = Map(ConfirmAddress.field -> ConfirmAddress.NO)

        val form = ConfirmAddress.form().bind(input)
        form.errors.size mustBe 0
      }

    }

    "return errors" when {

      "nothing is selected" in {
        val input = Map[String, String]()

        val form = ConfirmAddress.form().bind(input)
        form.errors.size mustBe 1
        form.errors must contain(FormError(ConfirmAddress.field, ConfirmAddress.emptyError))
      }

    }
  }
}
