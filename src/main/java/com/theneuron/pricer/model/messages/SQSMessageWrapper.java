package com.theneuron.pricer.model.messages;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class SQSMessageWrapper {

    public String Message;
    public String TopicARN;

}
