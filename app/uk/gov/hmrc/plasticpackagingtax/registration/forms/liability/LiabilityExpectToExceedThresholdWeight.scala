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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.liability

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{CommonFormValidators, CommonFormValues}

@deprecated("use ExpectToExceedThresholdWeight")
case class LiabilityExpectToExceedThresholdWeight(answer: Option[Boolean])

object LiabilityExpectToExceedThresholdWeight extends CommonFormValidators with CommonFormValues {

  lazy val emptyError = "liabilityExpectToExceedThresholdWeightPage.question.empty.error"

  def form(): Form[LiabilityExpectToExceedThresholdWeight] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(Seq(YES, NO)))
      )(LiabilityExpectToExceedThresholdWeight.apply)(
        LiabilityExpectToExceedThresholdWeight.unapply
      )
    )

  def apply(value: String): LiabilityExpectToExceedThresholdWeight =
    value match {
      case YES => LiabilityExpectToExceedThresholdWeight(Some(true))
      case NO  => LiabilityExpectToExceedThresholdWeight(Some(false))
      case _   => LiabilityExpectToExceedThresholdWeight(None)
    }

  def unapply(liableDate: LiabilityExpectToExceedThresholdWeight): Option[String] =
    liableDate.answer.map(value => if (value) YES else NO)

}
