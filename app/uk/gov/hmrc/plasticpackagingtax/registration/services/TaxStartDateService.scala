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

import com.google.inject.ImplementedBy
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

case class TaxStartDate (isLiable: Boolean, oldDate: Option[LocalDate] = None)

class TaxStartDateServiceImpl extends TaxStartDateService {

  //todo actually calculate
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate] = Some(LocalDate.of(1996, 3, 27))
  //    if (details.exceededThresholdWeight.contains(true))
  //      details.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
  //    else if (details.expectToExceedThresholdWeight.contains(true))
  //      details.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
  //    else Some(LocalDate.of(1996, 3, 27))

  def calculateTaxStartDate2(liabilityDetails: LiabilityDetails): TaxStartDate = {

    val backwardsIsYes = liabilityDetails.mustHave(_.exceededThresholdWeight, "exceededThresholdWeight")
    val forwardsIsYes = liabilityDetails.mustHave(_.expectToExceedThresholdWeight, "expectToExceedThresholdWeight")
    
    liabilityDetails
      .mightHaveIf(_.dateExceededThresholdWeight, "dateExceededThresholdWeight", backwardsIsYes)
      .mightHaveIf(_.dateRealisedExpectedToExceedThresholdWeight, "dateRealisedExpectedToExceedThresholdWeight", forwardsIsYes)
    
    if (!backwardsIsYes && !forwardsIsYes) {
      TaxStartDate(isLiable = false)
    }
    else {

      val backwardsStartDate = liabilityDetails.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
      val forwardsStartDate = liabilityDetails.dateRealisedExpectedToExceedThresholdWeight.map(_.date)

      val taxStartDate: LocalDate = (backwardsStartDate, forwardsStartDate) match {
        case (Some(backwardsStartDate), None) => backwardsStartDate
        case (None, Some(forwardsStartDate)) => forwardsStartDate
        case (Some(backwardsStartDate), Some(forwardsStartDate)) => earliestOf(backwardsStartDate, forwardsStartDate)
      } 
      
      TaxStartDate(isLiable = true, oldDate = Some(taxStartDate))
    }
  }

  private def earliestOf(a: LocalDate, b: LocalDate): LocalDate = 
    if (a.isBefore(b)) a else b

  private def calculateExceedStartDate(date: Date): LocalDate =
    date.date.plusMonths(1).withDayOfMonth(1)

}

@ImplementedBy(classOf[TaxStartDateServiceImpl])
trait TaxStartDateService {
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate]
}
