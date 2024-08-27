export ACCOUNT_ID=$(aws sts get-caller-identity --output text --query Account)
cd infrastructure
cdk bootstrap
cdk deploy --outputs-file target/output.json