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
import forms.contact.EmailAddress.{
  emailAddress,
  emailAddressEmptyError,
  emailAddressFormatError
}

class EmailAddressSpec extends AnyWordSpec with Matchers with CommonTestUtils {
  "Email address validation rules" should {

    "return success" when {

      "email address is not empty and is valid" in {

        val input = Map(emailAddress -> "test@test.com")

        val form = EmailAddress.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map(emailAddress -> "")
        val expectedErrors = Seq(FormError(emailAddress, emailAddressEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains non alphabetical characters" in {

        val input          = Map(emailAddress -> "@com")
        val expectedErrors = Seq(FormError(emailAddress, emailAddressFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains only whitespaces" in {

        val input          = Map(emailAddress -> "   ")
        val expectedErrors = Seq(FormError(emailAddress, emailAddressEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "exceeds the max length" in {

        val input = Map(
          emailAddress -> "aasdasdfsdaadsdfsfklgjfdlgjdflgdfjkndflfgjflgjfdlgjdfkgdfkghflkghslkgjhighkdngkngflgdioldlndndkgndfjkgkgdfgkdfgkdhgkdhgkdhgdfkgh@test.com"
        )
        val expectedErrors = Seq(FormError(emailAddress, emailAddressFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = EmailAddress.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
