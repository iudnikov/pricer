#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/test
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name test

#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/bid-response
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name bid-response

#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/win
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name win

#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/loss
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name loss

#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/currency-rate
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name currency-rate

aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name currency-rate
aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url http://localhost:4566/000000000000/currency-rate --message-body '{
  "Type": "Notification",
  "MessageId": "2945a4e3-5b42-56e4-bb23-fd7d55f45694",
  "TopicArn": "arn:aws:sns:eu-west-1:021858044175:dooh-dsp-currency-rate",
  "Message": "{\"date\":\"2021-08-24\",\"rates\":[{\"currency\":\"GBP/AUD\",\"rate\":1.9026},{\"currency\":\"AED/SAR\",\"rate\":1.02102},{\"currency\":\"CAD/GBP\",\"rate\":0.57584},{\"currency\":\"EUR/SAR\",\"rate\":4.4049},{\"currency\":\"JOD/AUD\",\"rate\":1.95514},{\"currency\":\"USD/CAD\",\"rate\":1.2652},{\"currency\":\"GBP/JOD\",\"rate\":0.9731},{\"currency\":\"GBP/AED\",\"rate\":5.04133},{\"currency\":\"GBP/EUR\",\"rate\":1.1685},{\"currency\":\"USD/SAR\",\"rate\":3.7503},{\"currency\":\"AUD/USD\",\"rate\":0.7214},{\"currency\":\"GBP/CAD\",\"rate\":1.7366},{\"currency\":\"JOD/AED\",\"rate\":5.18068},{\"currency\":\"SAR/AUD\",\"rate\":0.36962},{\"currency\":\"AED/EUR\",\"rate\":0.23178},{\"currency\":\"SAR/CAD\",\"rate\":0.33736},{\"currency\":\"CAD/SAR\",\"rate\":2.9642},{\"currency\":\"JOD/EUR\",\"rate\":1.20076},{\"currency\":\"AED/JOD\",\"rate\":0.19302},{\"currency\":\"AED/CAD\",\"rate\":0.34445},{\"currency\":\"AUD/GBP\",\"rate\":0.5256},{\"currency\":\"JOD/CAD\",\"rate\":1.78449},{\"currency\":\"EUR/CAD\",\"rate\":1.4861},{\"currency\":\"SAR/EUR\",\"rate\":0.22702},{\"currency\":\"SAR/JOD\",\"rate\":0.18905},{\"currency\":\"SAR/AED\",\"rate\":0.97941},{\"currency\":\"EUR/AED\",\"rate\":4.3145},{\"currency\":\"EUR/JOD\",\"rate\":0.83281},{\"currency\":\"AED/AUD\",\"rate\":0.37739},{\"currency\":\"GBP/SAR\",\"rate\":5.1475},{\"currency\":\"AUD/SAR\",\"rate\":2.70547},{\"currency\":\"USD/GBP\",\"rate\":0.7286},{\"currency\":\"EUR/AUD\",\"rate\":1.6282},{\"currency\":\"CAD/EUR\",\"rate\":0.6729},{\"currency\":\"JOD/SAR\",\"rate\":5.28956},{\"currency\":\"CAD/JOD\",\"rate\":0.56039},{\"currency\":\"CAD/AED\",\"rate\":2.90318},{\"currency\":\"AUD/EUR\",\"rate\":0.61418},{\"currency\":\"JOD/GBP\",\"rate\":1.02764},{\"currency\":\"SAR/GBP\",\"rate\":0.19427},{\"currency\":\"AUD/AED\",\"rate\":2.64977},{\"currency\":\"AUD/JOD\",\"rate\":0.51147},{\"currency\":\"AUD/CAD\",\"rate\":0.9127},{\"currency\":\"GBP/USD\",\"rate\":1.3725},{\"currency\":\"CAD/AUD\",\"rate\":1.09565},{\"currency\":\"SAR/USD\",\"rate\":0.26665},{\"currency\":\"AED/USD\",\"rate\":0.27225},{\"currency\":\"JOD/USD\",\"rate\":1.41044},{\"currency\":\"USD/AUD\",\"rate\":1.38619},{\"currency\":\"EUR/USD\",\"rate\":1.17462},{\"currency\":\"USD/EUR\",\"rate\":0.85134},{\"currency\":\"AED/GBP\",\"rate\":0.19836},{\"currency\":\"USD/JOD\",\"rate\":0.709},{\"currency\":\"USD/AED\",\"rate\":3.6731},{\"currency\":\"EUR/GBP\",\"rate\":0.8558},{\"currency\":\"CAD/USD\",\"rate\":0.79039}]}",
  "Timestamp": "2021-08-23T21:00:38.059Z",
  "SignatureVersion": "1",
  "Signature": "Cmlh4ZWV/wPnQx4iDBjqxtRaT3CaPpY7EPNDdLN0jlY8Es1QOHmn2h6JyX/bYS4J1uqSjWcw9Zup0FkpkNVRpa9usFLhslErsanZgt4fhfso2r+N7SnnfV9UWfPybxNsPB4b43iHcfJ1Qn1IJwTpIPQnE0CGbPXi5+uBncneF0z7mE7F9Nqa9iAKPpaHsReOlz/B1CzhRkf9cQ3DUY74CYRriHHgSGXI7whueizM+DkxzSnOoA0xHWSKPvAvesGBw5hH+m5SyE1LUaAM4+JboOpPShP52Ug2LaWUj2158fKgEppjImQP6IeSd264Pk/iQggx3DCFig6HWdLLU6FyDg==",
  "SigningCertURL": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem",
  "UnsubscribeURL": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:021858044175:dooh-dsp-currency-rate:12c23700-1d5e-4167-bf92-30981a0b5ac2"
}'

aws --endpoint-url=http://localhost:4566 sns create-topic --name directives

#aws --endpoint-url=http://localhost:4566 sqs delete-queue --queue-url http://localhost:4566/000000000000/directives
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name directives

aws --endpoint-url=http://localhost:4566 sns subscribe --topic-arn arn:aws:sns:us-east-1:000000000000:directives --protocol sqs --notification-endpoint http://localhost:4566/000000000000/directives
