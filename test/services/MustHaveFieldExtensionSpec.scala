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

package services

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import services.MustHaveFieldExtension.ExtendedObject

class MustHaveFieldExtensionSpec extends PlaySpec with BeforeAndAfterEach {

  case class Example(x: Option[String])

  "mustHave should return the field contents" in {
    val example = Example(Some("the contents of x"))
    example.mustHave(_.x, "x") mustBe "the contents of x"
  }

  "mustHave should complain about an empty field" in {
    val example = Example(None)
    the [IllegalStateException] thrownBy example.mustHave(_.x, "x") must have message "Missing field 'x'"
  }

  "mightHaveIf should ignore an empty field" in {
    val example = Example(None)
    noException should be thrownBy example.mightHaveIf(_.x, "x", predicate = false)
  }

  "mightHaveIf should complain about an empty field" in {
    val example = Example(None)
    the [IllegalStateException] thrownBy example.mightHaveIf(_.x, "x", predicate = true) must have message "Missing field 'x'"
  }

  "mightHaveIf should be happy with a defined field" in {
    val example = Example(Some("the contents of x"))
    noException should be thrownBy example.mightHaveIf(_.x, "x", predicate = true)
  }
}


