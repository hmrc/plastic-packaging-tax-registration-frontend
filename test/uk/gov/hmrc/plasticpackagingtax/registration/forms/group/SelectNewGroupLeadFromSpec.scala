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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.group

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError

class SelectNewGroupLeadFromSpec extends PlaySpec {

  val sut: SelectNewGroupLeadForm = new SelectNewGroupLeadForm


  "the form" should {
    "bind" when {
      "a value is in the members list" in {
        val boundForm = sut.apply(Seq("abc")).bind(Map("value" -> "abc"))

        boundForm.value mustBe Some("abc")
        boundForm.errors mustBe empty
      }

      "a value is in the members list amoung others " in {
        val boundForm = sut.apply(Seq("xyz", "abc")).bind(Map("value" -> "abc"))

        boundForm.value mustBe Some("abc")
        boundForm.errors mustBe empty
      }
    }
    "error" when {
      "the form is empty" in {
        val boundForm = sut.apply(Seq("xyz", "abc")).bind(Map[String, String]())

        boundForm.errors mustBe Seq(FormError("value", "SelectNewGroupLead.error.required"))
        boundForm.value mustBe None
      }
      "the form has no value" in {
        val boundForm = sut.apply(Seq("xyz", "abc")).bind(Map("cheese" -> "abc"))

        boundForm.errors mustBe Seq(FormError("value", "SelectNewGroupLead.error.required"))
        boundForm.value mustBe None
      }
      "the form value is empty" in {
        val boundForm = sut.apply(Seq("xyz", "abc")).bind(Map("value" -> ""))

        boundForm.errors mustBe Seq(FormError("value", "SelectNewGroupLead.error.required"))
        boundForm.value mustBe None
      }

      "the value is not in the members list" in {
        val boundForm = sut.apply(Seq("xyz", "abc")).bind(Map("value" -> "pan"))

        boundForm.errors mustBe Seq(FormError("value", "SelectNewGroupLead.error.required"))
        boundForm.value mustBe None
      }
    }
  }

}
