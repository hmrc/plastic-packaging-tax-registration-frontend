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

package forms.liability

import config.AppConfig
import forms.liability.ExpectToExceedThresholdWeightDate._
import forms.mappings.Mappings
import play.api.data.Form
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject

class ExpectToExceedThresholdWeightDate @Inject() (appConfig: AppConfig) extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "expect-to-exceed-threshold-weight-date" -> liabilityLocalDate(
        dateEmptyError,
        requiredKey,
        twoRequiredKey,
        dateFormattingError,
        dateOutOfRangeError,
        beforeLiveDateError,
        appConfig
      )
    )

}

object ExpectToExceedThresholdWeightDate {
  val dateFormattingError = "liability.expectToExceedThresholdDate.invalid"
  val dateOutOfRangeError = "liability.expectToExceedThresholdDate.future"
  val dateEmptyError      = "liability.expectToExceedThresholdDate.none"
  val twoRequiredKey      = "liability.expectToExceedThresholdDate.two.required.fields"
  val requiredKey         = "liability.expectToExceedThresholdDate.one.field"

  val beforeLiveDateError =
    "liability.taxStartDate.realisedThresholdWouldBeExceeded.before.goLiveDate.error"

}
