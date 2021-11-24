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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import com.google.inject.Singleton
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Country

import scala.collection.immutable.ListMap

case class FcoCountry(name: String)

object FcoCountry {
  implicit val format: OFormat[FcoCountry] = Json.format[FcoCountry]
}

@Singleton
class CountryService {

  implicit val countryOrdering: Ordering[Country] = Ordering.by(_.name)

  val countries = parseCountriesResource()

  def getName(code: String) = countries(code).name

  def getAll() = countries

  private def parseCountriesResource() = {

    val permittedCountryCodes = Json.parse(
      getClass.getResourceAsStream("/resources/permitted-country-codes.json")
    ).as[Seq[String]]

    val countryMap = Json.parse(getClass.getResourceAsStream("/resources/countriesEN.json")).as[Map[
      String,
      FcoCountry
    ]]
      .map { entry =>
        entry._1 -> Country(code = entry._1, name = entry._2.name)
      }
      .filter(entry => permittedCountryCodes.contains(entry._1))

    ListMap(countryMap.toSeq.sortBy(_._2): _*)
  }

}
