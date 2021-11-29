#!/usr/bin/env amm

@main
def main(supportedCountriesConfigFile: String, countryLookupAlisConfigFile: String, outputFile: String): Unit = {

  val supportedCountryJson = os.read(os.pwd/os.RelPath(supportedCountriesConfigFile))
  val supportedCountryCodes = ujson.read(supportedCountryJson).obj.keys.toSet
  println(s"Supported country codes = $supportedCountryCodes")

  val countryAliasConfigJson = os.read(os.pwd/os.RelPath(countryLookupAlisConfigFile))
  val countryAliasConfig = ujson.read(countryAliasConfigJson)
  val countryAliasMap = countryAliasConfig.obj.keys.map { key => key -> countryAliasConfig(key) }.toMap

  def getEntryType(str: String) = str.split(":")

  def supportedEntry(id: String, targets: Set[String], enabledCountries: Set[String]): Boolean = {
    val nodeType: String = getEntryType(id)(0)
    val nodeVal: String = getEntryType(id)(1)

    val targetCountries = targets.filter { t => getEntryType(t)(0) == "country" }.map { t => getEntryType(t)(1) }

    val supported = nodeType match {
      case "country" => enabledCountries.contains(nodeVal)
      case _ => (targetCountries & enabledCountries).nonEmpty
    }

    if (!supported) {
      println(s"Stripping alias: $id")
    }

    supported
  }

  def extractTargets(countryDef: ujson.Value) = countryDef("edges")("from").arr.map { v => v.str }.toSet

  val filteredCountryAliasMap = countryAliasMap.filter { entry => supportedEntry(entry._1, extractTargets(entry._2), supportedCountryCodes) }

  os.write(os.pwd/os.RelPath(outputFile), ujson.write(filteredCountryAliasMap))
}
