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

package uk.gov.hmrc.plasticpackagingtax.registration.views.models

import play.api.mvc.Call

sealed trait BackButtonLink {
  val title: String
  val hiddenText: String
  val call: Call
}

case class BackButton(override val title: String, override val call: Call, override val hiddenText: String) extends BackButtonLink

case class BackButtonJs(override val title: String, override val hiddenText: String) extends BackButtonLink {
  override val call: Call = Call("GET", "javascript:window.history.back()")
}
