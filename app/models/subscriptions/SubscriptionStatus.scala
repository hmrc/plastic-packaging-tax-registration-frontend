/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.*

import scala.util.Try

enum SubscriptionStatus:
  case SUBSCRIBED, NOT_SUBSCRIBED, UNKNOWN

object SubscriptionStatus {
  given Format[SubscriptionStatus] =
    Format(
      Reads {
        case JsString(value) =>
          Try(SubscriptionStatus.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown SubscriptionStatus: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(subscriptionStatus => JsString(subscriptionStatus.toString))
    )
}
