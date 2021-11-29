
# Plastic Packaging Tax Registration (PPT) Frontend

This is the Scala microservice responsible for the PPT registration UI user journey, which is part of the PPT tax regime, as discussed in this [GovUk Guidance](https://www.gov.uk/government/publications/introduction-of-plastic-packaging-tax/plastic-packaging-tax)
 
This service integrates with the Generic Registration Service and Tax Enrolments.

### How to run the service

* Start a MongoDB instance

* Start the microservices
 
```
# Start the plastic packaging services and dependencies for company and registered society organisation types - 
sm --start PLASTIC_PACKAGING_TAX_ALL INCORPORATED_ENTITY_IDENTIFICATION_ALL EMAIL_VERIFICATION_ALL TAX_ENROLMENTS_ALL ADDRESS_LOOKUP_SERVICES -r

# For the above organisation types along with partnerships and sole traders use these profiles -
sm --start PLASTIC_PACKAGING_TAX_ALL INCORPORATED_ENTITY_IDENTIFICATION_ALL SOLE_TRADER_IDENTIFICATION_ALL PARTNERSHIP_IDENTIFICATION_ALL EMAIL_VERIFICATION_ALL TAX_ENROLMENTS_ALL ADDRESS_LOOKUP_SERVICES -r

# confirm all services are running
sm -s 
```

* Run the microservice locally

```
# Stop the microservice in service manager 
sm --stop PLASTIC_PACKAGING_TAX_REGISTRATION_FRONTEND

# Run the microservice using sbt  (script run_local-sh)
sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run


```

### Login/Access

* Visit the HMRC [Generic Registration service feature switch config page](http://localhost:9718/identify-your-incorporated-business/test-only/feature-switches) and
select the appropriate stubbing behaviour _(select all)_. 
* Visit http://localhost:9949/auth-login-stub/gg-sign-in
* Enter the redirect url: http://localhost:8503/plastic-packaging-tax/start
* Choose affinity group as `Organisation`
* Enter the email `test.preLaunch@ppt.test` or `test.postLaunch@ppt.test`
* Enter `Submit`

### User Journey autocompletion scripts

In order to allow any stakeholder to submit a user journey on any HMRC environment, without having to manually enter any information, we rely on [TamperMonkey](https://www.tampermonkey.net/).
Install it as an extension to Google Chrome of Mozilla Firefox and then copy and install the following scripts:
 * [Plastic Packaging Tax (PPT) Autocomplete](https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-registration-frontend/master/tampermonkey/PPT_AutoComplete.js) 
 * [Plastic Packaging Tax (PPT) Auth Autocomplete](https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-registration-frontend/master/tampermonkey/PPT_Auth_AutoComplete.js) 


### Precheck

Before submitting a commit or pushing code remotely, please run  
```
./precheck.sh
```
This will execute unit and integration tests, check the Scala style and code coverage

### Scalastyle

Project contains `scalafmt` plugin.

Commands for code formatting:

```
sbt scalafmt        # format compile sources
sbt test:scalafmt   # format test sources
sbt sbt:scalafmt    # format .sbt source
```

### Country Lookup

On address entry pages we use a country lookup widget the contents of which is controlled by the file
```
conf/resources/countriesEN.json
```
A copy of this file was taken from https://github.com/hmrc/address-lookup-frontend/blob/main/conf/countriesEN.json to 
ensure that countries which can be selected are consistent with these offered by the Address Lookup Frontend.

#### Auto-completion (and aliases)

In order to improve the user experience when entering a country we use a Javascript powered country auto-completion facility
(see https://github.com/alphagov/govuk-country-and-territory-autocomplete). This uses another configuration file which
defines aliases for countries (see https://github.com/alphagov/govuk-country-and-territory-autocomplete/blob/master/dist/location-autocomplete-graph.json)

In order for this to work correctly and only offer country suggestions which actually exist in the associated country
"select" element, the configuration file must be consistent with the list of countries which are supported.

We have created a shell script (which uses Scala Ammonite under the covers) to perform preparation of a suitable
alias config file based on the contents of the country lookup config file (see above).

Download the following file to the local `/utils` subdirectory:

```
https://github.com/alphagov/govuk-country-and-territory-autocomplete/blob/master/dist/location-autocomplete-graph.json
```

Run the following from the `/utils` subdirectory in order to generate a compatible alias config file. Please note that 
this command will fail if the target file (the 3rd parameter) already exists.

```
./filter-country-lookup-config.sh ../conf/resources/countriesEN.json location-autocomplete-graph.json ../app/assets/json/location-autocomplete-graph.json
```

You will need Scala development tools installed incl. Ammonite Scala scripting in order for this to run. See
https://docs.scala-lang.org/getting-started/index.html

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

