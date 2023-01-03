/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

class CountryServiceSpec extends AnyWordSpecLike {

  private val countryService = new CountryService()

  ".getAll" should {
    "read country details from resource file" in {
      countryService.getAll().size mustBe 195
    }
  }

  ".getName" should {
    "return country names" in {
      countryService.tryLookupCountryName("GB") mustBe "United Kingdom"
    }

    "return the original country Code" when {
      "country cannot be found for code provided" in {

        countryService.tryLookupCountryName("XX") mustBe "XX"
      }
    }
  }

  ".getKeyForName" should {
    "retrieve the key for a country name in the countriesEN resource" in {
      countryService.getKeyForName("United Kingdom") mustBe Some("GB")
    }
    "retrieve the key for a country name in the synonyms resource if it cannot be found in the countriesEN resource" +
      " but is found in the countrySynonyms resource" in {
      countryService.getKeyForName("england") mustBe Some("GB")
    }
    "return None if the country is not found in the countriesEN or countrySynonyms resources" in {
      countryService.getKeyForName("Mars") mustBe None
    }
  }
}
