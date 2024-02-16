/*
 * Copyright 2024 HM Revenue & Customs
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

package views.viewmodels

import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SignOutReasonSpec extends AnyWordSpec with Matchers with EitherValues {

  "Sign out request params binder" should {

    "bind request params" when {

      "valid" in {

        val result =
          SignOutReason.binder.bind("signOutReason", Map("signOutReason" -> Seq(SignOutReason.UserAction.toString)))

        result.get mustBe Right(SignOutReason.UserAction)
      }
    }

    "bind session timeout request params" when {

      "not valid" in {

        val result =
          SignOutReason.binder.bind("signOutReason", Map("someRubbishKey" -> Seq(SignOutReason.UserAction.toString)))

        result.get mustBe Right(SignOutReason.SessionTimeout)
      }
    }
  }

  "unbind request params" in {

    val result =
      SignOutReason.binder.unbind("signOutReason", SignOutReason.UserAction)

    result mustBe s"signOutReason=${SignOutReason.UserAction}"

  }
}
