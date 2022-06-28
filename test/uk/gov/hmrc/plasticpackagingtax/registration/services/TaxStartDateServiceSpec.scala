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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails

import java.time.LocalDate

class YesNoDate private(private val maybeDate: Option[LocalDate]) {
  def fold[B](ifEmpty: => B)(f: LocalDate => B): B = maybeDate.fold(ifEmpty)(f)
}

object YesNoDate {
  def yesAndDate(date: LocalDate) : YesNoDate = new YesNoDate(Some(date))
  def no() : YesNoDate = new YesNoDate(None)
}

class TaxStartDateServiceSpec extends PlaySpec {

  private val dateExceededThresholdWeight = LocalDate.of(2022, 4, 30)
  private val taxStartDateService = new TaxStartDateService

  private val completedLiabilityDetails = LiabilityDetails(
    exceededThresholdWeight = Some(true), 
    dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 1))),   
    expectToExceedThresholdWeight = Some(true), 
    dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 4, 1))),
    expectedWeightNext12m = Some(LiabilityWeight(Some(20000L)))
  )

  "Unanswered questions" should {
    "cause an error" when {

      "all answers are missing" in {
        an [IllegalStateException] must be thrownBy {
          taxStartDateService.calculateTaxStartDate2(LiabilityDetails())
        }
      }

      "the backwards yes-no answer is missing" in {
        val missingBackwardsYesNo = completedLiabilityDetails.copy(exceededThresholdWeight = None)
        the [IllegalStateException] thrownBy {
          taxStartDateService.calculateTaxStartDate2(missingBackwardsYesNo) 
        } must have message "Missing field 'exceededThresholdWeight'"
      }

      "the forwards yes-no answer is missing" in {
        val missingForwardsYesNo = completedLiabilityDetails.copy(expectToExceedThresholdWeight = None)
        the [IllegalStateException] thrownBy {
          taxStartDateService.calculateTaxStartDate2(missingForwardsYesNo)
        } must have message "Missing field 'expectToExceedThresholdWeight'"
      }


      "the forwards date answer is missing" when {

        val missingForwardsDate = completedLiabilityDetails.copy(
          expectToExceedThresholdWeight = Some(true),
          dateRealisedExpectedToExceedThresholdWeight = None)
        
        "the forwards yes-no answer was yes" in  {
          the [IllegalStateException] thrownBy {
            taxStartDateService.calculateTaxStartDate2(missingForwardsDate)
          } must have message "Missing field 'dateRealisedExpectedToExceedThresholdWeight'"
        }
        
        "but not when the forwards yes-no answer was no" in {
          val missingForwardsDate2 = missingForwardsDate.copy(expectToExceedThresholdWeight = Some(false))
          noException should be thrownBy taxStartDateService.calculateTaxStartDate2(missingForwardsDate2)
        }
        
      }
      
      
      "the backwards date is missing" when {

        val missingBackwardsDate = completedLiabilityDetails.copy(
          exceededThresholdWeight = Some(true),
          dateExceededThresholdWeight = None)
        
        "the backwards yes-no answer was yes" in {
          the [IllegalStateException] thrownBy {
            taxStartDateService.calculateTaxStartDate2(missingBackwardsDate)
          } must have message "Missing field 'dateExceededThresholdWeight'"
        }
        
        "but not when the backwards yes-no answer was no" in {
          val missingBackwardsDate2 = missingBackwardsDate.copy(exceededThresholdWeight = Some(false))
          noException should be thrownBy taxStartDateService.calculateTaxStartDate2(missingBackwardsDate2)
        }
        
      }
      
    }
  }

  "Answering no to both questions" should {
    "mean the user is not liable" in {
      val bothAnswersAreNo = LiabilityDetails(
        exceededThresholdWeight = Some(false), 
        expectToExceedThresholdWeight = Some(false)) 
      taxStartDateService.calculateTaxStartDate2(bothAnswersAreNo) mustBe TaxStartDate(isLiable = false)
    }
  }


  "Answering yes to only the backwards question" should {
    "mean the user is liable from the 1st day of the following month" in {
      val onlyBackwardsIsYes = LiabilityDetails(
        exceededThresholdWeight = Some(true),
        dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 14))), 
        expectToExceedThresholdWeight = Some(false)
      )
      val firstDayOfNextMonth: LocalDate = LocalDate.of(2022, 5, 1)
      taxStartDateService.calculateTaxStartDate2(onlyBackwardsIsYes) mustBe TaxStartDate(isLiable = true, Some(firstDayOfNextMonth))
    }
  }

  "Answering yes to only the forwards question" should {
    "mean the user is liable from the date they gave" in {
      val onlyForwardsIsYes = LiabilityDetails(
        expectToExceedThresholdWeight = Some(true),
        dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 4, 17))),
        exceededThresholdWeight = Some(false),
      )
      val sameDateAsUserEntered: LocalDate = LocalDate.of(2022, 4, 17)
      taxStartDateService.calculateTaxStartDate2(onlyForwardsIsYes) mustBe TaxStartDate(isLiable = true, Some(sameDateAsUserEntered))
    }
  }

  "Answering yes to both questions" when {
    "the backwards calculation gives the earliest date" should {
      "mean the user is liable from the first day of the month following the date they gave to the backward question" in {
        val backwardsStartDateIsEarlier = LiabilityDetails(
          exceededThresholdWeight = Some(true),
          dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 14))),
          expectToExceedThresholdWeight = Some(true),
          dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 5, 2))),
        )
        val firstDayOfNextMonth: LocalDate = LocalDate.of(2022, 5, 1)
        taxStartDateService.calculateTaxStartDate2(backwardsStartDateIsEarlier) mustBe TaxStartDate(isLiable = true, Some(firstDayOfNextMonth))
      }
    }
    
    "the forwards calculation gives the earliest date" should {
      "mean the user is liable from the date they gave to the forward question" in {
        val forwardsStartDateIsEarlier = LiabilityDetails(
          exceededThresholdWeight = Some(true),
          dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 14))),
          expectToExceedThresholdWeight = Some(true),
          dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 4, 17))),
        )
        val sameDateAsUserEntered: LocalDate = LocalDate.of(2022, 4, 17)
        taxStartDateService.calculateTaxStartDate2(forwardsStartDateIsEarlier) mustBe TaxStartDate(isLiable = true, Some(sameDateAsUserEntered))
      }
    }

    "the two calculation give the same date" should {
      "make no difference" in {
        val bothStartDatesAreTheSame = LiabilityDetails(
          exceededThresholdWeight = Some(true),
          dateExceededThresholdWeight = Some(Date(LocalDate.of(2022, 4, 14))),
          expectToExceedThresholdWeight = Some(true),
          dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 5, 1))),
        )
        val firstDayOfNextMonth: LocalDate = LocalDate.of(2022, 5, 1)
        taxStartDateService.calculateTaxStartDate2(bothStartDatesAreTheSame) mustBe TaxStartDate(isLiable = true, Some(firstDayOfNextMonth))
      }
    }
  }

  "return 1st day of the month following the date capture" when {
    "plastic package limit are exceeded" in {
      val liabilityDetailsForBreachedThreshold = LiabilityDetails(
        expectToExceedThresholdWeight = Some(false), 
        exceededThresholdWeight = Some(true),
        dateExceededThresholdWeight = Some(forms.Date(dateExceededThresholdWeight))
      )
      taxStartDateService.calculateTaxStartDate2(liabilityDetailsForBreachedThreshold).oldDate mustBe Some(LocalDate.of(2022, 5, 1))
    }
  }

  "return the capture date" when {
    "plastic package limit will be exceed in the next 30 days" in {
      val liabilityDetailsForThresholdWouldBeBreached = LiabilityDetails(
        expectToExceedThresholdWeight = Some(true),
        dateRealisedExpectedToExceedThresholdWeight = Some(forms.Date(LocalDate.of(2022, 4, 30))),
        exceededThresholdWeight = Some(false)
      )
      taxStartDateService.calculateTaxStartDate2(liabilityDetailsForThresholdWouldBeBreached).oldDate mustBe Some(dateExceededThresholdWeight)
    }
  }

}
