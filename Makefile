STACK_NAME := aws-lambda-recipes-api-micronaut

configure-recipes-import-trigger:
	$(eval FUNCTION_NAME := $(shell aws cloudformation describe-stack-resource --stack-name $(STACK_NAME) --logical-resource-id ImportRecipesFunction --query "StackResourceDetail.PhysicalResourceId" --output text))
	$(eval LAMBDA_ARN := $(shell aws lambda get-function-configuration --function-name $(FUNCTION_NAME) --query FunctionArn --output text))
	$(eval BUCKET := $(shell aws cloudformation describe-stack-resource --stack-name $(STACK_NAME) --logical-resource-id RecipesImportBucket --query "StackResourceDetail.PhysicalResourceId" --output text))
	aws s3api put-bucket-notification-configuration \
		--bucket $(BUCKET) \
		--notification-configuration '{"LambdaFunctionConfigurations":[{"LambdaFunctionArn":"$(LAMBDA_ARN)","Events":["s3:ObjectCreated:*"]}]}'

ifeq ($(OS),Windows_NT)
build-ListRecipesFunction build-GetRecipeByIdFunction build-CreateRecipeFunction build-ImportRecipesFunction:
	mvn clean package -DskipTests
	cmd /c if not exist "$(ARTIFACTS_DIR)\lib" mkdir "$(ARTIFACTS_DIR)\lib"
	cmd /c copy /Y recipes-api\target\recipes-api-rest-1.0.0.jar "$(ARTIFACTS_DIR)\lib"
else
build-ListRecipesFunction build-GetRecipeByIdFunction build-CreateRecipeFunction build-ImportRecipesFunction:
	mvn clean package -DskipTests
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp recipes-api/target/recipes-api-rest-1.0.0.jar $(ARTIFACTS_DIR)/lib/
endif
