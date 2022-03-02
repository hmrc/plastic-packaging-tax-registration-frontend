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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{
  LiabilityExpectedWeight,
  LiabilityWeight
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails.minimumLiabilityWeightKg
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

case class LiabilityDetails(
  // Pre-launch - remove after launch
  expectedWeight: Option[LiabilityExpectedWeight] = None,
  // Old Post-launch - remove after launch
  weight: Option[LiabilityWeight] = None,
  // New Post-launch
  exceededThresholdWeight: Option[Boolean] = None,
  dateExceededThresholdWeight: Option[Date] = None,
  expectToExceedThresholdWeight: Option[Boolean] = None,
  dateRealisedExpectedToExceedThresholdWeight: Option[Date] = None,
  expectedWeightNext12m: Option[LiabilityWeight] = None,
  // Derived fields - not directly input by user
  startDate: Option[OldDate] = None,
  isLiable: Option[Boolean] = None
) {

  private def prelaunchCompleted: Boolean =
    isLiable.contains(true) && expectedWeight.exists(
      expectedWeight =>
        expectedWeight.expectToExceedThresholdWeight.contains(
          true
        ) && expectedWeight.overLiabilityThreshold
    )

  private def postLaunchComplete: Boolean =
    weight.flatMap(_.totalKg).exists(
      _ >= minimumLiabilityWeightKg
    ) || expectToExceedThresholdWeight.contains(true)

  def isCompleted: Boolean =
    startDate.nonEmpty && (prelaunchCompleted || postLaunchComplete)

  def isInProgress: Boolean =
    weight.isDefined || startDate.isDefined || isLiable.isDefined || expectToExceedThresholdWeight.isDefined

  def status: TaskStatus =
    if (isCompleted) TaskStatus.Completed
    else if (isInProgress) TaskStatus.InProgress
    else TaskStatus.NotStarted

}

object LiabilityDetails {
  implicit val format: OFormat[LiabilityDetails] = Json.format[LiabilityDetails]

  val minimumLiabilityWeightKg: Long = 10000L
}
