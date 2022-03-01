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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.mappings.Mappings

import java.time.LocalDate
import javax.inject.Inject
import scala.util.Try

class ExceededThresholdWeightDate @Inject extends Mappings {

  val dateFormattingError = "liabilityStartDate.formatting.error"
  val dateOutOfRangeError = "liabilityStartDate.outOfRange.error"
  val dateEmptyError      = "liabilityStartDate.empty.error"
  val twoRequiredKey      = "date.missing.two.required.fields"
  val requiredKey         = "date.missing.one.field"

  def apply(): Form[Date] =
    Form(
      mapping(
        "liability-start-date" -> localDate(emptyDateKey =
                                              dateEmptyError,
                                            requiredKey,
                                            twoRequiredKey,
                                            dateFormattingError
        ).verifying(isInDateRange)
      )(Date.apply)(Date.unapply)
    )

}
