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
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.EisError.DUPLICATE_SUBSCRIPTION_ERROR_CODES

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
  val isDuplicateSubscription: Boolean = DUPLICATE_SUBSCRIPTION_ERROR_CODES.contains(code)
}

object EisError {

  implicit val format: OFormat[EisError] =
    Json.format[EisError]

  private val DUPLICATE_SUBSCRIPTION_ERROR_CODES = List("ACTIVE_SUBSCRIPTION_EXISTS",
                                                        "BUSINESS_VALIDATION",
                                                        "ACTIVE_GROUP_SUBSCRIPTION_EXISTS",
                                                        "CANNOT_CREATE_PARTNERSHIP_SUBSCRIPTION"
  )

}

case class SubscriptionCreateResponseFailure(failures: Seq[EisError])
    extends SubscriptionCreateResponse

object SubscriptionCreateResponseFailure {

  implicit val format: OFormat[SubscriptionCreateResponseFailure] =
    Json.format[SubscriptionCreateResponseFailure]

}
