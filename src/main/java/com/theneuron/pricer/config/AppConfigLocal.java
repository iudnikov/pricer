package com.theneuron.pricer.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.theneuron.pricer.jedis.JedisStatefulClient;
import com.theneuron.pricer.mock.UUIDSupplierQueued;
import com.theneuron.pricer.repo.*;
import com.theneuron.pricer.services.DirectivePublisherSNSImpl;
import com.theneuron.pricer.services.MoneyExchanger;
import com.theneuron.pricer.services.MoneyExchangerImpl;
import com.theneuron.pricer.services.PricerService;
import org.javamoney.moneta.Money;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import javax.jms.JMSException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedList;
import java.util.function.Supplier;

@Configuration
@Profile("local")
public class AppConfigLocal {

    @Bean
    public static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JodaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Bean
    public JedisStatefulClient jedisStatefulClient(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") Integer port,
            @Value("${spring.redis.database}") Integer db
    ) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(32);
        JedisPool pool = new JedisPool(poolConfig, host, port);
        return new JedisStatefulClient(pool, db);
    }

    @Bean
    public SnsClient amazonSNSClientLocal(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey,
            @Value("${aws.region}") String region
    ) {
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"));
        return SnsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.US_WEST_1)
                .build();
    }

    @Bean
    public SQSConnection sqsConnectionLocal(
            @Value("${aws.region}") String region
    ) throws JMSException {
        // aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url http://localhost:4566/000000000000/test --message-body 'Hello world!'
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
                        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:4566", region))
                        .build()
        );
        return connectionFactory.createConnection();
    }

    @Bean
    public MoneyExchanger moneyExchangerLocal(
            CurrencyRateReader reader
    ) {
        return new MoneyExchangerImpl(reader, DateTime::now);
    }


    @Bean
    public UUIDSupplierQueued uuidSupplier() {
        return new UUIDSupplierQueued(new LinkedList<>());
    }

    @Bean
    public PricerService pricerServiceNew(
            ObjectMapper objectMapper,
            GuidelineReader guidelineReader,
            GuidelineWriter guidelineWriter,
            CacheRepoRedisImpl cacheRepoRedis,
            DirectivePublisherSNSImpl directivePublisherSNS,
            MoneyExchanger moneyExchanger,
            UUIDSupplierQueued uuidSupplier,
            @Value("${app.price-increase-step.amount}") Double priceIncreaseStepAmount,
            @Value("${app.price-increase-step.currency}") String priceIncreaseStepCurrency,
            @Value("${app.max-wins-percentage}") Integer maxWinsPercentage,
            @Value("${app.min-costs-percentage}") Integer minCostsPercentage
    ) {
        Money priceIncreaseStep = Money.of(priceIncreaseStepAmount, priceIncreaseStepCurrency);
        return new PricerService(objectMapper, cacheRepoRedis, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisherSNS, cacheRepoRedis, guidelineReader, Instant::now);
    }

}
