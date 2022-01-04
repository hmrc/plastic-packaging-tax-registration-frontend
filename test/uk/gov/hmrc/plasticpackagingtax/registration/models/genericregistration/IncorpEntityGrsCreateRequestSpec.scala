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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class IncorpEntityGrsCreateRequestSpec extends AnyWordSpec with Matchers {

  "IncorpEntityGrsCreateRequestSpec" should {
    "Convert to and from json as expected" in {
      val grsCreateJourneyRequest = IncorpEntityGrsCreateRequest(continueUrl = "a",
                                                                 optServiceName = Some("b"),
                                                                 deskProServiceId = "c",
                                                                 signOutUrl = "d"
      )

      val json = Json.toJson(grsCreateJourneyRequest)

      (json \ "continueUrl").as[String] mustBe "a"
      (json \ "optServiceName").as[String] mustBe "b"
      (json \ "deskProServiceId").as[String] mustBe "c"
      (json \ "signOutUrl").as[String] mustBe "d"
      (json \ "regime").as[String] mustBe "PPT"
      (json \ "businessVerificationCheck").as[Boolean] mustBe true
    }

    "Convert to and from json with businessVerificationCheck as false" in {
      val grsCreateJourneyRequest = IncorpEntityGrsCreateRequest(continueUrl = "a",
                                                                 optServiceName = Some("b"),
                                                                 deskProServiceId = "c",
                                                                 signOutUrl = "d",
                                                                 businessVerificationCheck = false
      )

      val json = Json.toJson(grsCreateJourneyRequest)

      (json \ "continueUrl").as[String] mustBe "a"
      (json \ "optServiceName").as[String] mustBe "b"
      (json \ "deskProServiceId").as[String] mustBe "c"
      (json \ "signOutUrl").as[String] mustBe "d"
      (json \ "regime").as[String] mustBe "PPT"
      (json \ "businessVerificationCheck").as[Boolean] mustBe false
    }
  }
}
