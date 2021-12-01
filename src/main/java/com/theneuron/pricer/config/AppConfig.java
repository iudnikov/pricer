package com.theneuron.pricer.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.theneuron.pricer.repo.CacheRepoRedisImpl;
import com.theneuron.pricer.repo.CurrencyRateReader;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
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
import software.amazon.awssdk.services.sns.SnsClient;

import javax.jms.JMSException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
@Profile("!local")
public class AppConfig {

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
    public SnsClient amazonSNSClient() {
        return SnsClient.create();
    }

    @Bean
    public MoneyExchanger moneyExchanger(
            CurrencyRateReader reader
    ) {
        return new MoneyExchangerImpl(reader, DateTime::now);
    }

    @Bean
    public PricerService pricerServiceNew(
            ObjectMapper objectMapper,
            GuidelineReader guidelineReader,
            GuidelineWriter guidelineWriter,
            CacheRepoRedisImpl cacheRepoRedis,
            DirectivePublisherSNSImpl directivePublisherSNS,
            MoneyExchanger moneyExchanger,
            @Value("${app.price-increase-step.amount}") Double priceIncreaseStepAmount,
            @Value("${app.price-increase-step.currency}") String priceIncreaseStepCurrency,
            @Value("${app.max-wins-percentage}") Integer maxWinsPercentage,
            @Value("${app.min-costs-percentage}") Integer minCostsPercentage
    ) {
        Money priceIncreaseStep = Money.of(priceIncreaseStepAmount, priceIncreaseStepCurrency);
        return new PricerService(objectMapper, cacheRepoRedis, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisherSNS, cacheRepoRedis, guidelineReader, Instant::now);
    }

    @Bean
    public Supplier<Jedis> jedis(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") Integer port,
            @Value("${spring.redis.database}") Integer db
    ) {
        return () -> {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(32);
            JedisPool pool = new JedisPool(poolConfig, host, port);
            Jedis jedis = pool.getResource();
            jedis.select(db);
            return jedis;
        };
    }

    @Bean
    public SQSConnection sqsConnectionProd() throws JMSException {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.defaultClient());
        return connectionFactory.createConnection();
    }
}
