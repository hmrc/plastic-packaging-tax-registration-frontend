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

package services

import org.mockito.Mockito.{never, reset, times, verify}
import org.mockito.ArgumentMatchers._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result
import forms.Date
import forms.liability.LiabilityWeight
import models.registration.LiabilityDetails
import services.MustHaveFieldExtension.ExtendedObject

import java.time.LocalDate


class MustHaveFieldExtensionSpec extends PlaySpec with BeforeAndAfterEach {
  // TODO move to own files

  case class Example(x: Option[String])
  
  "mustHave should return the field contents" in {
    val example = Example(Some("the contents of x"))
    example.mustHave(_.x, "x") mustBe "the contents of x" 
  }
  
  "mustHave should complain about an empty field" in {
    val example = Example(None)
    the [IllegalStateException] thrownBy example.mustHave(_.x, "x") must have message "Missing field 'x'" 
  }
  
  "mightHaveIf should ignore an empty field" in {
    val example = Example(None)
    noException should be thrownBy example.mightHaveIf(_.x, "x", predicate = false)
  }
  
  "mightHaveIf should complain about an empty field" in {
    val example = Example(None)
    the [IllegalStateException] thrownBy example.mightHaveIf(_.x, "x", predicate = true) must have message "Missing field 'x'"
  }
  
  "mightHaveIf should be happy with a defined field" in {
    val example = Example(Some("the contents of x"))
    noException should be thrownBy example.mightHaveIf(_.x, "x", predicate = true)
  }
}


class TaxStartDateSpec extends PlaySpec with BeforeAndAfterEach {
  // TODO move to own files

  trait PossibleActions {
    def notLiableAction: Result
    def isLiableAction(date: LocalDate, isDateFromBackwardsTest: Boolean): Result 
  }
  
  private val actions = mock[PossibleActions]
  val aDate: LocalDate = LocalDate.ofEpochDay(0)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(actions)
  }

  "Act should follow the not liable action" in {
    TaxStartDate.notLiable.act(actions.notLiableAction, actions.isLiableAction)
    verify(actions, times(1)).notLiableAction
    verify(actions, never).isLiableAction(any(), any())
  }

  "Act should follow the is liable action" when {

    "liable because of the backwards test" in {
      TaxStartDate.liableFromBackwardsTest(aDate).act(actions.notLiableAction, actions.isLiableAction)
      verify(actions, never).notLiableAction
      verify(actions, times(1)).isLiableAction(aDate, isDateFromBackwardsTest = true)
    }

    "liable because of the forwards test" in {
      TaxStartDate.liableFromForwardsTest(aDate).act(actions.notLiableAction, actions.isLiableAction)
      verify(actions, never).notLiableAction
      verify(actions, times(1)).isLiableAction(aDate, isDateFromBackwardsTest = false)
    }

  }
}


class TaxStartDateServiceSpec extends PlaySpec {

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
          taxStartDateService.calculateTaxStartDate(LiabilityDetails())
        }
      }

      "the backwards yes-no answer is missing" in {
        val missingBackwardsYesNo = completedLiabilityDetails.copy(exceededThresholdWeight = None)
        the [IllegalStateException] thrownBy {
          taxStartDateService.calculateTaxStartDate(missingBackwardsYesNo) 
        } must have message "Missing field 'exceededThresholdWeight'"
      }

      "the forwards yes-no answer is missing" in {
        val missingForwardsYesNo = completedLiabilityDetails.copy(expectToExceedThresholdWeight = None)
        the [IllegalStateException] thrownBy {
          taxStartDateService.calculateTaxStartDate(missingForwardsYesNo)
        } must have message "Missing field 'expectToExceedThresholdWeight'"
      }


      "the forwards date answer is missing" when {

        val missingForwardsDate = completedLiabilityDetails.copy(
          expectToExceedThresholdWeight = Some(true),
          dateRealisedExpectedToExceedThresholdWeight = None)
        
        "the forwards yes-no answer was yes" in  {
          the [IllegalStateException] thrownBy {
            taxStartDateService.calculateTaxStartDate(missingForwardsDate)
          } must have message "Missing field 'dateRealisedExpectedToExceedThresholdWeight'"
        }
        
        "but not when the forwards yes-no answer was no" in {
          val missingForwardsDate2 = missingForwardsDate.copy(expectToExceedThresholdWeight = Some(false))
          noException should be thrownBy taxStartDateService.calculateTaxStartDate(missingForwardsDate2)
        }
        
      }
      
      
      "the backwards date is missing" when {

        val missingBackwardsDate = completedLiabilityDetails.copy(
          exceededThresholdWeight = Some(true),
          dateExceededThresholdWeight = None)
        
        "the backwards yes-no answer was yes" in {
          the [IllegalStateException] thrownBy {
            taxStartDateService.calculateTaxStartDate(missingBackwardsDate)
          } must have message "Missing field 'dateExceededThresholdWeight'"
        }
        
        "but not when the backwards yes-no answer was no" in {
          val missingBackwardsDate2 = missingBackwardsDate.copy(exceededThresholdWeight = Some(false))
          noException should be thrownBy taxStartDateService.calculateTaxStartDate(missingBackwardsDate2)
        }
        
      }
      
    }
  }

  "Answering no to both questions" should {
    "mean the user is not liable" in {
      val bothAnswersAreNo = LiabilityDetails(
        exceededThresholdWeight = Some(false), 
        expectToExceedThresholdWeight = Some(false)) 
      taxStartDateService.calculateTaxStartDate(bothAnswersAreNo) mustBe TaxStartDate.notLiable
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
      taxStartDateService.calculateTaxStartDate(onlyBackwardsIsYes) mustBe TaxStartDate.liableFromBackwardsTest(firstDayOfNextMonth)
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
      taxStartDateService.calculateTaxStartDate(onlyForwardsIsYes) mustBe TaxStartDate.liableFromForwardsTest(sameDateAsUserEntered)
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
        taxStartDateService.calculateTaxStartDate(backwardsStartDateIsEarlier) mustBe TaxStartDate.liableFromBackwardsTest(firstDayOfNextMonth)
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
        taxStartDateService.calculateTaxStartDate(forwardsStartDateIsEarlier) mustBe TaxStartDate.liableFromForwardsTest(sameDateAsUserEntered)
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
        taxStartDateService.calculateTaxStartDate(bothStartDatesAreTheSame) mustBe TaxStartDate.liableFromForwardsTest(firstDayOfNextMonth)
      }
    }
  }

}
