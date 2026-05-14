package com.codesync.execution.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_JOBS = "execution.jobs";
    public static final String QUEUE_RESULTS = "execution.results";
    public static final String EXCHANGE_EXECUTION = "execution.exchange";
    public static final String ROUTING_KEY_JOBS = "execution.jobs.routing";
    public static final String ROUTING_KEY_RESULTS = "execution.results.routing";

    @Bean
    public Queue jobsQueue() {
        return new Queue(QUEUE_JOBS);
    }

    @Bean
    public Queue resultsQueue() {
        return new Queue(QUEUE_RESULTS);
    }

    @Bean
    public TopicExchange executionExchange() {
        return new TopicExchange(EXCHANGE_EXECUTION);
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, TopicExchange executionExchange) {
        return BindingBuilder.bind(jobsQueue).to(executionExchange).with(ROUTING_KEY_JOBS);
    }

    @Bean
    public Binding resultsBinding(Queue resultsQueue, TopicExchange executionExchange) {
        return BindingBuilder.bind(resultsQueue).to(executionExchange).with(ROUTING_KEY_RESULTS);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
