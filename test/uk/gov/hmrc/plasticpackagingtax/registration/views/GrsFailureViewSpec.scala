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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles.{
  gdsPageBodyText,
  gdsPageHeading
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.grs_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class GrsFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: grs_failure_page =
    instanceOf[grs_failure_page]

  private def createView(): Html = page()(journeyRequest, messages)

  "Business Registration Failure Page" should {
    val view: Html = createView()

    "display title" in {
      view.getElementsByClass(gdsPageHeading).first() must containMessage("grs.failure.title")
    }

    "display detail" in {
      view.getElementsByClass(gdsPageBodyText).get(0) must containMessage("grs.failure.detail.1")
      view.getElementsByClass(gdsPageBodyText).get(1) must containMessage("grs.failure.detail.2")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
