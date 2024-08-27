export API_GW_URL=$(aws cloudformation describe-stacks --stack-name CdkExampleStack --region eu-central-1 | jq -r '.Stacks[0].Outputs[] | select(.OutputKey == "ApiGatewayOrderingPlarformApiEndpoint").OutputValue')

curl -X POST $API_GW_URL'/orders' \
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

aws dynamodb scan --table-name orders --region eu-central-1
aws dynamodb scan --table-name products-inventory --region eu-central-1