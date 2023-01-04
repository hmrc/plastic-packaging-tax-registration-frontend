/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.deregistration

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.libs.json.{Json, OFormat}
import forms.{CommonFormValidators, CommonFormValues}
import models.deregistration.DeregistrationReason
import models.deregistration.DeregistrationReason.DeregistrationReason

case class DeregisterReasonForm(answer: Option[DeregistrationReason])

object DeregisterReasonForm extends CommonFormValidators with CommonFormValues {
  implicit val format: OFormat[DeregisterReasonForm] = Json.format[DeregisterReasonForm]

  def form(): Form[DeregisterReasonForm] =
    Form(
      mapping(
        "answer" -> nonEmptyString("deregistration.reason.empty.error")
          .verifying("deregistration.reason.empty.error",
                     contains(DeregistrationReason.values.toSeq.map(_.toString))
          )
      )(DeregisterReasonForm.toForm)(DeregisterReasonForm.fromForm)
    )

  def toForm(value: String): DeregisterReasonForm =
    DeregisterReasonForm(DeregistrationReason.withNameOpt(value))

  def fromForm(reason: DeregisterReasonForm): Option[String] =
    reason.answer.map(_.toString)

}
