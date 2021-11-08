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

class MembersUnderGroupControlSpec extends AnyWordSpec with Matchers {

  "Members under group control validation" should {

    "return success" when {

      "yes is selected" in {

        val input = Map("value" -> "yes")

        val form = MembersUnderGroupControl.form().bind(input)
        form.errors.size mustBe 0
      }

      "no is selected" in {

        val input = Map("value" -> "no")

        val form = MembersUnderGroupControl.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map.empty[String, String]
        val expectedErrors = Seq(FormError("value", "group.membersUnderGroupControl.error.empty"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = MembersUnderGroupControl.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
