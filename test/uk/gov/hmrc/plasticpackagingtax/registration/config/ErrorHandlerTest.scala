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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import org.scalatest.matchers.must.Matchers
import play.api.test.Helpers.stubMessagesApi
import spec.UnitViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.error_template

class ErrorHandlerTest extends UnitViewSpec with Matchers {

  private val errorPage    = instanceOf[error_template]
  private val appConfig    = app.injector.instanceOf[AppConfig]
  private val errorHandler = new ErrorHandler(errorPage, stubMessagesApi())(appConfig)

  "ErrorHandlerSpec" should {

    "standardErrorTemplate" in {

      val result = errorHandler.standardErrorTemplate("title", "heading", "message")(request).body

      result must include("title")
      result must include("heading")
      result must include("message")
    }
  }
}
