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

package views.utils

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{mock, when}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import services.CountryService

import java.time.LocalDate

class ViewUtilsSpec extends PlaySpec {

  private val messages       = mock[Messages]
  private val countryService = mock[CountryService]
  private val sut            = new ViewUtils(countryService)

  "displayLocalDate" should {
    "return a date as a string" in {

      when(messages.apply(any[String])).thenReturn("May")

      val date = LocalDate.parse("2023-05-15")

      sut.displayLocalDate(date)(messages) mustBe "15 May 2023"
    }
  }

}
