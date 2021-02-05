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

package base.unit

import base.Injector
import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import spec.ViewMatchers

class UnitViewSpec extends AnyWordSpec with ViewMatchers with Injector with GuiceOneAppPerSuite {

  import utils.FakeRequestCSRFSupport._

  implicit val request: Request[AnyContent] = FakeRequest().withCSRFToken

  protected implicit def messages(implicit request: Request[_]): Messages =
    realMessagesApi.preferred(request)

  protected def messages(key: String, args: Any*)(implicit request: Request[_]): String =
    messages(request)(key, args: _*)

  val realMessagesApi = UnitViewSpec.realMessagesApi

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder().build()
  }

}

object UnitViewSpec extends Injector {
  val realMessagesApi: MessagesApi = instanceOf[MessagesApi]
}
