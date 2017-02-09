package com.esri.kura.example.flightsensor;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightSensor implements ConfigurableComponent, CloudClientListener {
    private static final Logger s_logger = LoggerFactory.getLogger(FlightSensor.class);
    private static final String APP_ID = "FlightSensor";
    
    private static final String FLIGHT_URL = "url";
    
    private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";
    private static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
    private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";
    
    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private final ScheduledExecutorService m_worker;
    private ScheduledFuture<?> m_handle;
    
    private Map<String, Object> properties;
    
    public FlightSensor() {
    	super();
        this.m_worker = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Bundle " + APP_ID + " has started with config!");

        this.properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

        // get the mqtt client for this application
        try {

            // Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudClient for {}...", APP_ID);
            this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
            this.m_cloudClient.addCloudClientListener(this);

            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate(false);
        } catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
        s_logger.info("Activating Heater... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating " + APP_ID + "...");

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        // Releasing the CloudApplicationClient
        s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
        this.m_cloudClient.release();

        s_logger.debug("Deactivating " + APP_ID + "...  Done");
    }

    public void updated(Map<String, Object> properties) {
    	s_logger.debug("Updating " + APP_ID + "...  Done");

        // store the properties received
        this.properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate(true);
        s_logger.debug("Updating " + APP_ID + "...  Done");
    }

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onControlMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageArrived(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageConfirmed(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(boolean onUpdate) {
        // cancel a current worker handle if one if active
        if (this.m_handle != null) {
            this.m_handle.cancel(true);
        }

        // schedule a new worker based on the properties of the service
        int pubrate = (Integer) this.properties.get(PUBLISH_RATE_PROP_NAME);
        this.m_handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName(getClass().getSimpleName());
                doPublish();
            }
        }, 0, pubrate, TimeUnit.SECONDS);
    }
    
    /**
     * Called at the configured rate to publish the next temperature measurement.
     */
    private void doPublish() {
        // fetch the publishing configuration from the publishing properties
        String topic = (String) this.properties.get(PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) this.properties.get(PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) this.properties.get(PUBLISH_RETAIN_PROP_NAME);

        
     // Get and filter json
        String str2send = "";
        try {
        String urlString = (String) this.properties.get(FLIGHT_URL);
//        s_logger.info("url string is " + urlString);
        str2send = FlightReader.getFilteredJson(urlString);
        if(str2send.isEmpty())
        	str2send = "Empty String. " + new Date();
        }
        catch (Throwable ex) {
        	s_logger.error(ex.getMessage());
        	str2send = ex.getMessage();
        }
        
        // Publish the message
        try {
            this.m_cloudClient.publish(topic, str2send.getBytes(), qos, retain, 2);
            s_logger.info("Published to {} message: {}", topic, str2send);
        } catch (Exception e) {
            s_logger.error("Cannot publish topic: " + topic, e);
        }
    }
}
