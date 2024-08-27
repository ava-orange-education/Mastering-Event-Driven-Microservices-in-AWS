aws kinesis put-record \
    --stream-name OrderingSystemClickstream \
    --data  "{\"session\": \"S4321\", \"pageUrl\": \"example.com\"}" \
    --partition-key 123 \
    --cli-binary-format raw-in-base64-out \
    --region eu-central-1

aws dynamodb scan --table-name clickstreams --region eu-central-1
