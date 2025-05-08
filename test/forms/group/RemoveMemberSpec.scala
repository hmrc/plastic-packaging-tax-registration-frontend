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

package forms.group

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import forms.CommonFormValues

class RemoveMemberSpec extends AnyWordSpec with Matchers with CommonFormValues {

  "Remove Member Form" should {
    "round trip" when {
      "yes is selected" in {
        val yes = RemoveMember.toForm(Some(YES))

        val fromYesForm = RemoveMember.fromForm(yes).flatten
        fromYesForm mustBe Some("yes")

        val backToForm = RemoveMember.toForm(fromYesForm)
        RemoveMember.fromForm(backToForm).flatten mustBe Some("yes")
      }
    }

  }
}
