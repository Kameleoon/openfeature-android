package com.kameleoon.openfeature;

import com.kameleoon.data.Conversion;
import com.kameleoon.data.CustomData;
import com.kameleoon.data.Data;
import com.kameleoon.logging.KameleoonLogger;
import com.kameleoon.logging.LogLevel;
import com.kameleoon.openfeature.dto.types.ConversionType;
import com.kameleoon.openfeature.dto.types.CustomDataType;
import com.kameleoon.openfeature.dto.types.DataType;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class DataConverterTest {

	@BeforeAll
	public static void setUp() {
		KameleoonLogger.setLogLevel(LogLevel.NONE);
	}

	@Test
	public void toKameleoon_NullContext_ReturnsEmpty() {
		// Arrange
		EvaluationContext context = null;

		// Act
		List<Data> result = DataConverter.toKameleoon(context);

		// Assert
		assertTrue(result.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void toKameleoon_WithConversionData_ReturnsConversionData(boolean addRevenue) {
		// Arrange
		Random rnd = new Random();
		int expectedGoalId = rnd.nextInt();
		double expectedRevenue = rnd.nextDouble();
		Map<String, Value> conversionDictionary = new HashMap<String, Value>() {{
			put(ConversionType.GOAL_ID.getValue(), new Value.Integer(expectedGoalId));
		}};

		if (addRevenue) {
			conversionDictionary.put(ConversionType.REVENUE.getValue(), new Value.Double(expectedRevenue));
		}
		EvaluationContext context = new ImmutableContext("",
				Collections.singletonMap(DataType.CONVERSION.getValue(), new Value.Structure(conversionDictionary))
		);

		// Act
		List<Data> result = DataConverter.toKameleoon(context);

		// Assert
		assertEquals(1, result.size());
		Conversion conversion = (Conversion) result.get(0);
		assertEquals(expectedGoalId, conversion.getGoalId());

		if (addRevenue) {
			assertEquals(expectedRevenue, conversion.getRevenue(), 0.0000001);
		}
	}

	@ParameterizedTest
	@MethodSource("provideCustomData")
	public void toKameleoonData_WithCustomData_ReturnsCustomData(String[] expectedValues) {
		// Arrange
		int expectedIndex = new Random().nextInt();
		Map<String, Value> customDataDictionary = new HashMap<String, Value>() {{
			put(CustomDataType.INDEX.getValue(), new Value.Integer(expectedIndex));
		}};
		if (expectedValues.length == 1) {
			customDataDictionary.put(CustomDataType.VALUES.getValue(), new Value.String(expectedValues[0]));
		} else if (expectedValues.length > 1) {
			List<Value> valueList = Stream.of(expectedValues).map(Value.String::new).collect(Collectors.toList());
			customDataDictionary.put(CustomDataType.VALUES.getValue(), new Value.List(valueList));
		}
		EvaluationContext context = new ImmutableContext("",
				Collections.singletonMap(DataType.CUSTOM_DATA.getValue(), new Value.Structure(customDataDictionary))
		);

		// Act
		List<Data> result = DataConverter.toKameleoon(context);

		// Assert
		assertEquals(1, result.size());
		CustomData customData = (CustomData) result.get(0);
		assertEquals(expectedIndex, customData.getId());
		assertArrayEquals(expectedValues, customData.getValues());
	}

	private static Stream<Arguments> provideCustomData() {
		return Stream.of(
				Arguments.of((Object) new String[]{}),
				Arguments.of((Object) new String[]{""}),
				Arguments.of((Object) new String[]{"v1"}),
				Arguments.of((Object) new String[]{"v1", "v1"}),
				Arguments.of((Object) new String[]{"v1", "v2", "v3"})
		);
	}

	@Test
	public void toKameleoonData_AllTypes_ReturnsAllData() {
		// Arrange
		Random rnd = new Random();
		int goalId1 = rnd.nextInt();
		int goalId2 = rnd.nextInt();
		int index1 = rnd.nextInt();
		int index2 = rnd.nextInt();

		Map<String, Value> allDataDictionary = new HashMap<String, Value>() {{
			put(DataType.CONVERSION.getValue(), new Value.List(Arrays.asList(
					new Value.Structure(Collections.singletonMap(ConversionType.GOAL_ID.getValue(), new Value.Integer(goalId1))),
					new Value.Structure(Collections.singletonMap(ConversionType.GOAL_ID.getValue(), new Value.Integer(goalId2)))
			)));
			put(DataType.CUSTOM_DATA.getValue(), new Value.List(Arrays.asList(
					new Value.Structure(Collections.singletonMap(CustomDataType.INDEX.getValue(), new Value.Integer(index1))),
					new Value.Structure(Collections.singletonMap(CustomDataType.INDEX.getValue(), new Value.Integer(index2)))
			)));
		}};

		EvaluationContext context = new ImmutableContext("", allDataDictionary);

		// Act
		List<Data> result = DataConverter.toKameleoon(context);

		// Assert
		assertEquals(4, result.size());
		List<Conversion> conversions = result.stream()
				.filter(Conversion.class::isInstance)
				.map(Conversion.class::cast)
				.collect(Collectors.toList());
		assertEquals(goalId1, conversions.get(0).getGoalId());
		assertEquals(goalId2, conversions.get(1).getGoalId());
		List<CustomData> customData = result.stream()
				.filter(CustomData.class::isInstance)
				.map(CustomData.class::cast)
				.collect(Collectors.toList());
		assertEquals(index1, customData.get(0).getId());
		assertEquals(index2, customData.get(1).getId());
	}

	@ParameterizedTest
	@MethodSource("provideTestData")
	public void toOpenFeature_ReturnsCorrectValue(Object input, Value expected) {
		// Act
		Value result = DataConverter.toOpenFeature(input);

		// Assert
		assertEquals(expected, result);
	}

	private static Stream<Arguments> provideTestData() {
		JsonObject jsonObject = null;
		JsonArray jsonArray = null;

		try (JsonReader jsonReader1 = Json.createReader(new StringReader("{\"key\": \"value\"}"));
				JsonReader jsonReader2 = Json.createReader(new StringReader("[1, 2, 3]"))) {
			jsonObject = jsonReader1.readObject();
			jsonArray = jsonReader2.readArray();
		}

		return Stream.of(
				Arguments.of(null, null),
				Arguments.of(new Value.Integer(1), new Value.Integer(1)),
				Arguments.of(42, new Value.Integer(42)),
				Arguments.of(3.14, new Value.Double(3.14)),
				Arguments.of(true, new Value.Boolean(true)),
				Arguments.of("test", new Value.String("test")),
				Arguments.of(jsonObject, new Value.Structure(Collections.singletonMap("key", new Value.String("value")))),
				Arguments.of(jsonArray, new Value.List(Stream.of(1, 2, 3).map(Value.Integer::new)
						.collect(Collectors.toList())))
		);
	}
}
