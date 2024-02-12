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

package forms.contact

import base.unit.CommonTestUtils
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import forms.contact.PhoneNumber.{
  maxLength,
  phoneNumber,
  phoneNumberEmptyError,
  phoneNumberInvalidFormat,
  phoneNumberTooLongError
}

class PhoneNumberSpec extends AnyWordSpec with Matchers with CommonTestUtils {

  "Phone number validation rules" should {

    "return success" when {

      "phone number is not empty" in {

        val input = Map(phoneNumber -> "123")

        val form = PhoneNumber.form().bind(input)
        form.errors.size mustBe 0
      }

      "phone number is exactly max allowed amount" in {

        val input = Map(phoneNumber -> randomNumericString(maxLength))

        val form = PhoneNumber.form().bind(input)
        form.errors.size mustBe 0
      }

      "phone number contains allowed characters" in {

        val input = Map(phoneNumber -> "+077 23 23 23")

        val form = PhoneNumber.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map(phoneNumber -> "")
        val expectedErrors = Seq(FormError(phoneNumber, phoneNumberEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains invalid characters" in {

        val input          = Map(phoneNumber -> "&^!a=")
        val expectedErrors = Seq(FormError(phoneNumber, phoneNumberInvalidFormat))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains only whitespaces" in {

        val input          = Map(phoneNumber -> "   ")
        val expectedErrors = Seq(FormError(phoneNumber, phoneNumberEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "exceeds max allowed length" in {

        val input          = Map(phoneNumber -> randomNumericString(maxLength + 1))
        val expectedErrors = Seq(FormError(phoneNumber, phoneNumberTooLongError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = PhoneNumber.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
