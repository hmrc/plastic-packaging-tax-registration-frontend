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

import scala.collection.immutable.ListMap

case class FcoCountry(name: String)

object FcoCountry {
  implicit val format: OFormat[FcoCountry] = Json.format[FcoCountry]
}

@Singleton
class CountryService {

  val countries = parseCountriesResource()

  def getName(code: String): String = countries(code)

  def getAll(): Map[String, String] = countries

  private def parseCountriesResource(): Map[String, String] = {

    val countryMap = Json.parse(getClass.getResourceAsStream("/resources/countriesEN.json")).as[Map[
      String,
      FcoCountry
    ]]
      .map { entry =>
        entry._1 -> entry._2.name
      }

    ListMap(countryMap.toSeq.sortBy(_._2): _*)
  }

}
