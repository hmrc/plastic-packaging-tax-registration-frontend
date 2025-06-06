/*
 * Copyright 2025 HM Revenue & Customs
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

package forms

import com.google.common.base.Strings
import play.api.data.Forms.{optional, text}
import play.api.data.Mapping

import java.util.regex.Pattern

trait CommonFormValidators {

  val fullNameRegexPattern: Pattern     = Pattern.compile("^[A-Za-z .'-]{1,160}$")
  val nameRegexPattern: Pattern         = Pattern.compile("^[A-Za-z .'-]{1,35}$")
  val phoneNumberRegexPattern: Pattern  = Pattern.compile("^[+]?[0-9 ]*$")
  val addressInputRegexPattern: Pattern = Pattern.compile("^[A-Za-z0-9 \\-,.&']{1,35}$")

  val emailPattern: Pattern =
    Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$")

  val postcodeRegexPattern: Pattern =
    Pattern.compile("^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,10}$")

  val isProvided: String => Boolean = value => value.nonEmpty

  val isNonEmpty: String => Boolean = value => !Strings.isNullOrEmpty(value) && value.trim.nonEmpty

  val isNoneWhiteSpace: String => Boolean = value => value.isEmpty || value.exists(char => !char.isWhitespace)

  val isNotExceedingMaxLength: (String, Int) => Boolean = (value, maxLength) =>
    value.isEmpty || value.length <= maxLength

  val isNotExceedingMaxLengthExcludingWhitespaces: (String, Int) => Boolean = (value, maxLength) => {
    val cleaned = value.filter(c => !Character.isWhitespace(c)).mkString
    cleaned.isEmpty || cleaned.length <= maxLength
  }

  val containsOnlyAlphaAndWhitespacesAnd: (String, Option[String]) => Boolean =
    (value, allowedChars) =>
      value.isEmpty || value.chars().allMatch(char =>
        Character.isLetter(char) || Character.isWhitespace(char) || allowedChars.exists(_.contains(char.toChar))
      )

  val isMatchingPattern: (String, Pattern) => Boolean = (value, pattern) => pattern.matcher(value).matches()

  val isValidFullName: String => Boolean =
    isValidAnyName(fullNameRegexPattern, 160)

  val isValidName: String => Boolean =
    isValidAnyName(nameRegexPattern, 35)

  val isValidEmail: String => Boolean = (email: String) => email.isEmpty || isMatchingPattern(email, emailPattern)

  val isValidTelephoneNumber: String => Boolean = (value: String) =>
    value.isEmpty || isMatchingPattern(value, phoneNumberRegexPattern)

  val isValidAddressInput: String => Boolean = (value: String) =>
    value.isEmpty || isMatchingPattern(value, addressInputRegexPattern)

  val isValidPostcode: String => Boolean = (value: String) =>
    value.isEmpty || isMatchingPattern(value, postcodeRegexPattern)

  val contains: Seq[String] => String => Boolean = seq => choice => seq.contains(choice)

  protected val validatePostcode: Int => String => Boolean =
    (length: Int) => (input: String) => isNotExceedingMaxLength(input, length) && isValidPostcode(input.toUpperCase)

  def nonEmptyString(errorKey: String): Mapping[String] =
    optional(text)
      .verifying(errorKey, _.nonEmpty)
      .transform[String](_.get, Some.apply)
      .verifying(errorKey, _.trim.nonEmpty)

  private def isValidAnyName(pattern: Pattern, len: Int): String => Boolean =
    (name: String) => (name.size > len || name.isEmpty) || isMatchingPattern(name, pattern)

}
