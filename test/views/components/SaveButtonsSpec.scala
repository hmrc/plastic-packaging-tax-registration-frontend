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

package views.components

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import views.html.components.{saveAndContinue, saveButtons}

class SaveButtonsSpec extends UnitViewSpec with Matchers {

  private val saveAndContinueButton = inject[saveAndContinue]

  protected val component: saveButtons =
    new saveButtons(saveAndContinueButton)

  "Save Buttons Component" should {
    "render only the save button" in {
      val view = component()(messages)

      val buttons = view.select("button")
      buttons.size() mustBe 1
      buttons.get(0).text() mustBe messages("site.button.saveAndContinue")
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    component.f("site.button.saveAndContinue")
    component.render("site.button.saveAndContinue", messages)
  }

}
