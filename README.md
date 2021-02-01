
# Plastic Packaging Tax Registration (PPT) Frontend

This is the Scala microservice responsible for the PPT registration UI user journey, which is part of the PPT tax regime, as discussed in this [GovUk Guindance](https://www.gov.uk/government/publications/introduction-of-plastic-packaging-tax/plastic-packaging-tax)
 
This service integrates with the HMRC Strategic Generic Registration service, namely: 
 * [Incorporated Entity Identification Frontend](https://github.com/hmrc/incorporated-entity-identification-frontend)

### How to run the service

* Start a MongoDB instance

* Start the microservices
 
```
# Start the plastic packaging services and dependencies 
sm --start PLASTIC_PACKAGING_TAX_ALL -f

# start the incorporated Entity Identification Frontend and dependencies
sm --start INCORPORATED_ENTITY_IDENTIFICATION_ALL

# confirm all services are running
sm -s 
```

* Visit http://localhost:9949/auth-login-stub/gg-sign-in
* Enter the redirect url: http://localhost:8503/plastic-packaging-tax/start and press **Submit**.

### Login

Single enrolment is required:
    
    EnrolmentKey: HMRC-CUS-ORG
    IdentifierName: UTR
    Value: any String/Int (if you pass same it'll pull out existing registration, otherwise create new one)

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

