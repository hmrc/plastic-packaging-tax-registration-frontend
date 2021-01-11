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

package spec

import base.Injector
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest

class UnitViewSpec extends AnyWordSpec with ViewMatchers with Injector {

  import utils.FakeRequestCSRFSupport._

  implicit val request: Request[AnyContent] = FakeRequest().withCSRFToken

  protected implicit def messages(implicit request: Request[_]): Messages =
    realMessagesApi.preferred(request)

  protected def messages(key: String, args: Any*)(implicit request: Request[_]): String =
    messages(request)(key, args: _*)

  val realMessagesApi = UnitViewSpec.realMessagesApi
}

object UnitViewSpec extends Injector {
  val realMessagesApi: MessagesApi = instanceOf[MessagesApi]
}
