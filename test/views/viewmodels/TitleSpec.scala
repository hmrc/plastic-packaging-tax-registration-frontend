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

package views.viewmodels

import base.unit.MessagesSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class TitleSpec extends MessagesSpec with Matchers {

  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val serviceName = messages("service.name")

  "Title" should {

    "format title without section" in {
      Title("notLiable.pageTitle").toString(messages) must equal(
        s"${messages("notLiable.pageTitle")} - $serviceName - GOV.UK"
      )
    }
  }
}
