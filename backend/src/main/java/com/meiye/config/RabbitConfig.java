package com.meiye.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "meiye.direct";
    public static final String NOTIFY_QUEUE = "meiye.notify.queue";
    public static final String NOTIFY_ROUTE = "notify";
    public static final String DELAY_QUEUE = "meiye.delay.queue";
    public static final String DELAY_ROUTE = "delay";
    public static final String APPT_RELEASE_QUEUE = "meiye.appointment.release.queue";
    public static final String APPT_RELEASE_ROUTE = "appointment.release";

    @Bean
    public DirectExchange meiyeExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(meiyeExchange()).with(NOTIFY_ROUTE);
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", APPT_RELEASE_ROUTE)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(meiyeExchange()).with(DELAY_ROUTE);
    }

    @Bean
    public Queue appointmentReleaseQueue() {
        return QueueBuilder.durable(APPT_RELEASE_QUEUE).build();
    }

    @Bean
    public Binding appointmentReleaseBinding() {
        return BindingBuilder.bind(appointmentReleaseQueue()).to(meiyeExchange()).with(APPT_RELEASE_ROUTE);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
