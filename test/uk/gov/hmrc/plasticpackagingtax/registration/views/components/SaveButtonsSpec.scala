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

package uk.gov.hmrc.plasticpackagingtax.registration.views.components

import base.unit.UnitViewSpec
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features.isUkCompanyPrivateBeta
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.components.{
  saveAndComeBackLater,
  saveAndContinue,
  saveButtons
}

class SaveButtonsSpec extends UnitViewSpec with Matchers {

  private val saveAndContinueButton      = instanceOf[saveAndContinue]
  private val saveAndComeBackLaterButton = instanceOf[saveAndComeBackLater]
  private val mockConfig                 = mock[AppConfig]

  protected val component: saveButtons =
    new saveButtons(saveAndContinueButton, saveAndComeBackLaterButton, mockConfig)

  "Save Buttons Component" should {
    "render both buttons" when {
      "outside of private BETA" in {
        when(mockConfig.isDefaultFeatureFlagEnabled(isUkCompanyPrivateBeta)).thenReturn(false)
        val view = component()(messages)

        val buttons = view.select("button")
        buttons.size() mustBe 2
        buttons.get(0).text() mustBe messages("site.button.saveAndContinue")
        buttons.get(1).text() mustBe messages("site.button.saveAndComeBackLater")
      }
    }

    "render only the save button" when {
      "during private BETA" in {
        when(mockConfig.isDefaultFeatureFlagEnabled(isUkCompanyPrivateBeta)).thenReturn(true)
        val view = component()(messages)

        val buttons = view.select("button")
        buttons.size() mustBe 1
        buttons.get(0).text() mustBe messages("site.button.saveAndContinue")
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    component.f("site.button.saveAndContinue", "site.button.saveAndComeBackLater")
    component.render("site.button.saveAndContinue", "site.button.saveAndComeBackLater", messages)
  }

}
