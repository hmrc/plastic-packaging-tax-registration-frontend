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

import play.api.libs.json.{Format, Reads, Writes}

object ETMPSubscriptionStatus extends Enumeration {
  type EtmpSubscriptionStatus = Value
  val NO_FORM_BUNDLE_FOUND: Value     = Value
  val SUCCESSFUL: Value               = Value
  val REG_FORM_RECEIVED: Value        = Value
  val SENT_TO_DS: Value               = Value
  val DS_OUTCOME_IN_PROGRESS: Value   = Value
  val REJECTED: Value                 = Value
  val IN_PROCESSING: Value            = Value
  val CREATE_FAILED: Value            = Value
  val WITHDRAWAL: Value               = Value
  val SENT_TO_RCM: Value              = Value
  val APPROVED_WITH_CONDITIONS: Value = Value
  val REVOKED: Value                  = Value
  val DE_REGISTERED: Value            = Value("DE-REGISTERED")
  val CONTRACT_OBJECT_INACTIVE: Value = Value

  implicit val format: Format[EtmpSubscriptionStatus] =
    Format(Reads.enumNameReads(ETMPSubscriptionStatus), Writes.enumNameWrites)

}
