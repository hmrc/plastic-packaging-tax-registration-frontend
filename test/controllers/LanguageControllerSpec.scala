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

package controllers

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

import base.unit.ControllerSpec
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import config.AppConfig

class LanguageControllerSpec extends ControllerSpec with Injecting {

  lazy val controller = inject[LanguageController]

  "Calling LanguageController.enGb" should {
    "change the language to English and return 303" in {
      val result = controller.enGb()(request)
      cookies(result).get("PLAY_LANG").get.value shouldBe "en"
      status(result) shouldBe SEE_OTHER
    }
  }

  "Calling LanguageController.cyGb" should {
    "change the language to Welsh and return 303" in {
      val result = controller.cyGb()(request)
      cookies(result).get("PLAY_LANG").get.value shouldBe "cy"
      status(result) shouldBe SEE_OTHER
    }
  }

  "changeLang" should {
    "redirect" when {
      "redirect URI contains the path" in {
        val request = FakeRequest()
        val result  = controller.cyGb()(request)
        cookies(result).get("PLAY_LANG").get.value shouldBe "cy"
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe inject[AppConfig].pptAccountUrl
      }
    }
  }
}
