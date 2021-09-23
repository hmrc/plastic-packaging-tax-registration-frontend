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

package uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions

import play.api.libs.json.{Json, OFormat}

import java.time.ZonedDateTime

trait SubscriptionCreateResponse

case class SubscriptionCreateResponseSuccess(
  pptReference: String,
  processingDate: ZonedDateTime,
  formBundleNumber: String,
  nrsNotifiedSuccessfully: Boolean,
  nrsSubmissionId: Option[String],
  nrsFailureReason: Option[String],
  enrolmentInitiatedSuccessfully: Boolean
) extends SubscriptionCreateResponse

object SubscriptionCreateResponseSuccess {

  implicit val format: OFormat[SubscriptionCreateResponseSuccess] =
    Json.format[SubscriptionCreateResponseSuccess]

}

case class EisError(code: String, reason: String) {
  val isDuplicateSubscription: Boolean = code == "ACTIVE_SUBSCRIPTION_EXISTS"
}

object EisError {

  implicit val format: OFormat[EisError] =
    Json.format[EisError]

}

case class SubscriptionCreateResponseFailure(failures: Seq[EisError])
    extends SubscriptionCreateResponse

object SubscriptionCreateResponseFailure {

  implicit val format: OFormat[SubscriptionCreateResponseFailure] =
    Json.format[SubscriptionCreateResponseFailure]

}
