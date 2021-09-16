
# Plastic Packaging Tax Registration (PPT) Frontend

This is the Scala microservice responsible for the PPT registration UI user journey, which is part of the PPT tax regime, as discussed in this [GovUk Guidance](https://www.gov.uk/government/publications/introduction-of-plastic-packaging-tax/plastic-packaging-tax)
 
This service integrates with the HMRC Strategic Generic Registration service, namely: 
 * [Incorporated Entity Identification Frontend](https://github.com/hmrc/incorporated-entity-identification-frontend)

### How to run the service

* Start a MongoDB instance

* Start the microservices
 
```
# Start the plastic packaging services and dependencies 
sm --start PLASTIC_PACKAGING_TAX_ALL INCORPORATED_ENTITY_IDENTIFICATION_ALL SOLE_TRADER_IDENTIFICATION_ALL PARTNERSHIP_IDENTIFICATION_ALL EMAIL_VERIFICATION_ALL -r

# confirm all services are running
sm -s 
```

* Run the microservice locally

```
# Stop the microservice in service manager 
sm --stop PLASTIC_PACKAGING_TAX_REGISTRATION_FRONTEND

# Run the microservice using sbt
sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run
```

### Login/Access

* Visit the HMRC [Generic Registration service feature switch config page](http://localhost:9718/identify-your-incorporated-business/test-only/feature-switches) and
select the appropriate stubbing behaviour _(select all)_. 
* Visit http://localhost:9949/auth-login-stub/gg-sign-in
* Enter the redirect url: http://localhost:8503/plastic-packaging-tax/start-plastic-packaging-tax-registration.
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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

