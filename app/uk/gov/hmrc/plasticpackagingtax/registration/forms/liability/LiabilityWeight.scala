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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.liability

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.Json
import uk.gov.voa.play.form.ConditionalMappings.mandatory

import scala.util.Try

case class LiabilityWeight(totalKg: Option[Long]) {}

object LiabilityWeight {
  implicit val format = Json.format[LiabilityWeight]

  val maxTotalKg                   = 100000000 // one hundred million
  val minTotalKg                   = 0
  val totalKg                      = "totalKg"
  val weightEmptyError             = "liabilityWeight.empty.error"
  val weightFormatError            = "liabilityWeight.format.error"
  val weightOutOfRangeError        = "liabilityWeight.outOfRange.error"
  val weightBelowThresholdError    = "liabilityWeight.below.threshold.error"
  val weightLeadingBlankSpaceError = "liabilityWeight.leadingBlankSpace.error"

  private val weightIsValidNumber: String => Boolean = weight =>
    weight.isEmpty || Try(BigDecimal(weight)).isSuccess

  private val weightWithinRange: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigDecimal(weight) <= maxTotalKg

  private val weightAboveThreshold: String => Boolean = weight =>
    weight.isEmpty || !weightIsValidNumber(weight) || BigDecimal(weight) >= minTotalKg

  private val weightHasNoLeadingBlankSpace: String => Boolean = weight =>
    weight.isEmpty || !weight.startsWith(" ")

  def form(): Form[LiabilityWeight] =
    Form(
      mapping(
        totalKg -> mandatory(
          text()
            .verifying(weightEmptyError, _.nonEmpty)
            .verifying(weightLeadingBlankSpaceError, weightHasNoLeadingBlankSpace)
            .transform[String](weight => weight.trim, weight => weight)
            .verifying(weightFormatError, weightIsValidNumber)
            .verifying(weightBelowThresholdError, weightAboveThreshold)
            .verifying(weightOutOfRangeError, weightWithinRange)
        )
      )(LiabilityWeight.fromForm)(LiabilityWeight.toForm)
    )

  def fromForm(totalKg: Option[String]): LiabilityWeight =
    new LiabilityWeight(totalKg.map(BigInt(_).longValue()))

  def toForm(liabilityWeight: LiabilityWeight): Option[Option[String]] =
    Some(liabilityWeight.totalKg.map(_.toString))

}
