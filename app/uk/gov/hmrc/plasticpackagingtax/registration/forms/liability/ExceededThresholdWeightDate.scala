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
import play.api.data.Forms.mapping
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.mappings.Mappings

import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
class ExceededThresholdWeightDate @Inject() (appConfig: AppConfig, clock: Clock) extends Mappings {

  val dateFormattingError = "liability.exceededThresholdWeightDate.formatting.error"
  val dateOutOfRangeError = "liability.exceededThresholdWeightDate.outOfRange.error"
  val dateEmptyError      = "liability.exceededThresholdWeightDate.empty.error"
  val twoRequiredKey      = "liability.exceededThresholdWeightDate.two.required.fields"
  val requiredKey         = "liability.exceededThresholdWeightDate.one.field"

  def apply(): Form[Date] =
    Form(
      mapping(
        "exceeded-threshold-weight-date" -> localDate(emptyDateKey =
                                                        dateEmptyError,
                                                      requiredKey,
                                                      twoRequiredKey,
                                                      dateFormattingError
        ).verifying(isInDateRange(appConfig, clock, dateOutOfRangeError))
      )(Date.apply)(Date.unapply)
    )

}
