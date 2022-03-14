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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.mappings

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig

import java.time.{Clock, LocalDate}

trait Constraints {

  private def isDateInFuture(clock: Clock): LocalDate => Boolean =
    date => !date.isAfter(LocalDate.now(clock))

  private def isDateBeforeLiveDate(appConfig: AppConfig): LocalDate => Boolean =
    date => !date.isBefore(appConfig.goLiveDate)

  private def getMonth(appConfig: AppConfig)(implicit messages: Messages) =
    messages(s"date.month.${appConfig.goLiveDate.getMonthValue}")

  def isInDateRange(errorKey: String, beforeLiveDateErrorKey: String)(implicit
    appConfig: AppConfig,
    clock: Clock,
    messages: Messages
  ): Constraint[LocalDate] = {
    val goLiveDate = appConfig.goLiveDate
    Constraint {
      case request if !isDateBeforeLiveDate(appConfig).apply(request) =>
        Invalid(
          ValidationError(
            messages(beforeLiveDateErrorKey,
                     s"${goLiveDate.getDayOfMonth}  ${getMonth(appConfig)} ${goLiveDate.getYear}"
            )
          )
        )
      case request if !isDateInFuture(clock).apply(request) =>
        Invalid(ValidationError(errorKey))
      case _ => Valid
    }
  }

}
