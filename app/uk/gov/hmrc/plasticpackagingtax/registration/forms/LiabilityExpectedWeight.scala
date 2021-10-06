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
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityExpectedWeight.weightThresholdKg
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.util.Try

case class LiabilityExpectedWeight(
  expectToExceedThresholdWeight: Option[Boolean],
  totalKg: Option[Long]
) {

  def overLiabilityThreshold: Boolean = totalKg.exists(_ >= weightThresholdKg)
}

object LiabilityExpectedWeight {
  implicit val format: OFormat[LiabilityExpectedWeight] = Json.format[LiabilityExpectedWeight]

  val weightThresholdKg         = 10000L
  val maxTotalKg                = 100000000 // one hundred million
  val answer                    = "answer"
  val totalKg                   = "totalKg"
  val yes                       = "yes"
  val no                        = "no"
  val answerError               = "liabilityExpectedWeight.answer.empty.error"
  val weightEmptyError          = "liabilityExpectedWeight.empty.error"
  val weightBelowThresholdError = "liabilityExpectedWeight.below.threshold.error"
  val weightOutOfRangeError     = "liabilityExpectedWeight.outOfRange.error"
  val weightFormatError         = "liabilityExpectedWeight.format.error"

  private val weightIsValidNumber: String => Boolean = weight =>
    weight.isEmpty || Try(BigInt(weight)).isSuccess

  private val weightAboveThreshold: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigInt(weight) >= weightThresholdKg

  private val weightWithinRange: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigInt(weight) <= maxTotalKg

  def form(): Form[LiabilityExpectedWeight] =
    Form(
      mapping(
        answer -> optional(text())
          .verifying(answerError, _.nonEmpty),
        totalKg -> mandatoryIfEqual(answer,
                                    yes,
                                    text()
                                      .verifying(weightEmptyError, _.nonEmpty)
                                      .verifying(weightFormatError, weightIsValidNumber)
                                      .verifying(weightBelowThresholdError, weightAboveThreshold)
                                      .verifying(weightOutOfRangeError, weightWithinRange)
        )
      )(LiabilityExpectedWeight.fromForm)(LiabilityExpectedWeight.toForm)
    )

  def fromForm(answer: Option[String], totalKg: Option[String]): LiabilityExpectedWeight =
    answer match {
      case Some(`yes`) =>
        new LiabilityExpectedWeight(Some(true), totalKg.map(_.toLong))
      case _ => new LiabilityExpectedWeight(Some(false), None)
    }

  def toForm(liabilityWeight: LiabilityExpectedWeight): Option[(Option[String], Option[String])] =
    liabilityWeight.expectToExceedThresholdWeight match {
      case Some(true) => Some((Some(yes), liabilityWeight.totalKg.map(_.toString)))
      case _          => Some((Some(no), None))
    }

}
