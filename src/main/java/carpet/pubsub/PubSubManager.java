package carpet.pubsub;

import carpet.CarpetMod;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Central interface for PubSub interactions
 *
 * @see CarpetMod#PUBSUB
 */
public final class PubSubManager {
	public final PubSubNode ROOT = new PubSubNode(null, "");
	private final Map<String, PubSubNode> knownNodes = new TreeMap<>();

	/**
	 * Get a node if it already exists. Call this for client-initiated requests
	 *
	 * @param name The path to the node (separated by ".")
	 *
	 * @return The node requested or null if it does not exist
	 * @see PubSubNode#getChildNode(String...)
	 * @see PubSubNode#getChildNode(Collection)
	 */
	@Nullable
	public PubSubNode getNode(String name) {
		synchronized (knownNodes) {
			return knownNodes.get(name);
		}
	}

	/**
	 * Get a node or create one if it does not exist Call this for server-initiated requests (when publishing)
	 *
	 * @param name The path to the node (separated by ".")
	 *
	 * @return The node requested
	 * @see PubSubNode#getOrCreateChildNode(String...)
	 * @see PubSubNode#getOrCreateChildNode(Collection)
	 */
	public PubSubNode getOrCreateNode(String name) {
		synchronized (knownNodes) {
			PubSubNode node = knownNodes.get(name);
			if (node == null) {
				String[] path = name.split("\\.");
				node = addKnownNode(ROOT.getOrCreateChildNode(path));
			}
			return node;
		}
	}

	public PubSubNode addKnownNode(PubSubNode node) {
		synchronized (knownNodes) {
			for (PubSubNode n = node; n != ROOT; n = n.parent) {
				knownNodes.put(n.fullName, n);
			}
		}
		return node;
	}

	public void subscribe(PubSubNode node, PubSubSubscriber subscriber) {
		node.subscribe(subscriber);
	}

	public void unsubscribe(PubSubNode node, PubSubSubscriber subscriber) {
		node.unsubscribe(subscriber);
	}

	public void publish(PubSubNode node, Object value) {
		node.publish(value);
	}

	public void update(int tickCounter) {
		ROOT.update(tickCounter);
	}
}
