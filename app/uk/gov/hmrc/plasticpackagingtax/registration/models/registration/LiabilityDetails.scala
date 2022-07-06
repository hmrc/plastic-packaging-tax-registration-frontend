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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

case object NewLiability {
  implicit val format: OFormat[NewLiability.type] = Json.format[NewLiability.type]
}

case class LiabilityDetails(exceededThresholdWeight: Option[Boolean] = None,
                            dateExceededThresholdWeight: Option[Date] = None,
                            expectToExceedThresholdWeight: Option[Boolean] = None,
                            dateRealisedExpectedToExceedThresholdWeight: Option[Date] = None,
                            expectedWeightNext12m: Option[LiabilityWeight] = None,
                            // Derived fields - not directly input by user
                            startDate: Option[OldDate] = None,
                            isLiable: Option[Boolean] = None,
                            newLiabilityFinished: Option[NewLiability.type] = None,
                            newLiabilityStarted: Option[NewLiability.type] = None
                           ) {

  def isCompleted: Boolean =
    startDate.nonEmpty && expectedWeightNext12m.isDefined && newLiabilityStarted.isDefined && newLiabilityFinished.isDefined

  def isInProgress: Boolean = exceededThresholdWeight.isDefined || expectToExceedThresholdWeight.isDefined

  def status: TaskStatus =
    if (isCompleted) TaskStatus.Completed
    else if (isInProgress) TaskStatus.InProgress
    else TaskStatus.NotStarted

}

object LiabilityDetails {
  implicit val format: OFormat[LiabilityDetails] = Json.format[LiabilityDetails]

  val minimumLiabilityWeightKg: Long = 10000L
}
