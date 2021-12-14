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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partials.amendment.amend_error_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class AmendErrorPageSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[amend_error_page]

  private def createView(): Document =
    page()(journeyRequest, messages)

  "Amend Error page" should {

    val view = createView()

    "contain title" in {
      view.select("title").text() must include(messages("error.title"))
    }

    "contain heading" in {
      view.select("h1").text() mustBe messages("error.title")
    }

    "contain detail" in {
      val detail = view.select("p").text()
      detail must include(messages("error.detail1"))
      detail must not include (messages("error.detail2"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
