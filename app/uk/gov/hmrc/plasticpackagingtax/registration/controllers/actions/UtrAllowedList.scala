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

import com.google.inject.{Inject, ProvidedBy}
import play.api.Configuration

import javax.inject.Provider

@ProvidedBy(classOf[UtrAllowedListProvider])
class UtrAllowedList(values: Seq[String]) {
  def isAllowed(utr: String): Boolean = values.isEmpty || values.contains(utr)
}

class UtrAllowedListProvider @Inject() (configuration: Configuration)
    extends Provider[UtrAllowedList] {

  override def get(): UtrAllowedList =
    new UtrAllowedList(configuration.get[Seq[String]]("allowedList.utr").map(_.trim))

}
