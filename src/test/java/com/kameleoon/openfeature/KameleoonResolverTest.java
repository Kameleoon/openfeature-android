package com.kameleoon.openfeature;

import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonException;
import com.kameleoon.KameleoonException.FeatureEnvironmentDisabled;
import com.kameleoon.KameleoonException.FeatureNotFound;
import com.kameleoon.openfeature.dto.types.DataType;
import com.kameleoon.types.Variable;
import com.kameleoon.types.Variation;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KameleoonResolverTest {

	private KameleoonClient clientMock = mock(KameleoonClient.class);

	private void setupClientMock(Variation variation, KameleoonException error) {
		try {
			if (error == null) {
				when(clientMock.getVariation(anyString())).thenReturn(variation);
			} else {
				when(clientMock.getVariation(anyString())).thenThrow(error);
			}
		} catch (KameleoonException e) {
			throw new RuntimeException(e);
		}
	}

	private static Stream<Arguments> resolve_NoMatchVariable_ReturnsErrorForFlagNotFound_DataProvider() {
		return Stream.of(
				Arguments.of(new Variation("on", -1, -1, Collections.emptyMap()), false, "The variation 'on' has no variables"),
				Arguments.of(new Variation("var", -1, -1, Collections.singletonMap("key", new Variable("", "", null))), true, "The value for provided variable key 'variableKey' isn't found in variation 'var'")
		);
	}

	@ParameterizedTest
	@MethodSource("resolve_NoMatchVariable_ReturnsErrorForFlagNotFound_DataProvider")
	public void resolve_NoMatchVariable_ReturnsErrorForFlagNotFound(Variation variation, boolean addVariableKey, String errorMessage) {
		// Arrange
		setupClientMock(variation, null);

		KameleoonResolver resolver = new KameleoonResolver(clientMock);
		String key = "testFlag";
		int defaultValue = 42;
		Map<String, Value> attributes = new HashMap<>();
		if (addVariableKey) {
			attributes.put(DataType.VARIABLE_KEY.getValue(), new Value.String("variableKey"));
		}
		EvaluationContext context = new ImmutableContext("testVisitor", attributes);

		// Act
		ProviderEvaluation<Integer> result = resolver.resolve(key, defaultValue, context);

		// Assert
		assertEquals(defaultValue, result.getValue());
		assertEquals(ErrorCode.FLAG_NOT_FOUND, result.getErrorCode());
		assertEquals(errorMessage, result.getErrorMessage());
		assertEquals(variation.getKey(), result.getVariant());
	}

	private static Stream<Arguments> resolve_MismatchType_ReturnsErrorTypeMismatch_DataProvider() {
		return Stream.of(
				Arguments.of(new Variable("key", "BOOLEAN", true)),
				Arguments.of(new Variable("key", "STRING", "test")),
				Arguments.of(new Variable("key", "NUMBER", 10.0))
		);
	}

	@ParameterizedTest
	@MethodSource("resolve_MismatchType_ReturnsErrorTypeMismatch_DataProvider")
	public void resolve_MismatchType_ReturnsErrorTypeMismatch(Variable variable) {
		// Arrange
		Variation variation = new Variation("on", -1, -1, Collections.singletonMap("key", variable));
		setupClientMock(variation, null);

		KameleoonResolver resolver = new KameleoonResolver(clientMock);
		String key = "testFlag";
		int defaultValue = 42;

		// Act
		ProviderEvaluation<Integer> result = resolver.resolve(key, defaultValue, null);

		// Assert
		assertEquals(defaultValue, result.getValue());
		assertEquals(ErrorCode.TYPE_MISMATCH, result.getErrorCode());
		assertEquals("The type of value received is different from the requested value.", result.getErrorMessage());
		assertEquals(variation.getKey(), result.getVariant());
	}

	private static Stream<Arguments> kameleoonException_DataProvider() {
		return Stream.of(
				Arguments.of(new FeatureNotFound("featureException")),
				Arguments.of(new FeatureEnvironmentDisabled("featureException"))
		);
	}

	@ParameterizedTest
	@MethodSource("kameleoonException_DataProvider")
	public void resolve_KameleoonException_ReturnsErrorProperError(KameleoonException error) {
		// Arrange
		setupClientMock(null, error);

		KameleoonResolver resolver = new KameleoonResolver(clientMock);
		String flagKey = "testFlag";
		int defaultValue = 42;

		// Act
		ProviderEvaluation<Integer> result = resolver.resolve(flagKey, defaultValue, null);

		// Assert
		assertEquals(defaultValue, result.getValue());
		assertEquals(ErrorCode.FLAG_NOT_FOUND, result.getErrorCode());
		assertEquals(error.getMessage(), result.getErrorMessage());
		assertNull(result.getVariant());
	}

	private static Stream<Arguments> resolve_ReturnsResultDetails_DataProvider() {
		return Stream.of(
				Arguments.of(null, Collections.singletonMap("k", new Variable("k", "NUMBER", 10)), 10, 9),
				Arguments.of(null, Collections.singletonMap("k1", new Variable("k1", "STRING", "str")), "str", "st"),
				Arguments.of(null, Collections.singletonMap("k2", new Variable("k2", "BOOLEAN", true)), true, false),
				Arguments.of(null, Collections.singletonMap("k3", new Variable("k3", "NUMBER", 10.0)), 10.0, 11.0),
				Arguments.of(null, Collections.singletonMap("k3", new Variable("k3", "NUMBER", 10.0)), 10.0, new Value.String("11")),
				Arguments.of("varKey", Collections.singletonMap("varKey", new Variable("varKey", "NUMBER", 10.0)), 10.0, 11.0)
		);
	}

	@ParameterizedTest
	@MethodSource("resolve_ReturnsResultDetails_DataProvider")
	public void resolve_ReturnsResultDetails(String variableKey, Map<String, Variable> variables, Object expectedValue, Object defaultValue) {
		// Arrange
		Variation variation = new Variation("on", -1, -1, variables);
		setupClientMock(variation, null);

		KameleoonResolver resolver = new KameleoonResolver(clientMock);
		String flagKey = "testFlag";
		Map<String, Value> attributes = new HashMap<>();
		if (variableKey != null) {
			attributes.put(DataType.VARIABLE_KEY.getValue(), new Value.String(variableKey));
		}
		EvaluationContext context = new ImmutableContext("testVisitor", attributes);

		// Act
		ProviderEvaluation<Object> result = resolver.resolve(flagKey, defaultValue, context);

		// Assert
		assertEquals(expectedValue, result.getValue());
		assertNull(result.getErrorCode());
		assertNull(result.getErrorMessage());
		assertEquals(variation.getKey(), result.getVariant());
	}
}
