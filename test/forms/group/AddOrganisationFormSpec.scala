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

package forms.group

import org.scalatestplus.play.PlaySpec
import play.api.data.{Form, FormError}

class AddOrganisationFormSpec extends PlaySpec {

  val sut: Form[Boolean] = AddOrganisationForm.form()

  "form" must {
    "bind" when {
      "value is 'yes'" in {
        val boundForm = sut.bind(Map("addOrganisation" -> "yes"))

        boundForm.value mustBe Some(true)
        boundForm.errors mustBe empty
      }
      "value is 'no'" in {
        val boundForm = sut.bind(Map("addOrganisation" -> "no"))

        boundForm.value mustBe Some(false)
        boundForm.errors mustBe empty
      }
    }
    "error" when {
      "value is empty" in {
        val boundForm = sut.bind(Map.empty[String, String])

        boundForm.errors mustBe Seq(FormError("addOrganisation", "addOrganisation.empty.error"))
        boundForm.value mustBe None
      }
      "value is absent" in {
        val boundForm = sut.bind(Map("addOrganisation" -> ""))

        boundForm.errors mustBe Seq(FormError("addOrganisation", "addOrganisation.empty.error"))
        boundForm.value mustBe None
      }
    }
  }
}
