package com.kameleoon.openfeature.dto.types;

import dev.openfeature.sdk.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * DataType is used to add different Kameleoon data types using {@link dev.openfeature.sdk.EvaluationContext}.
 */
public enum DataType {
	VARIABLE_KEY("variableKey"),
	CONVERSION("conversion"),
	CUSTOM_DATA("customData");

	private final String value;

	DataType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Makes {@link dev.openfeature.sdk.Value} based on {@link com.kameleoon.data.Conversion} parameters.
	 *
	 * @param goalId  the goal ID for the Conversion
	 * @param revenue the revenue for the Conversion
	 * @return a {@link dev.openfeature.sdk.Value} structure containing the Conversion data
	 */
	public static Value makeConversion(int goalId, float revenue) {
		return new Value.Structure(new HashMap<String, Value>() {{
			put(ConversionType.GOAL_ID.getValue(), new Value.Integer(goalId));
			put(ConversionType.REVENUE.getValue(), new Value.Double(revenue));
		}});
	}

	/**
	 * Makes {@link dev.openfeature.sdk.Value} based on {@link com.kameleoon.data.Conversion} parameters.
	 *
	 * @param goalId the goal ID for the Conversion
	 * @return a {@link dev.openfeature.sdk.Value} structure containing the Conversion data
	 */
	public static Value makeConversion(int goalId) {
		return new Value.Structure(new HashMap<String, Value>() {{
			put(ConversionType.GOAL_ID.getValue(), new Value.Integer(goalId));
		}});
	}

	/**
	 * Makes {@link dev.openfeature.sdk.Value} based on {@link com.kameleoon.data.CustomData} parameters.
	 *
	 * @param id     the ID for the CustomData
	 * @param values the values for the CustomData
	 * @return a {@link dev.openfeature.sdk.Value} structure containing the CustomData
	 */
	public static Value makeCustomData(int id, List<String> values) {
		List<Value.String> valueList = new ArrayList<>();
		if (values != null) {
			for (String value : values) {
				valueList.add(new Value.String(value));
			}
		}
		return new Value.Structure(new HashMap<String, Value>() {{
			put(CustomDataType.INDEX.getValue(), new Value.Integer(id));
			put(CustomDataType.VALUES.getValue(), new Value.List(valueList));
		}});
	}

	/**
	 * Makes {@link dev.openfeature.sdk.Value} based on {@link com.kameleoon.data.CustomData} parameters.
	 *
	 * @param id     the ID for the CustomData
	 * @param values the values for the CustomData
	 * @return a {@link dev.openfeature.sdk.Value} structure containing the CustomData
	 */
	public static Value makeCustomData(int id, String... values) {
		return makeCustomData(id, Arrays.asList(values));
	}
}
