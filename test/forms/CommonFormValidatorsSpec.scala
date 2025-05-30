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

import base.unit.CommonTestUtils
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.regex.Pattern

class CommonFormValidatorsSpec extends AnyWordSpec with Matchers with CommonFormValidators with CommonTestUtils {

  "CommonFormValidators" should {

    "return true" when {

      "string is not empty" in {

        isNonEmpty("myString") mustBe true
      }

      "string not exceeding max length" in {

        isNotExceedingMaxLength(randomAlphabetString(5), 5) mustBe true
      }

      "string includes only alphabetic and whitespace chars" in {

        containsOnlyAlphaAndWhitespacesAnd(randomAlphabetString(5), None) mustBe true
      }

      "string includes alphabetic,whitespace and allowed chars" in {
        val allowedChars = Some("*£%$")

        containsOnlyAlphaAndWhitespacesAnd("te$t*", allowedChars) mustBe true
      }

      "is matching pattern" in {

        isMatchingPattern("VAL(ID)123", Pattern.compile("^[A-Z0-9()]+$")) mustBe true
      }

      "is valid Email" in {

        isValidEmail("test@test") must be(true)
        isValidEmail("test@test.com") must be(true)
        isValidEmail("#!$%&'*+-/=?^_{}|~@domain.org") must be(true)
      }

      "is valid Telephone number" in {

        isValidTelephoneNumber("07723456765") must be(true)
        isValidTelephoneNumber("077 234 567 65") must be(true)
        isValidTelephoneNumber("+7723456765") must be(true)
      }

      "is valid address Input" in {

        isValidAddressInput("Scala Street") must be(true)
        isValidAddressInput("Scala's Street") must be(true)
        isValidAddressInput("Scala's Street 9 -,.&") must be(true)
      }

      "is valid postcode" in {

        isValidPostcode("LS5 7TH") must be(true)
        isValidPostcode("LS57TH") must be(true)
      }
    }

    "return false" when {

      "string is empty" in {

        isNonEmpty("") mustBe false
      }

      "null value passed" in {

        isNonEmpty(null) mustBe false
      }

      "string is exceeding max length" in {

        isNotExceedingMaxLength(randomAlphabetString(6), 5) mustBe false
      }

      "string includes none alphabetic chars" in {

        containsOnlyAlphaAndWhitespacesAnd(randomAlphabetString(5) + "&", None) mustBe false
      }

      "is not matching pattern" in {

        isMatchingPattern("invalid£", Pattern.compile("^[A-Z0-9&]+$")) mustBe false
      }

      "is not valid email" in {

        isValidEmail("@test") must be(false)
        isValidEmail("test@") must be(false)
      }

      "is not valid phone number" in {

        isValidTelephoneNumber("@%^&*") must be(false)
        isValidTelephoneNumber("test01") must be(false)
      }

      "is not valid address Input" in {

        isValidAddressInput("#####Scala Street%%") must be(false)
        isValidAddressInput("Street with very long long long name really long name") must be(false)
      }

      "is not valid postcode" in {

        isValidPostcode("LS5 7THHHHH") must be(false)
      }
    }

    forAll(
      Seq(
        "AA9A 9AA",
        "A9A 9AA",
        "A9 9AA",
        "A99 9AA",
        "AA99AA",
        "aa9a 9aa",
        "a9a 9aa",
        "a9 9aa",
        "a99 9aa",
        "aa9 9aa",
        "aa9a9aa",
        "a9a9aa",
        "a99aa",
        "a999aa",
        "aa99aa"
      )
    ) { postcode =>
      "return true for " + postcode when {
        "valid" in {
          isValidPostcode(postcode.toUpperCase) must be(true)
        }
      }
    }
  }
}
