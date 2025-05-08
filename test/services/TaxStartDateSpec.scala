/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{never, reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result

import java.time.LocalDate

class TaxStartDateSpec extends PlaySpec with BeforeAndAfterEach {

  trait PossibleActions {
    def notLiableAction: Result
    def isLiableAction(date: LocalDate, isDateFromBackwardsTest: Boolean): Result
  }

  private val actions  = mock[PossibleActions]
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
