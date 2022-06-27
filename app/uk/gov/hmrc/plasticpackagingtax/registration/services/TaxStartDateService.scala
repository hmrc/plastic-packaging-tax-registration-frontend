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
  }
}

class TaxStartDateServiceImpl extends TaxStartDateService {

  //todo actually calculate
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate] = Some(LocalDate.of(1996, 3, 27))
  //    if (details.exceededThresholdWeight.contains(true))
  //      details.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
  //    else if (details.expectToExceedThresholdWeight.contains(true))
  //      details.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
  //    else Some(LocalDate.of(1996, 3, 27))

  def taxStartDate(liabilityDetails: LiabilityDetails): Option[LocalDate] = {

    val backwardsIsYes = liabilityDetails.mustHave(_.exceededThresholdWeight, "exceededThresholdWeight")
    val forwardsIsYes = liabilityDetails.mustHave(_.expectToExceedThresholdWeight, "expectToExceedThresholdWeight")

    if (backwardsIsYes) {
      val backwardsDate = liabilityDetails.mustHave(_.dateExceededThresholdWeight, "dateExceededThresholdWeight")
    }
    val a = liabilityDetails.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))

    if (forwardsIsYes) {
      val forwardsDate = liabilityDetails.mustHave(_.dateRealisedExpectedToExceedThresholdWeight, "dateRealisedExpectedToExceedThresholdWeight")
      liabilityDetails.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
    }
    else a
//      throw new IllegalStateException("huh?")
    
  }

  private def calculateExceedStartDate(date: Date): LocalDate =
    date.date.plusMonths(1).withDayOfMonth(1)

}

@ImplementedBy(classOf[TaxStartDateServiceImpl])
trait TaxStartDateService {
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate]
}
