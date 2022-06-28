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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import play.api.mvc.Result
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails
import uk.gov.hmrc.plasticpackagingtax.registration.services.MustHaveFieldExtension.ExtendedObject

import java.time.LocalDate
import scala.language.implicitConversions


object MustHaveFieldExtension {
  implicit class ExtendedObject[O](o: O) {

    def mustHave[A](f: O => Option[A], fieldName: String): A =
      f(o).getOrElse {
        throw new IllegalStateException(s"Missing field '$fieldName'")
      }

    def mightHaveIf[A](f: O => Option[A], fieldName: String, predicate: Boolean): O =
      if (predicate && f(o).isEmpty) {
        throw new IllegalStateException(s"Missing field '$fieldName'")
      }
      else o
  }
}


case class TaxStartDate private(
  private val maybeLiableFrom: Option[LocalDate], 
  private val isDateFromBackwardsTest: Boolean
) {
  def act(notLiableAction: => Result, isLiableAction: (LocalDate, Boolean) => Result): Result =
    maybeLiableFrom match {
      case None => notLiableAction
      case Some(date) => isLiableAction(date, isDateFromBackwardsTest)
    }
}

object TaxStartDate {
  def notLiable: TaxStartDate = TaxStartDate(None, isDateFromBackwardsTest = false)
  def liableFromBackwardsTest(date: LocalDate): TaxStartDate = TaxStartDate(Some(date), isDateFromBackwardsTest = true)
  def liableFromForwardsTest(date: LocalDate): TaxStartDate = TaxStartDate(Some(date), isDateFromBackwardsTest = false)
}


class TaxStartDateService {

  def calculateTaxStartDate(liabilityDetails: LiabilityDetails): TaxStartDate = {

    val backwardsIsYes = liabilityDetails.mustHave(_.exceededThresholdWeight, "exceededThresholdWeight")
    val forwardsIsYes = liabilityDetails.mustHave(_.expectToExceedThresholdWeight, "expectToExceedThresholdWeight")

    liabilityDetails
      .mightHaveIf(_.dateExceededThresholdWeight, "dateExceededThresholdWeight", backwardsIsYes)
      .mightHaveIf(_.dateRealisedExpectedToExceedThresholdWeight, "dateRealisedExpectedToExceedThresholdWeight", forwardsIsYes)

    val backwardsStartDate = liabilityDetails.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
    val forwardsStartDate = liabilityDetails.dateRealisedExpectedToExceedThresholdWeight.map(_.date)

    (backwardsStartDate, forwardsStartDate) match {
      case (None, None) => TaxStartDate.notLiable
      case (Some(backwardsStartDate), None) => TaxStartDate.liableFromBackwardsTest(backwardsStartDate)
      case (None, Some(forwardsStartDate)) => TaxStartDate.liableFromForwardsTest(forwardsStartDate)
      case (Some(backwardsStartDate), Some(forwardsStartDate)) => takeEarliestOf(backwardsStartDate, forwardsStartDate)
    }
  }

  private def takeEarliestOf(backwardsDate: LocalDate, forwardsDate: LocalDate): TaxStartDate =
    if (backwardsDate.isAfter(forwardsDate)) 
      TaxStartDate.liableFromForwardsTest(forwardsDate) 
    else 
      TaxStartDate.liableFromBackwardsTest(backwardsDate)

  private def calculateExceedStartDate(date: Date): LocalDate =
    date.date.plusMonths(1).withDayOfMonth(1)

}

