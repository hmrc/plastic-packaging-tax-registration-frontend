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
import play.api.mvc.Flash
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class EnrolmentFailureViewSpec extends UnitViewSpec with Matchers {

  private val page: enrolment_failure_page =
    instanceOf[enrolment_failure_page]

  private val flash = Flash(Map(FlashKeys.referenceId -> "PPT123"))

  private def createView(): Html =
    page()(journeyRequest, messages, flash)

  "Enrolment Failure Page" should {
    val view: Html = createView()

    "display title" in {
      view.getElementsByClass(gdsPageHeading).first() must containMessage("enrolment.failure.title")
    }

    "display detail" in {
      val pptReferenceText = view.getElementsByClass(gdsPageBodyText)
      pptReferenceText.get(0) must containMessage("enrolment.failure.detail.1")
      pptReferenceText.get(1) must containMessage("enrolment.failure.detail.2",
                                                  "PPT123",
                                                  messages("enrolment.failure.detail.2.text")
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages, flash)
    page.render(request, messages, flash)
  }

}
