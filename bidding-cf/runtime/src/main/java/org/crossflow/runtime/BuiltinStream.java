package org.crossflow.runtime;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;

import javax.jms.*;
import java.io.Serializable;
import java.util.*;

public class BuiltinStream<T extends Serializable> implements Stream {

	protected ActiveMQDestination destination;
	protected Connection connection;
	protected Session session;
	protected Workflow<?> workflow;
	protected List<MessageConsumer> consumers = new LinkedList<>();
	protected List<BuiltinStreamConsumer<T>> pendingConsumers = new ArrayList<>();
	protected String name;
	protected boolean broadcast;

	public BuiltinStream(Workflow<?> workflow, String name) {
		this(workflow, name, true);
	}

	public BuiltinStream(Workflow<?> workflow, String name, boolean broadcast) {
		this.workflow = workflow;
		this.name = name;
		this.broadcast = broadcast;
	}

	protected String getDestinationName() {
		return name + "." + workflow.getInstanceId();
	}

	public void init() throws Exception {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(workflow.getBroker());
		connectionFactory.setTrustAllPackages(true);
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		if (broadcast)
			destination = (ActiveMQDestination) session.createTopic(getDestinationName());
		else
			destination = (ActiveMQDestination) session.createQueue(getDestinationName());

		for (BuiltinStreamConsumer<T> pendingConsumer : pendingConsumers) {
			addConsumer(pendingConsumer);
		}
		pendingConsumers.clear();

	}

	long lastExceptionHidden = 0;

	public void send(T t) throws Exception {
		try {
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			producer.setPriority(9);
			producer.send(session.createTextMessage(workflow.getSerializer().serialize(t)));
			producer.close();
		} catch (Exception e) {
			long currentTime = System.currentTimeMillis();
			if (e.getMessage() != null && e.getMessage().equals("The Session is closed")) {
				if (currentTime - lastExceptionHidden > 1000)
//					System.err.println(
//							"BuiltinStream: send(" + t + ") cannot process message after workflow termination.");
				lastExceptionHidden = currentTime;
			} else
				throw e;
		}
	}

	public void addConsumer(BuiltinStreamConsumer<T> consumer) throws Exception {

		if (session == null) {
			pendingConsumers.add(consumer);
			return;
		}

		MessageConsumer messageConsumer = session.createConsumer(destination);
		consumers.add(messageConsumer);
		messageConsumer.setMessageListener(message -> {
			String messageText;
			try {
				if (message instanceof TextMessage) {
					TextMessage amqMessage = (TextMessage) message;
					messageText = amqMessage.getText();
				} else {
					BytesMessage bm = (BytesMessage) message;
					byte data[] = new byte[(int) bm.getBodyLength()];
					bm.readBytes(data);
					messageText = new String(data);
				}
				consumer.consume(workflow.getSerializer().deserialize(messageText));
			} catch (JMSException e) {
				workflow.reportInternalException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void stop() {
		try {

			for (MessageConsumer c : consumers) {
				c.close();
			}
			session.close();
			connection.close();
		} catch (Exception ex) {
			// Nothing to do at this stage
			System.err.println(getName() + ":");
			ex.printStackTrace();
		}
	}

	@Override
	public boolean isBroadcast() {
		return broadcast;
	}

	@Override
	public Collection<String> getDestinationNames() {
		return Collections.singleton(destination.getPhysicalName());
	}

	public String getName() {
		return name;
	}

}
