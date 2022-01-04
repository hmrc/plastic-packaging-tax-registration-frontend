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

package uk.gov.hmrc.plasticpackagingtax.registration.config

import com.typesafe.config.Config
import play.api.ConfigLoader

import scala.collection.JavaConverters._

case class AllowedUser(email: String, features: Map[String, Boolean] = Map.empty)

object AllowedUser {

  private val email    = "email"
  private val features = "features"

  implicit val configLoader: ConfigLoader[Seq[AllowedUser]] =
    ConfigLoader(_.getConfigList).map(_.asScala.toList.map {
      config =>
        val userEmail: String    = config.getString(email).trim
        val featuresList: Config = config.getConfig(features)
        val userFeatures: Map[String, Boolean] = featuresList.entrySet().asScala.map { entry =>
          entry.getKey -> featuresList.getBoolean(entry.getKey)
        }.toMap

        AllowedUser(userEmail, userFeatures)
    })

}
