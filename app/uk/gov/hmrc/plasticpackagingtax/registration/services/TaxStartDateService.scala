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

import java.time.LocalDate

class TaxStartDateServiceImpl extends TaxStartDateService {

  //todo actually calculate
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate] = Some(LocalDate.of(1996, 3, 27))
  //    if (details.exceededThresholdWeight.contains(true))
  //      details.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
  //    else if (details.expectToExceedThresholdWeight.contains(true))
  //      details.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
  //    else Some(LocalDate.of(1996, 3, 27))


  def taxStartDate(liabilityDetails: LiabilityDetails): Option[LocalDate] = {
    
    if (liabilityDetails.exceededThresholdWeight.isEmpty)
      throw new IllegalStateException("Missing answer for 'exceededThresholdWeight'")
    
    if (liabilityDetails.dateExceededThresholdWeight.isEmpty && liabilityDetails.exceededThresholdWeight.get)
      throw new IllegalStateException("Missing answer for 'dateExceededThresholdWeight'")

    if (liabilityDetails.expectToExceedThresholdWeight.isEmpty)
      throw new IllegalStateException("Missing answer for 'expectToExceedThresholdWeight'")
      
    if (liabilityDetails.dateRealisedExpectedToExceedThresholdWeight.isEmpty && liabilityDetails.expectToExceedThresholdWeight.get)
      throw new IllegalStateException("Missing answer for 'dateRealisedExpectedToExceedThresholdWeight'")

    if (liabilityDetails.exceededThresholdWeight.contains(true))
      liabilityDetails.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
    else if (liabilityDetails.expectToExceedThresholdWeight.contains(true))
      liabilityDetails.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
    else 
      throw new IllegalStateException("huh?") 
  }

  private def calculateExceedStartDate(date: Date): LocalDate =
    date.date.plusMonths(1).withDayOfMonth(1)

}

@ImplementedBy(classOf[TaxStartDateServiceImpl])
trait TaxStartDateService {
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate]
}
