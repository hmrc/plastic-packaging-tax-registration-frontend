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

package models.subscriptions

import play.api.libs.json.{Format, Reads, Writes}

object SubscriptionStatus extends Enumeration {
  type Status = Value
  val SUBSCRIBED: Value     = Value
  val NOT_SUBSCRIBED: Value = Value
  val UNKNOWN: Value        = Value

  implicit val format: Format[Status] =
    Format(Reads.enumNameReads(SubscriptionStatus), Writes.enumNameWrites)

}
