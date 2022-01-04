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
import play.api.data.Forms.{mapping, optional, text}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{CommonFormValidators, CommonFormValues}

case class LiabilityLiableDate(answer: Option[Boolean])

object LiabilityLiableDate extends CommonFormValidators with CommonFormValues {

  val emptyError = "liabilityLiableDatePage.question.empty.error"
  val field      = "answer"

  def form(): Form[LiabilityLiableDate] =
    Form(
      mapping(
        field -> optional(text)
          .verifying(emptyError, _.nonEmpty)
      )(LiabilityLiableDate.toForm)(LiabilityLiableDate.fromForm)
    )

  def toForm(value: Option[String]): LiabilityLiableDate =
    value match {
      case Some(YES) => LiabilityLiableDate(Some(true))
      case Some(NO)  => LiabilityLiableDate(Some(false))
      case _         => LiabilityLiableDate(None)
    }

  def fromForm(liableDate: LiabilityLiableDate): Option[Option[String]] =
    liableDate.answer.map(value => if (value) Some(YES) else Some(NO))

}
