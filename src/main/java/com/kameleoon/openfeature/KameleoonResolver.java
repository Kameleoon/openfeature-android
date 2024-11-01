package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonException;
import com.kameleoon.openfeature.dto.types.DataType;
import com.kameleoon.types.Variable;
import com.kameleoon.types.Variation;
import dev.openfeature.sdk.*;

import dev.openfeature.sdk.exceptions.ErrorCode;
import java.util.Iterator;
import java.util.Map;

/**
 * KameleoonResolver makes evalutions based on provided data, conforms to Resolver interface
 */
class KameleoonResolver implements Resolver {

	private final KameleoonClient client;

	KameleoonResolver(KameleoonClient client) {
		this.client = client;
	}

	/**
	 * Main method for getting resolution details based on provided data.
	 */
	@Override
	public <T> ProviderEvaluation<T> resolve(String flagKey, T defaultValue, EvaluationContext context) {
		try {
			// Get a variation (main SDK method)
			Variation variation = client.getVariation(flagKey);

			// Get variant (variation key)
			String variant = variation.getKey();

			// Get variableKey if it's provided in context or any first in variation.
			// It's the responsibility of the client to have only one variable per variation if
			// variableKey is not provided.
			String variableKey = getVariableKey(context, variation.getVariables());

			// Try to get variable by variable key
			Variable variable = variation.getVariables().get(variableKey);

			// Try to get value from variable
			Object value = variable != null ? variable.getValue() : null;

			if (variableKey == null || value == null) {
				return makeResolutionDetails(defaultValue, variant, ErrorCode.FLAG_NOT_FOUND,
						makeErrorDescription(variant, variableKey));
			}

			// Check if the variable value has a required type
			if (!(defaultValue instanceof Value || value.getClass().equals(defaultValue.getClass()))) {
				return makeResolutionDetails(defaultValue, variant, ErrorCode.TYPE_MISMATCH,
						"The type of value received is different from the requested value.");
			}

			@SuppressWarnings("unchecked")
			T typedValue = (T) value;
			return makeResolutionDetails(typedValue, variant);
		} catch (KameleoonException.FeatureException exception) {
			return makeResolutionDetails(defaultValue, null, ErrorCode.FLAG_NOT_FOUND, exception.getMessage());
		} catch (Exception exception) {
			return makeResolutionDetails(defaultValue, null, ErrorCode.GENERAL, exception.getMessage());
		}
	}

	/**
	 * Helper method to get the variable key from the context or variables map.
	 */
	private static String getVariableKey(EvaluationContext context, Map<String, Variable> variables) {
		Value variableKeyValue = context != null ? context.getValue(DataType.VARIABLE_KEY.getValue()) : null;
		String variableKey = variableKeyValue != null ? variableKeyValue.asString() : null;
		if (variableKey == null && !variables.isEmpty()) {
			Iterator<String> iterator = variables.keySet().iterator();
			variableKey = iterator.hasNext() ? iterator.next() : null;
		}
		return variableKey;
	}

	/**
	 * Helper method to create a ResolutionDetails object.
	 */
	private static <T> ProviderEvaluation<T> makeResolutionDetails(T value, String variant) {
		return new ProviderEvaluation<T>(value, variant, Reason.STATIC.toString(), null, null);
	}

	/**
	 * Helper method to create a ResolutionDetails object.
	 */
	private static <T> ProviderEvaluation<T> makeResolutionDetails(T value, String variant, ErrorCode errorCode,
			String errorMessage) {
		return new ProviderEvaluation<T>(value, variant, Reason.STATIC.toString(), errorCode, errorMessage);
	}

	/**
	 * Helper method to create an error description.
	 */
	private static String makeErrorDescription(String variant, String variableKey) {
		return (variableKey == null || variableKey.isEmpty())
				? String.format("The variation '%s' has no variables", variant)
				: String.format("The value for provided variable key '%s' isn't found in variation '%s'", variableKey, variant);
	}
}
