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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.liability

import play.api.data.Form
import play.api.data.Forms.{default, mapping, text, tuple}
import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.mappings.Mappings
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{CommonFormValidators, YesNoValues}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import java.time.{Clock, LocalDate}
import javax.inject.Inject

//potential refactor: yesNo isnt actually needed here, we can just do date.isDefined, it holds the same meaning
case class ExceededThresholdWeightAnswer(yesNo: Boolean, date: Option[LocalDate])

class ExceededThresholdWeight @Inject()(appConfig: AppConfig, clock: Clock) extends CommonFormValidators with Mappings {

  val emptyError = "liability.exceededThresholdWeight.question.empty.error"

  val dateFormattingError = "liability.exceededThresholdWeightDate.formatting.error"
  val dateOutOfRangeError = "liability.exceededThresholdWeightDate.outOfRange.error"
  val dateEmptyError = "liability.exceededThresholdWeightDate.empty.error"
  val twoRequiredKey = "liability.exceededThresholdWeightDate.two.required.fields"
  val requiredKey = "liability.exceededThresholdWeightDate.one.field"
  val isBeforeLiveDateError = "liability.exceededThresholdWeightDate.before.goLiveDate.error"


  def form()(implicit messages: Messages): Form[ExceededThresholdWeightAnswer] =
    Form(
      mapping(
        "answer" -> nonEmptyString(emptyError)
          .verifying(emptyError, contains(Seq(YesNoValues.YES, YesNoValues.NO)))
          .transform[Boolean](_ == YesNoValues.YES, bool => if (bool) YesNoValues.YES else YesNoValues.NO),
        "exceeded-threshold-weight-date" -> mandatoryIf(isEqual("answer", YesNoValues.YES),
          tuple(
            "day" -> default(text(), ""),
            "month" -> default(text(), ""),
            "year" -> default(text(), "")
          ).verifying(firstError(
            nonEmptyDate(requiredKey),
            validDate(dateFormattingError))
          ).transform[LocalDate](
            { case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt) },
            date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
          ).verifying(isInDateRange(dateOutOfRangeError, isBeforeLiveDateError)(appConfig, clock, messages))
        ))(ExceededThresholdWeightAnswer.apply)(ExceededThresholdWeightAnswer.unapply))


}
