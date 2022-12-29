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

package forms.contact

import base.unit.CommonTestUtils
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import forms.contact.JobTitle.{
  jobTitle,
  jobTitleEmptyError,
  jobTitleNonAlphaCharError,
  jobTitleTooLongError,
  maxLength
}

class JobTitleSpec extends AnyWordSpec with Matchers with CommonTestUtils {

  "Job title validation rules" should {

    "return success" when {

      "job title is not empty" in {

        val input = Map(jobTitle -> "myjobtitle")

        val form = JobTitle.form().bind(input)
        form.errors.size mustBe 0
      }

      "job title is exactly max allowed amount" in {

        val input = Map(jobTitle -> randomAlphabetString(maxLength))

        val form = JobTitle.form().bind(input)
        form.errors.size mustBe 0
      }

      "job title contains only alphabetical and whitespace characters" in {

        val input = Map(jobTitle -> "abc def n")

        val form = JobTitle.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map(jobTitle -> "")
        val expectedErrors = Seq(FormError(jobTitle, jobTitleEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains non alphabetical characters" in {

        val input          = Map(jobTitle -> "20A#")
        val expectedErrors = Seq(FormError(jobTitle, jobTitleNonAlphaCharError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains only whitespaces" in {

        val input          = Map(jobTitle -> "   ")
        val expectedErrors = Seq(FormError(jobTitle, jobTitleEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "exceeds max allowed length" in {

        val input          = Map(jobTitle -> randomAlphabetString(maxLength + 1))
        val expectedErrors = Seq(FormError(jobTitle, jobTitleTooLongError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = JobTitle.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
