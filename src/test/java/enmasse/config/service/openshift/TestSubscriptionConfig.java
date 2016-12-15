package enmasse.config.service.openshift;

import enmasse.config.service.TestResource;
import enmasse.config.service.model.LabelSet;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TestSubscriptionConfig implements SubscriptionConfig {
    @Override
    public MessageEncoder<TestResource> getMessageEncoder() {
        return set -> {
            Message message = Message.Factory.create();

            message.setBody(new AmqpSequence(set.stream().map(r -> r.getResource().getMetadata().getName()).collect(Collectors.toList())));
            return message;
        };
    }

    @Override
    public ObserverOptions getObserverOptions(OpenShiftClient client, Map<String, String> filter) {
        Map<String, String> filterMap = new LinkedHashMap<>(filter);
        filterMap.put("key", "value");
        return new ObserverOptions(LabelSet.fromMap(filterMap), new ClientMixedOperation[] {client.configMaps() });
    }
}
