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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig

import java.time.{Clock, LocalDate}

trait Constraints {

  private def isDateInRange(appConfig: AppConfig, clock: Clock): LocalDate => Boolean =
    date => !date.isBefore(appConfig.goLiveDate) && !date.isAfter(LocalDate.now(clock))

  def isInDateRange(appConfig: AppConfig, clock: Clock, errorKey: String): Constraint[LocalDate] =
    Constraint {
      case request if !isDateInRange(appConfig, clock).apply(request) =>
        Invalid(ValidationError(errorKey))
      case _ => Valid
    }

}
