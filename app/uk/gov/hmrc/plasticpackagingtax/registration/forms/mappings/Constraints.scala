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

import java.time.LocalDate

trait Constraints {

  private val dateUpperLimit: LocalDate = LocalDate.now()

  val dateLowerLimit = LocalDate.of(2022, 4, 1)

  private val isDateInRange: LocalDate => Boolean = date =>
    (date.isEqual(dateLowerLimit) || date.isBefore(dateLowerLimit)) &&
      (date.isEqual(dateUpperLimit) || date.isAfter(dateUpperLimit))

  def isInDateRange: Constraint[LocalDate] =
    Constraint {
      case request if !isDateInRange.apply(request) =>
        Invalid(ValidationError("liabilityStartDate.outOfRange.error"))
      case _ => Valid
    }

}
