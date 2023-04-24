/*
 * Copyright 2023 HM Revenue & Customs
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

package base.unit

import models.request.JourneyRequest
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import spec.{PptTestData, ViewMatchers}

abstract class UnitViewSpec
    extends MessagesSpec with MockRegistrationConnector with MockitoSugar with ViewMatchers with ViewAssertions with PptTestData {

  protected val emptyFormData: Map[String, String]   = Map.empty

  implicit val req: JourneyRequest[AnyContent] = registrationJourneyRequest

  "Exercise generated rendering methods" in {
    exerciseGeneratedRenderingMethods()
  }

  def exerciseGeneratedRenderingMethods(): Unit

}
