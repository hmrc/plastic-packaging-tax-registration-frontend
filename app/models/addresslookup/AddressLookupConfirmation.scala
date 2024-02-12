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

package models.addresslookup

import play.api.libs.json.{Json, OFormat}

case class AddressLookupConfirmation(
  auditRef: String,
  id: Option[String],
  address: AddressLookupAddress
) {

  // modified from the address-lookup-frontend, when it wants to split the address for manual edit
  def extractAddressLines(): (String, Option[String], Option[String], String) = {
    val l1: String         = this.address.lines.headOption.getOrElse("")
    val l2: Option[String] = if (this.address.lines.length > 2) this.address.lines.lift(1) else None
    val l3: Option[String] = if (this.address.lines.length > 3) this.address.lines.lift(2) else None
    val l4: String =
      if (this.address.lines.length > 1) this.address.lines.lastOption.getOrElse("") else ""
    (l1, l2, l3, l4)
  }

}

object AddressLookupConfirmation {
  implicit val format: OFormat[AddressLookupConfirmation] = Json.format[AddressLookupConfirmation]
}
