package org.ogn.gateway.plugin.udp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ogn.commons.beacon.AircraftBeaconWithDescriptor;
import org.ogn.commons.utils.JsonUtils;

public class UdpJsonForwarderTest {

	private static final String	MULTICAST_GROUP	= "230.0.0.0";
	private static final String	MULTICAST_PORT	= "4446";

	static {
		System.setProperty("ogn.gateway.plugin.udp.multicast_group", MULTICAST_GROUP);
		System.setProperty("ogn.gateway.plugin.udp.multicast_port", MULTICAST_PORT);
	}

	UdpJsonForwarder	forwarder;

	MulticastReceiver	r1, r2;

	AtomicInteger		counter	= new AtomicInteger();

	static class MulticastReceiver extends Thread {
		protected MulticastSocket	socket	= null;
		protected byte[]			buf		= new byte[256];
		private final AtomicInteger	counter;

		public MulticastReceiver(AtomicInteger counter) {
			this.counter = counter;
		}

		@Override
		public void run() {

			try {
				final InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

				socket = new MulticastSocket(4446);
				socket.joinGroup(group);

				while (!Thread.currentThread().isInterrupted()) {
					final DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
					final String received = new String(packet.getData(), 0, packet.getLength());
					// System.out.println(received);

					final AircraftBeaconWithDescriptor beacon =
							JsonUtils.fromJson(received, AircraftBeaconWithDescriptor.class);
					if (beacon != null)
						counter.incrementAndGet();
				}
				socket.leaveGroup(group);
			} catch (final Exception ex) {
				// ex.printStackTrace();
			} finally {
				socket.close();
			}

		}

		@Override
		public void interrupt() {
			super.interrupt();
			this.socket.close();
		}
	}

	@Before
	public void setUp() throws Exception {
		forwarder = new UdpJsonForwarder();
		forwarder.init();

		r1 = new MulticastReceiver(counter);
		r2 = new MulticastReceiver(counter);
		r1.start();
		r2.start();
	}

	@After
	public void tearDown() throws Exception {
		forwarder.stop();
		r1.interrupt();
		r2.interrupt();
	}

	@Test
	public void test() throws Exception {
		for (int i = 0; i < 5; i++)
			forwarder.onBeacon(null, null);

		await().atMost(5, SECONDS).until(() -> {
			return counter.get() == 10;
		});

		// Thread.sleep(2000);
	}

}
