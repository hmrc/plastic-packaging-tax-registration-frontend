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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class UtrWhitelistSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "utr allow whitelist" when {
    "is empty" should {
      "allow everyone" in {
        val utrWhiteList = new UtrWhitelist(Seq.empty)
        utrWhiteList.isAllowed("12345") mustBe true
        utrWhiteList.isAllowed("0987") mustBe true
      }
    }
    "has elements" should {
      val utrWhiteList = new UtrWhitelist(Seq("12345"))
      "allow listed utr" in {
        utrWhiteList.isAllowed("12345") mustBe true
      }
      "disallow not listed utr" in {
        utrWhiteList.isAllowed("0987") mustBe false
      }
    }
  }
}
