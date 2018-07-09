package com.github.jasminb.jsonapi;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jasminb.jsonapi.exceptions.InvalidJsonApiResourceException;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Errors;

/**
 * Utility methods for validating segments of JSON API resource object.
 *
 * @author jbegic
 */
public class ValidationUtils {

	private ValidationUtils() {
		// Private CTOR
	}

	/**
	 * Asserts that provided resource has required 'data' or 'meta' node.
	 * @param resource resource
	 */
	public static void ensureValidResource(JsonNode resource) {
		if (!resource.has(JSONAPISpecConstants.DATA) && !resource.has(JSONAPISpecConstants.META)) {
			throw new InvalidJsonApiResourceException();
		}
	}

	/**
	 * Returns <code>true</code> in case 'DATA' note has 'ID' and 'TYPE' attributes.
	 * @param dataNode relationship data node
	 * @return <code>true</code> if node has required attributes, else <code>false</code>
	 */
	public static boolean isRelationshipParsable(JsonNode dataNode) {
		return dataNode != null && dataNode.hasNonNull(JSONAPISpecConstants.ID) && dataNode.hasNonNull(JSONAPISpecConstants.TYPE) &&
				!dataNode.get(JSONAPISpecConstants.ID).isContainerNode() && !dataNode.get(JSONAPISpecConstants.TYPE).isContainerNode();
	}

	/**
	 * Ensures that provided node does not hold 'errors' attribute.
	 * @param resourceNode resource node
	 * @throws ResourceParseException
	 */
	public static void ensureNotError(ObjectMapper mapper, JsonNode resourceNode) {
		if (resourceNode != null && resourceNode.hasNonNull(JSONAPISpecConstants.ERRORS)) {
			try {
				throw new ResourceParseException(ErrorUtils.parseError(mapper, resourceNode, Errors.class));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static ObjectNode convertToObjectNode(ArrayNode a) {
		ObjectNode t1 = a.objectNode();
		Iterator<JsonNode> j = a.iterator();
		while (j.hasNext()) {
			JsonNode next = j.next();
			t1.set(next.get("id").asText().concat(next.get("type").asText()), next);
		}
		return t1;		
	}
	
	public static boolean areEqualIgnoreIncludedTagOrdering(JsonNode n1, JsonNode n2) {
		if (n1 instanceof ObjectNode && n2 instanceof ObjectNode) {
			ObjectNode o1 = (ObjectNode)n1;
			ObjectNode o2 = (ObjectNode)n2;
			JsonNode j1 = o1.get("included");
			JsonNode j2 = o2.get("included");
			if (j1 != null && j2 != null && j1 instanceof ArrayNode && j2 instanceof ArrayNode) {
				ObjectNode c1 = convertToObjectNode((ArrayNode)j1);
				ObjectNode c2 = convertToObjectNode((ArrayNode)j2);
				try {
					o1.remove("included");
					o2.remove("included");
					return o1.equals(o2) && c1.equals(c2);
				} finally {
					o2.set("included", j2);
					o1.set("included", j1);
				}
			}
		}
		return n1.equals(n2);
	}
}
