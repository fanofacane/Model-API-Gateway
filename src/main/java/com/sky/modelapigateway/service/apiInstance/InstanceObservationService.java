package com.sky.modelapigateway.service.apiInstance;

import com.sky.modelapigateway.domain.observe.InstanceObservationDTO;
import com.sky.modelapigateway.domain.observe.ObservationOverviewDTO;
import com.sky.modelapigateway.domain.request.InstanceObservationRequest;

import java.util.List;

public interface InstanceObservationService {
    ObservationOverviewDTO getObservationOverview(InstanceObservationRequest request);

    List<InstanceObservationDTO> getInstanceObservationList(InstanceObservationRequest request);
}
