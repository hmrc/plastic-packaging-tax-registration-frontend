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

package forms.liability

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.{Json, OFormat}
import uk.gov.voa.play.form.ConditionalMappings.mandatory

import scala.util.Try

case class LiabilityWeight(totalKg: Option[Long]) {}

object LiabilityWeight {
  implicit val format: OFormat[LiabilityWeight] = Json.format[LiabilityWeight]

  val maxTotalKg                = 99999999999L
  val minTotalKg                = 1
  val totalKg                   = "totalKg"
  val weightEmptyError          = "liabilityWeight.empty.error"
  val weightFormatError         = "liabilityWeight.format.error"
  val weightOutOfRangeError     = "liabilityWeight.outOfRange.error"
  val weightBelowThresholdError = "liabilityWeight.below.threshold.error"
  val weightDecimalError        = "liabilityWeight.decimal.error"

  private val weightIsValidNumber: String => Boolean = weight => weight.isEmpty || Try(BigDecimal(weight)).isSuccess

  private val weightIsWholeNumber: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || Try(BigInt(weight)).isSuccess

  private val weightWithinRange: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigDecimal(weight) <= maxTotalKg

  private val weightAboveThreshold: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigDecimal(weight) >= minTotalKg

  def form(): Form[LiabilityWeight] =
    Form(
      mapping(
        totalKg -> mandatory(
          text()
            .verifying(weightEmptyError, _.nonEmpty)
            .transform[String](weight => stripSpacesAndLetters(weight), weight => weight)
            .verifying(weightFormatError, weightIsValidNumber)
            .verifying(weightDecimalError, weightIsWholeNumber)
            .verifying(weightBelowThresholdError, weightAboveThreshold)
            .verifying(weightOutOfRangeError, weightWithinRange)
        )
      )(LiabilityWeight.fromForm)(LiabilityWeight.toForm)
    )

  def fromForm(totalKg: Option[String]): LiabilityWeight =
    new LiabilityWeight(totalKg.map(BigInt(_).longValue))

  def toForm(liabilityWeight: LiabilityWeight): Option[Option[String]] =
    Some(liabilityWeight.totalKg.map(_.toString))

  def stripSpacesAndLetters(weight: String) = {
    val extractNumberRegex = """^[^\d-]*(-?\d+\.?\d*)\D*$""".r

    weight.trim
      .replace(",", "")
      .replace(" ", "") match {
      case extractNumberRegex(number) => number
      case _                          => weight
    }
  }

}
