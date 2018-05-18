/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.catalog;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.apiclients.OSBApiClient;
import io.enmasse.systemtest.bases.TestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Optional;

import static io.enmasse.systemtest.Environment.useMinikubeEnv;
import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
class ServiceCatalogApiTest extends TestBase {

    private OSBApiClient osbApiClient;
    private static Logger log = CustomLogger.getLogger();
    private HashMap<AddressSpace, String> instances = new HashMap<>();

    //================================================================================================
    //==================================== OpenServiceBroker methods =================================
    //================================================================================================

    /**
     * Provision of service instance and optionally wait until instance is ready
     *
     * @param addressSpace address space that will be created
     * @param wait         true for wait until service instance is ready to use
     * @return id of instance
     * @throws Exception
     */
    private String createServiceInstance(AddressSpace addressSpace, boolean wait, Optional<String> instanceId) throws Exception {
        String processedInstanceId = osbApiClient.provisionInstance(addressSpace, instanceId);
        if (wait) {
            waitForServiceInstanceReady(processedInstanceId);
        }
        instances.put(addressSpace, processedInstanceId);
        return processedInstanceId;
    }

    private String createServiceInstance(AddressSpace addressSpace) throws Exception {
        return createServiceInstance(addressSpace, true, Optional.empty());
    }

    private void deleteServiceInstance(AddressSpace addressSpace, String instanceId) throws Exception {
        osbApiClient.deprovisionInstance(addressSpace, instanceId);
        instances.remove(addressSpace, instanceId);
    }

    private String generateBinding(AddressSpace addressSpace, String instanceId, HashMap<String, String> binding) throws Exception {
        return osbApiClient.generateBinding(addressSpace, instanceId, binding);
    }

    private void deprovisionBinding(AddressSpace addressSpace, String instanceId, String bindingId) throws Exception {
        osbApiClient.deprovisionBinding(addressSpace, instanceId, bindingId);
    }

    private void waitForServiceInstanceReady(String instanceId) throws Exception {
        TestUtils.waitForServiceInstanceReady(osbApiClient, instanceId);
    }

    @BeforeAll
    void initializeTestBase() {
        if (!environment.useMinikube()) {
            osbApiClient = new OSBApiClient(kubernetes);
        } else {
            log.info("Open Service Broker API client cannot be initialized, tests running on minikube");
        }
    }

    @AfterAll
    void tearDown() {
        if (!environment.skipCleanup()) {
            instances.forEach((space, id) -> {
                try {
                    osbApiClient.deprovisionInstance(space, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            instances.clear();
        } else {
            log.warn("Remove service instances in tear down - SKIPPED!");
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServiceInstanceWithBindingStandard() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("myspace-via-osbapi-standard", AddressSpaceType.STANDARD);
        provideAndCreateBinding(addressSpaceViaOSBAPI, "*");
    }

    @Test
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServiceInstanceWithBindingBrokered() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("myspace-via-osbapi-brokered", AddressSpaceType.BROKERED);
        provideAndCreateBinding(addressSpaceViaOSBAPI, "#");
    }

    private void provideAndCreateBinding(AddressSpace addressSpace, String wildcardMark) throws Exception {
        String instanceId = createServiceInstance(addressSpace);
        HashMap<String, String> bindResources = new HashMap<>();
        bindResources.put("sendAddresses", String.format("queue.%s", wildcardMark));
        bindResources.put("receiveAddresses", String.format("queue.%s", wildcardMark));
        bindResources.put("consoleAccess", "true");
        bindResources.put("consoleAdmin", "false");
        bindResources.put("externalAccess", "false");
        String bindingId = generateBinding(addressSpace, instanceId, bindResources);

        //deprovisionBinding(addressSpace, instanceId, bindingId); //!TODO disabled due to deleteBinding is not implemented
        deleteServiceInstance(addressSpace, instanceId);
    }

    @Test
    @Disabled("disabled due to issue: #1216")
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testProvideServicesWithIdenticalId() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace("my-space-via-osbapi-brokered", AddressSpaceType.BROKERED);
        AddressSpace addressSpaceViaOSBAPI2 = new AddressSpace("another-space-via-osbapi-brokered", AddressSpaceType.BROKERED);
        String instanceId = createServiceInstance(addressSpaceViaOSBAPI);
        createServiceInstance(addressSpaceViaOSBAPI2, true, Optional.of(instanceId));

        //!TODO there are missing steps what should happen when instanceId already exists
    }

    @Test
    @Disabled("disabled due to issue: #1215")
    @DisabledIfEnvironmentVariable(named = useMinikubeEnv, matches = "true")
    void testMissingRequiredParameter() throws Exception {
        AddressSpace addressSpaceViaOSBAPI = new AddressSpace(null, AddressSpaceType.BROKERED);
        createServiceInstance(addressSpaceViaOSBAPI);

        //!TODO there are missing steps what should happen when required parameter is missing
    }

}
