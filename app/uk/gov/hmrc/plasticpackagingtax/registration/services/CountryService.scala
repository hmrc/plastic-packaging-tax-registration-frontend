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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, OFormat, __}

import scala.collection.immutable.ListMap

case class FcoCountry(name: String)

object FcoCountry {
  implicit val format: OFormat[FcoCountry] = Json.format[FcoCountry]
}

@Singleton
class CountryService {
  private val logger = LoggerFactory.getLogger("application." + getClass.getCanonicalName)

  val countries = parseCountriesResource()

  def getName(code: String): String = countries(code)

  def getAll(): Map[String, String] = countries

  def getKeyForName(countryName: String): Option[String] = {
    val allCountries = getAll()

    val countryCode = allCountries.map(_.swap).get(countryName)

    countryCode.orElse{
      logger.warn(s"Failed to identify country code for [$countryName]")
      None
    }
  }

  private def parseCountriesResource(): Map[String, String] = {

    val countryMap = Json.parse(getClass.getResourceAsStream("/resources/countriesEN.json")).as[Map[String, FcoCountry]]
      .map { entry =>
        entry._1 -> entry._2.name
      }

    ListMap(countryMap.toSeq.sortBy(_._2): _*)
  }

}
