aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/test
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name test

aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/bid-response
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name bid-response

aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/win
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name win

aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/loss
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name loss

aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/currency-rate
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name currency-rate

aws --endpoint-url=http://localhost:4566 sns create-topic --name directives

aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/directives
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name directives

aws --endpoint-url=http://localhost:4566 sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:directives --protocol sqs --notification-endpoint http://localhost:4566/000000000000/directives
