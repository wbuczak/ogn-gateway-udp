/**
 * Copyright (c) 2018 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.udp;

import java.util.Optional;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftBeaconWithDescriptor;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.forwarder.OgnAircraftBeaconForwarder;
import org.ogn.commons.udp.MulticastPublisher;
import org.ogn.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * JSON UDP (multicast) publisher plug-in for OGN gateway
 * 
 * @author wbuczak
 */
public class UdpJsonForwarder implements OgnAircraftBeaconForwarder {

	private static final Logger				LOG			= LoggerFactory.getLogger(UdpJsonForwarder.class);

	private static final String				VERSION		= "0.0.1";

	private static final Object				syncMonitor	= 1;

	private static volatile boolean			initialized	= false;

	private MulticastPublisher				publisher;

	private ClassPathXmlApplicationContext	ctx;

	private Config							config;

	@Component
	private static class Config {
		@Value("${ogn.gateway.plugin.udp.multicast_group:#{systemProperties['OGN_GATEWAY_PLUGIN_UDP_MULTICAST_GROUP']}}")
		String	multicastGroup;

		@Value("${ogn.gateway.plugin.udp.multicast_port:#{systemProperties['OGN_GATEWAY_PLUGIN_UDP_MULTICAST_PORT'] ?: 0}}")
		int		multicastPort;

		@Value("${ogn.gateway.plugin.udp.multicast_ttl:#{systemProperties['OGN_GATEWAY_PLUGIN_UDP_MULTICAST_TTL'] ?: 1}}")
		int		ttl;

	}

	@Override
	public void init() {

		synchronized (syncMonitor) {
			if (!initialized) {
				try {
					ctx = new ClassPathXmlApplicationContext("classpath:udpforwarder-application-context.xml");
					ctx.getEnvironment().setDefaultProfiles("PRO");
					config = ctx.getBean(Config.class);

					LOG.info("configured plugin(group: {}, port: {}, ttl: {})", config.multicastGroup,
							config.multicastPort, config.ttl);
					initialized = true;

				} catch (final Exception ex) {
					LOG.error("context initialization failed", ex);
				}

				publisher = new MulticastPublisher(config.ttl);
			}
		} // sync

	}

	@Override
	public String getName() {
		return "JSON udp (multicast) publisher";
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public String getDescription() {
		return "publishes OGN beacons in JSON format (UDP multicast))";
	}

	@Override
	public void stop() {
		publisher.stop();
		if (ctx != null)
			ctx.close();
	}

	@Override
	public void onBeacon(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {
		send(new AircraftBeaconWithDescriptor(beacon, descriptor));
	}

	private void send(AircraftBeaconWithDescriptor beacon) {
		final String jsonMsg = JsonUtils.toJson(beacon);
		LOG.trace("sending beacon: {}", jsonMsg);
		publisher.send(config.multicastGroup, config.multicastPort, jsonMsg.getBytes());
	}

}