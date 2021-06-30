package com.test;

import org.apache.synapse.AbstractSynapseHandler;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.apimgt.tracing.Util;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import java.util.Map;
import org.wso2.carbon.apimgt.tracing.TracingSpan;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.utils.GatewayUtils;
import org.wso2.carbon.apimgt.tracing.TracingTracer;

public class Mock extends AbstractSynapseHandler{

    @Override
    public boolean handleRequestInFlow(MessageContext messageContext) {
        TracingTracer tracer = GatewayUtils.getTracingTracer();

        if (Util.tracingEnabled()) {
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            Map headersMap =
                    (Map) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            TracingSpan spanContext = Util.extract(tracer, headersMap);
            TracingSpan responseLatencySpan =
                    Util.startSpan(APIMgtGatewayConstants.RESPONSE_LATENCY, spanContext, tracer);
            Util.setTag(responseLatencySpan, APIMgtGatewayConstants.SPAN_KIND, APIMgtGatewayConstants.SERVER);
            GatewayUtils.setRequestRelatedTags(responseLatencySpan, messageContext);
            messageContext.setProperty(APIMgtGatewayConstants.RESPONSE_LATENCY, responseLatencySpan);
        }
        System.out.println("Hi handleRequestInFlow");
        return true;
    }

    @Override
    public boolean handleRequestOutFlow(MessageContext messageContext) {
        TracingTracer tracer = GatewayUtils.getTracingTracer();
        Map<String, String> tracerSpecificCarrier = new java.util.HashMap<>();
        if (Util.tracingEnabled()) {
            TracingSpan parentSpan = (TracingSpan) messageContext.getProperty(APIMgtGatewayConstants.RESPONSE_LATENCY);
            TracingSpan backendLatencySpan =
                    Util.startSpan(APIMgtGatewayConstants.BACKEND_LATENCY_SPAN, parentSpan, tracer);
            messageContext.setProperty(APIMgtGatewayConstants.BACKEND_LATENCY_SPAN, backendLatencySpan);
            Util.inject(backendLatencySpan, tracer, tracerSpecificCarrier);
            if (org.apache.axis2.context.MessageContext.getCurrentMessageContext() != null) {
                Map headers = (Map) org.apache.axis2.context.MessageContext.getCurrentMessageContext().getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                headers.putAll(tracerSpecificCarrier);
                org.apache.axis2.context.MessageContext.getCurrentMessageContext()
                        .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headers);
            }
        }
        System.out.println("Hi handleRequestOutFlow");
        return true;
    }

    @Override
    public boolean handleResponseInFlow(MessageContext messageContext) {
        if (Util.tracingEnabled()) {
            TracingSpan backendLatencySpan =
                    (TracingSpan) messageContext.getProperty(APIMgtGatewayConstants.BACKEND_LATENCY_SPAN);
            GatewayUtils.setEndpointRelatedInformation(backendLatencySpan, messageContext);
            Util.finishSpan(backendLatencySpan);
        }
        System.out.println("Hi handleResponseInFlow");
        return true;
    }

    @Override
    public boolean handleResponseOutFlow(MessageContext messageContext) {
        if (Util.tracingEnabled()) {
            TracingSpan responseLatencySpan =
                    (TracingSpan) messageContext.getProperty(APIMgtGatewayConstants.RESPONSE_LATENCY);
            GatewayUtils.setAPIRelatedTags(responseLatencySpan, messageContext);
            Util.finishSpan(responseLatencySpan);
        }
        System.out.println("Hi handleResponseOutFlow");
        return true;
    }
}
