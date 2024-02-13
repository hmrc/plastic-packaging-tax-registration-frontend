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

package config

import base.unit.MessagesSpec
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.stubMessagesApi
import spec.PptTestData
import views.html.error_template

class ErrorHandlerTest extends MessagesSpec with Matchers with DefaultAwaitTimeout with OptionValues with PptTestData {

  private val errorPage    = inject[error_template]
  private val errorHandler = new ErrorHandler(errorPage, stubMessagesApi())

  "ErrorHandlerSpec" should {

    "standardErrorTemplate" in {

      val result =
        errorHandler.standardErrorTemplate("title", "heading", "message")(registrationJourneyRequest).body

      result must include("title")
      result must include("heading")
      result must include("message")
    }

  }

}
