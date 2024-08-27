export API_GW_URL=$(aws cloudformation describe-stacks --stack-name CdkExampleStack --region eu-central-1 | jq -r '.Stacks[0].Outputs[] | select(.OutputKey == "ApiGatewayOrderingPlarformApiEndpoint").OutputValue')

curl -X POST $API_GW_URL'/orders' \
  --header 'Content-Type: application/json' \
  --header 'X-Idempotency-Key: 5fc03087-d265-11e7-b8c6-83e29cd24f4c' \
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
