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

package views

import base.unit.UnitViewSpec
import models.response.FlashKeys
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Flash
import play.twirl.api.Html
import views.components.Styles._
import views.html.enrolment.enrolment_failure_page

class EnrolmentFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: enrolment_failure_page =
    inject[enrolment_failure_page]

  private def createView(flash: Flash = Flash(Map.empty)): Html =
    page()(registrationJourneyRequest, messages, flash)

  "Enrolment Failure Page" should {
    val view: Html = createView()

    "display title" in {
      view.getElementsByClass(gdsPageHeading).first() must containMessage("enrolment.failure.title")
    }

    "display detail with referenceId" in {
      val viewWithReferenceId = createView(Flash(Map(FlashKeys.referenceId -> "PPT123")))
      val pptReferenceText    = viewWithReferenceId.getElementsByClass(gdsPageBodyText)
      pptReferenceText.get(0) must containMessage(
        "enrolment.failure.detail.1",
        "PPT123",
        messages("enrolment.failure.detail.1.text")
      )
    }

    "display detail with no referenceId provided" in {
      view.getElementsByClass(gdsPageBodyText).get(0) must containMessage("error.detail1")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages, Flash(Map.empty))
    page.render(request, messages, Flash(Map.empty))
  }

}
