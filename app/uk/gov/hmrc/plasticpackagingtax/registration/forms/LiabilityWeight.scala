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

import play.api.data.Forms.{longNumber, mapping}
import play.api.data.{Form, Forms}
import play.api.libs.json.Json

case class LiabilityWeight(totalKg: Option[Long]) {}

object LiabilityWeight {
  implicit val format = Json.format[LiabilityWeight]

  val maxTotalKg            = 100000000 // one hundred million
  val minTotalKg            = 1000
  val totalKg               = "totalKg"
  val weightEmptyError      = "liabilityWeight.empty.error"
  val weightOutOfRangeError = "liabilityWeight.outOfRange.error"

  private val isWithinRange: LiabilityWeight => Boolean = weight =>
    minTotalKg to maxTotalKg contains weight.totalKg.getOrElse(0)

  def form(): Form[LiabilityWeight] =
    Form(
      mapping(totalKg -> Forms.optional(longNumber()).verifying(weightEmptyError, _.nonEmpty))(
        LiabilityWeight.apply
      )(LiabilityWeight.unapply)
        .verifying(weightOutOfRangeError, liabilityWeight => isWithinRange(liabilityWeight))
    )

}
