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

package forms.liability

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import config.AppConfig
import forms.mappings.Mappings
import forms.{CommonFormValidators, CommonFormValues, YesNoValues}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import java.time.{Clock, LocalDate}
import javax.inject.Inject

case class ExpectToExceedThresholdWeightAnswer(yesNo: Boolean, date: Option[LocalDate])

class ExpectToExceedThresholdWeight @Inject()(appConfig: AppConfig, clock: Clock) extends CommonFormValidators with CommonFormValues with Mappings {

  val emptyError = "liability.expectToExceedThresholdWeight.question.empty.error"

  val dateFormattingError = "liability.expectToExceedThreshold.date.invalid"
  val dateOutOfRangeError = "liability.expectToExceedThreshold.date.future"
  val dateEmptyError = "liability.expectToExceedThreshold.date.none"
  val twoRequiredKey = "liability.expectToExceedThreshold.two.required.fields"
  val requiredKey = "liability.expectToExceedThreshold.one.field"

  val beforeLiveDateError =
    "liability.taxStartDate.realisedThresholdWouldBeExceeded.before.goLiveDate.error"

  def apply()(implicit messages: Messages): Form[ExpectToExceedThresholdWeightAnswer] =
    Form(
      mapping(
        "answer" -> toBoolean(emptyError),
        "expect-to-exceed-threshold-weight-date" -> mandatoryIf(isEqual("answer", YesNoValues.YES),
          localDate(emptyDateKey =
            dateEmptyError,
            requiredKey,
            twoRequiredKey,
            dateFormattingError
          ).verifying(
            isInDateRange(dateOutOfRangeError, beforeLiveDateError)(appConfig, clock, messages)
          ))
      )(ExpectToExceedThresholdWeightAnswer.apply)(ExpectToExceedThresholdWeightAnswer.unapply))

}
