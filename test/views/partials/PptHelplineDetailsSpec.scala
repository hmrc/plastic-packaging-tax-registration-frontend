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

package views.partials

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import views.html.partials.pptHelplineDetails

class PptHelplineDetailsSpec extends UnitViewSpec with Matchers {

  private val pptHelplinePartial = inject[pptHelplineDetails]

  private def pageParagraphMessageKeys =
    List("pptHelpline.intro",
         "pptHelpline.telephone.title",
         "pptHelpline.telephone.detail",
         "pptHelpline.textphone.title",
         "pptHelpline.textphone.detail",
         "pptHelpline.telephone.outsideUK.title",
         "pptHelpline.telephone.outsideUK.detail",
         "pptHelpline.openingTimes.title",
         "pptHelpline.openingTimes.detail.1",
         "pptHelpline.openingTimes.detail.2"
    )

  "PPT Helpline partial" should {
    val partial = pptHelplinePartial.apply()

    "display PPT helpline info" in {
      pageParagraphMessageKeys.foreach { messageKey =>
        partial.select("p.govuk-body").text must include(messages(messageKey))
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    pptHelplinePartial.f()
    pptHelplinePartial.render(request, messages)
  }

}
