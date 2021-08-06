/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, text}

case class ExpectToExceedThresholdWeight(answer: Option[Boolean])

object ExpectToExceedThresholdWeight extends CommonFormValidators {

  lazy val emptyError = "liabilityExpectToExceedThresholdWeightPage.question.empty.error"
  val yes             = "yes"
  val no              = "no"

  def form(): Form[ExpectToExceedThresholdWeight] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(Seq(yes, no)))
      )(ExpectToExceedThresholdWeight.apply)(ExpectToExceedThresholdWeight.unapply)
    )

  def apply(value: String): ExpectToExceedThresholdWeight =
    if (value == yes)
      ExpectToExceedThresholdWeight(Some(true))
    else if (value == no)
      ExpectToExceedThresholdWeight(Some(false))
    else ExpectToExceedThresholdWeight(None)

  def unapply(liableDate: ExpectToExceedThresholdWeight): Option[String] =
    liableDate.answer.flatMap { value =>
      if (value) Some(yes)
      else Some(no)
    }

}
