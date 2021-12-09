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

import play.api.data.Forms.{longNumber, mapping}
import play.api.data.{Form, Forms}
import play.api.libs.json.Json

case class LiabilityWeight(totalKg: Option[Long]) {}

object LiabilityWeight {
  implicit val format = Json.format[LiabilityWeight]

  val maxTotalKg                = 100000000 // one hundred million
  val minTotalKg                = 0
  val totalKg                   = "totalKg"
  val weightEmptyError          = "liabilityWeight.empty.error"
  val weightOutOfRangeError     = "liabilityWeight.outOfRange.error"
  val weightBelowThresholdError = "liabilityWeight.below.threshold.error"

  private val weightAboveThreshold: Option[Long] => Boolean = weight =>
    weight.forall(value => value >= minTotalKg)

  private val weightWithinRange: Option[Long] => Boolean = weight =>
    weight.forall(value => value <= maxTotalKg)

  def form(): Form[LiabilityWeight] =
    Form(
      mapping(
        totalKg -> Forms.optional(longNumber())
          .verifying(weightEmptyError, _.nonEmpty)
          .verifying(weightBelowThresholdError, weightAboveThreshold)
          .verifying(weightOutOfRangeError, weightWithinRange)
      )(LiabilityWeight.apply)(LiabilityWeight.unapply)
    )

}
