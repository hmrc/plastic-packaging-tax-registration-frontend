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

  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate] =
    if (details.exceededThresholdWeight.exists(_ == true))
      details.dateExceededThresholdWeight.map(o => calculateExceedStartDate(o))
    else if (details.expectToExceedThresholdWeight.exists(_ == true))
      details.dateRealisedExpectedToExceedThresholdWeight.map(_.date)
    else None

  private def calculateExceedStartDate(date: Date): LocalDate =
    date.date.plusMonths(1).withDayOfMonth(1)

}

@ImplementedBy(classOf[TaxStartDateServiceImpl])
trait TaxStartDateService {
  def calculateTaxStartDate(details: LiabilityDetails): Option[LocalDate]
}
