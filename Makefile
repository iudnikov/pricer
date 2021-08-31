AWS_PROFILE ?= neuron-dev-prod
CODEARTIFACT_TOKEN = $(shell aws --profile $(AWS_PROFILE) codeartifact get-authorization-token \
--region eu-west-1 --domain dooh --domain-owner 021858044175 --query authorizationToken --output text)

.PHONY: build
build:
	CODEARTIFACT_TOKEN=$(CODEARTIFACT_TOKEN) ./mvnw package

.PHONY: clean
clean:
	./mvnw clean

.PHONY: integration-test
integration-test:
	./mvnw integration-test -P integration-testing

.PHONY: copy-m2-settings
copy-m2-settings:
	cp m2.settings.xml ~/.m2/settings.xml

.PHONY: start
start:
	$(MAKE) copy-m2-settings clean build run

.PHONY: run
run:
	java -jar -Dspring.profiles.active=local ./target/pricer.jar
