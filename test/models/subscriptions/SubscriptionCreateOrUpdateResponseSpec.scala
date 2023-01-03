/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class SubscriptionCreateOrUpdateResponseSpec
    extends AnyWordSpecLike with TableDrivenPropertyChecks {

  "EisError" should {
    "identify duplicate subscription errors" in {
      val duplicateSubscriptionErrorCodes = Table("Error code",
                                                  "ACTIVE_SUBSCRIPTION_EXISTS",
                                                  "BUSINESS_VALIDATION",
                                                  "ACTIVE_GROUP_SUBSCRIPTION_EXISTS",
                                                  "CANNOT_CREATE_PARTNERSHIP_SUBSCRIPTION"
      )

      forAll(duplicateSubscriptionErrorCodes) { errorCode =>
        EisError(errorCode, "xxx").isDuplicateSubscription mustBe true
      }
    }
    "identify non duplicate subscription errors" in {
      val nonDuplicateSubscriptionErrorCodes =
        Table("Error code", "INVALID_REGIME", "INVALID_SAFEID")

      forAll(nonDuplicateSubscriptionErrorCodes) { errorCode =>
        EisError(errorCode, "xxx").isDuplicateSubscription mustBe false
      }
    }
  }
}
