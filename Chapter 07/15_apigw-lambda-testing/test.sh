# Test Lambda function using SAM

sam local generate-event apigateway aws-proxy \
  --body '{
    "customerId": "123",
    "items": ["item1", "item2", "item3"],
    "total": "74.5",
    "paymentDetails": {
        "paymentMethod": "CARD",
        "paymentTransactionId": "FDS232SGFDF341934"
    },
    "shippingDetails": {
        "receiverName": "John Doe",
        "receiverPhoneNumber": "123-456-7890",
        "shippingAddressLine1": "123 Main St",
        "shippingAddressLine2": "Apt 101",
        "shippingCity": "Anytown",
        "shippingState": "CA",
        "shippingZipCode": "12345",
        "shippingCountry": "USA"
    }
}' > event.json 

sam local invoke Lambda_NewOrderService -t CdkExampleStack.template.json -e event.json

# Test API Gateway using SAM

sam local start-api -t CdkExampleStack.template.json

curl -X POST http://127.0.0.1:3000/orders \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "customerId": "123",
    "items": ["item1", "item2", "item3"],
    "total": "74.5",
    "paymentDetails": {
        "paymentMethod": "CARD",
        "paymentTransactionId": "FDS232SGFDF341934"
    },
    "shippingDetails": {
        "receiverName": "John Doe",
        "receiverPhoneNumber": "123-456-7890",
        "shippingAddressLine1": "123 Main St",
        "shippingAddressLine2": "Apt 101",
        "shippingCity": "Anytown",
        "shippingState": "CA",
        "shippingZipCode": "12345",
        "shippingCountry": "USA"
    }
}'

# Test Step Function using Step Function Local
aws stepfunctions --endpoint http://127.0.0.1:8083 create-state-machine \
  --definition file://stepfunctions_state_machine.json \
  --name OrderingAPI-StateMachine \
  --role-arn arn:aws:iam::012345678901:role/DummyRole


aws stepfunctions --endpoint http://127.0.0.1:8083 start-execution \
  --state-machine-arn <STATE_MACHINE_ARN> \
  --input '{
   "NewOrderRequest":{
      "customerId":"123",
      "items":[
         "item1",
         "item2",
         "item3"
      ],
      "total":"74.5",
      "paymentDetails":{
         "paymentMethod":"CARD",
         "paymentTransactionId":"FDS232SGFDF341934"
      },
      "shippingDetails":{
         "receiverName":"John Doe",
         "receiverPhoneNumber":"123-456-7890",
         "shippingAddressLine1":"123 Main St",
         "shippingAddressLine2":"Apt 101",
         "shippingCity":"Anytown",
         "shippingState":"CA",
         "shippingZipCode":"12345",
         "shippingCountry":"USA"
      }
   }
}'

aws stepfunctions --endpoint http://127.0.0.1:8083 describe-execution \
  --execution-arn <EXECUTION_ARN>
  
# Load testing using Artillery
export API_GW_URL=$(aws cloudformation describe-stacks --stack-name CdkExampleStack --region eu-central-1 | jq -r '.Stacks[0].Outputs[] | select(.OutputKey == "ApiGatewayOrderingPlarformApiEndpoint").OutputValue')

artillery run -t $API_GW_URL artillery.yml