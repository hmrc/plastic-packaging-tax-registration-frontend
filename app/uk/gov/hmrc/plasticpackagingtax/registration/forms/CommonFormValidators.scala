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

import com.google.common.base.Strings

import java.util.regex.Pattern

trait CommonFormValidators {

  val isNonEmpty: String => Boolean = value => !Strings.isNullOrEmpty(value) && value.trim.nonEmpty

  val isNotExceedingMaxLength: (String, Int) => Boolean = (value, maxLength) =>
    value.isEmpty || value.length <= maxLength

  val containsOnlyAlphaAndWhitespacesAnd: (String, Option[String]) => Boolean =
    (value, allowedChars) =>
      value.isEmpty || value.chars().allMatch(
        char =>
          Character.isLetter(char) || Character.isWhitespace(char) || allowedChars.exists(
            _.contains(char)
          )
      )

  val isMatchingPattern: (String, Pattern) => Boolean = (value, pattern) =>
    value.isEmpty || pattern.matcher(value).matches()

}
