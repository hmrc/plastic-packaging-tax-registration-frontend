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

package forms.liability

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import config.AppConfig
import forms.mappings.Mappings
import forms.{CommonFormValidators, YesNoValues}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class ExceededThresholdWeightDate @Inject()(appConfig: AppConfig, clock: Clock) extends CommonFormValidators with Mappings {

  val dateFormattingError = "liability.exceededThresholdWeightDate.formatting.error"
  val dateOutOfRangeError = "liability.exceededThresholdWeightDate.outOfRange.error"
  val dateEmptyError = "liability.exceededThresholdWeightDate.empty.error"
  val twoRequiredKey = "liability.exceededThresholdWeightDate.two.required.fields"
  val requiredKey = "liability.exceededThresholdWeightDate.one.field"
  val isBeforeLiveDateError = "liability.exceededThresholdWeightDate.before.goLiveDate.error"


  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      mapping(
        "exceeded-threshold-weight-date" -> localDate(emptyDateKey =
            dateEmptyError,
            requiredKey,
            twoRequiredKey,
            dateFormattingError
          ).verifying(
            isInDateRange(dateOutOfRangeError, isBeforeLiveDateError)(appConfig, clock, messages)
          )
      )(identity)(Some.apply)
    )

}