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

import com.google.inject.Singleton
import play.api.data.Form
import play.api.data.Forms.{localDate, mapping}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.mappings.Mappings

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.util.Try

@Singleton
class ExpectToExceedThresholdWeightDate @Inject() (appConfig: AppConfig, clock: Clock)
    extends Mappings {

  val emptyError          = "liability.expectToExceedThreshold.date.none"
  val dateFormattingError = "liability.expectToExceedThreshold.date.invalid"
  val dateOutOfRangeError = "liability.expectToExceedThreshold.date.future"
  val twoRequiredKey      = "liability.expectToExceedThreshold.two.required.fields"
  val requiredKey         = "liability.expectToExceedThreshold.one.field"

  def apply(): Form[Date] =
    Form(
      mapping(
        "expect-to-exceed-threshold-weight-date" -> localDate(emptyDateKey =
                                                                emptyError,
                                                              requiredKey,
                                                              twoRequiredKey,
                                                              dateFormattingError
        ).verifying(isInDateRange(dateOutOfRangeError)(appConfig, clock))
      )(Date.apply)(Date.unapply)
    )

}
