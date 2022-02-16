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

package uk.gov.hmrc.plasticpackagingtax.registration.views.enrolment

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.reference_number_already_used_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ReferenceNumberAlreadyUsedFailurePageSpec extends UnitViewSpec with Matchers {

  private val page = inject[reference_number_already_used_failure_page]

  private def createView(): Document =
    page()(journeyRequest, messages)

  "PPT reference number already been used failure page" should {

    val view = createView()

    "contain title" in {
      view.select("title").text() must include(
        messages("enrolment.referenceNumber.already.used.title")
      )
    }

    "contain heading" in {
      view.select("h1").text() mustBe messages("enrolment.referenceNumber.already.used.title")
    }

    "contain detail" in {
      val detail = view.select("p")
      detail.get(1).text() must include(messages("enrolment.referenceNumber.already.used.detail1"))
      detail.get(2).text() must include(messages("enrolment.referenceNumber.already.used.detail2"))
    }

    "display 'Back to your business tax account' button" in {
      view.getElementById("submit").text() mustBe "Back to your business tax account"
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
