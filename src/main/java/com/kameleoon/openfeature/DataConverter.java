package com.kameleoon.openfeature;

import com.kameleoon.data.Conversion;
import com.kameleoon.data.CustomData;
import com.kameleoon.data.Data;
import com.kameleoon.openfeature.dto.types.ConversionType;
import com.kameleoon.openfeature.dto.types.CustomDataType;
import com.kameleoon.openfeature.dto.types.DataType;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Value;

import javax.json.*;
import java.util.*;

/**
 * DataConverter is used to convert a data from OpenFeature to Kameleoon and back.
 */
public class DataConverter {

	/**
	 * Dictionary which contains conversion methods by keys
	 */
	private static final Map<String, ValueToDataFunction> conversionMethods = new HashMap<String, ValueToDataFunction>() {
		{
			put(DataType.CONVERSION.getValue(), DataConverter::makeConversion);
			put(DataType.CUSTOM_DATA.getValue(), DataConverter::makeCustomData);
		}
	};


	private DataConverter() {
	}

	/**
	 * The method for converting EvaluationContext data to Kameleoon SDK data types.
	 */
	public static List<Data> toKameleoon(EvaluationContext context) {
		Map<String, Value> contextMap = context != null ? context.asMap() : null;
		if (contextMap == null || contextMap.isEmpty()) {
			return Collections.emptyList();
		}

		List<Data> data = new ArrayList<>(contextMap.size());
		for (Map.Entry<String, Value> entry : contextMap.entrySet()) {
			Value value = entry.getValue();
			List<Value> values = value instanceof Value.List ? value.asList() : Collections.singletonList(value);
			ValueToDataFunction conversionMethod = conversionMethods.get(entry.getKey());
			if (conversionMethod != null && values != null) {
				for (Value val : values) {
					data.add(conversionMethod.apply(val));
				}
			}
		}
		return data;
	}

	/**
	 * The method for converting Kameleoon objects to OpenFeature Value instances.
	 */
	public static Value toOpenFeature(Object context) {
		Value value = null;
		if (context instanceof Value) {
			value = (Value) context;
		} else if (context instanceof Integer) {
			value = new Value.Integer((Integer) context);
		} else if (context instanceof Double) {
			value = new Value.Double((Double) context);
		} else if (context instanceof Float) {
			value = new Value.Double(((Float) context).doubleValue());
		} else if (context instanceof Boolean) {
			value = new Value.Boolean((Boolean) context);
		} else if (context instanceof String) {
			value = new Value.String((String) context);
		} else if (context instanceof JsonObject) {
			value = toOpenFeature((JsonObject) context);
		} else if (context instanceof JsonArray) {
			value = toOpenFeature((JsonArray) context);
		} else if (context instanceof JsonValue) {
			value = toOpenFeature((JsonValue) context);
		}
		return value;
	}

	/**
	 * Converts a Kameleoon JsonObject to an OpenFeature Value instance.
	 *
	 * @param jsonObject the JsonObject to be converted
	 * @return the converted OpenFeature Value instance
	 */
	private static Value.Structure toOpenFeature(JsonObject jsonObject) {
		Map<String, Value> map = new HashMap<>();
		for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
			map.put(entry.getKey(), toOpenFeature(entry.getValue()));
		}
		return new Value.Structure(map);
	}

	/**
	 * Converts a Kameleoon JsonArray to an OpenFeature Value instance.
	 *
	 * @param jsonArray the JsonArray to be converted
	 * @return the converted OpenFeature Value instance
	 */
	private static Value toOpenFeature(JsonArray jsonArray) {
		List<Value> list = new ArrayList<>(jsonArray.size());
		for (JsonValue jsonValue : jsonArray) {
			list.add(toOpenFeature(jsonValue));
		}
		return new Value.List(list);
	}

	/**
	 * Converts a Kameleoon JsonValue to an OpenFeature Value instance.
	 *
	 * @param jsonValue the JsonValue to be converted
	 * @return the converted OpenFeature Value instance
	 */
	private static Value toOpenFeature(JsonValue jsonValue) {
		Value value = null;
		switch (jsonValue.getValueType()) {
			case NUMBER:
				JsonNumber jsonNumber = (JsonNumber) jsonValue;
				if (jsonNumber.isIntegral()) {
					value = new Value.Integer(jsonNumber.intValue());
				} else {
					value = new Value.Double(jsonNumber.doubleValue());
				}
				break;
			case STRING:
				JsonString jsonString = (JsonString) jsonValue;
				value = new Value.String(jsonString.getString());
				break;
			case TRUE:
				value = new Value.Boolean(true);
				break;
			case FALSE:
				value = new Value.Boolean(false);
				break;
			case NULL:
				break;
			default:
				throw new IllegalArgumentException("Unsupported JsonValue type: " + jsonValue.getValueType());
		}
		return value;
	}

	/**
	 * Make Kameleoon {@link CustomData} from {@link Value}
	 */
	private static CustomData makeCustomData(Value value) {
		CustomData customData = null;
		Map<String, Value> structCustomData = value.asStructure();
		if (structCustomData == null) {
			return null;
		}
		Value indexValue = structCustomData.get(CustomDataType.INDEX.getValue());
		Integer index = indexValue != null ? indexValue.asInteger() : null;
		if (index == null) {
			index = 0;
		}
		List<Value> values = null;

		Value dataValues = structCustomData.get(CustomDataType.VALUES.getValue());
		if (dataValues != null) {
			values = dataValues instanceof Value.List
					? dataValues.asList()
					: Collections.singletonList(dataValues);
		}

		if (values != null) {
			List<String> customDataValues = new ArrayList<>();
			for (Value val : values) {
				String strVal = val.asString();
				if (strVal != null) {
					customDataValues.add(strVal);
				}
			}
			customData = new CustomData(index, customDataValues);
		} else {
			customData = new CustomData(index, Collections.emptyList());
		}
		return customData;
	}

	/**
	 * Make Kameleoon {@link Conversion} from {@link Value}
	 */
	private static Conversion makeConversion(Value value) {
		Map<String, Value> structConversion = value.asStructure();
		if (structConversion == null) {
			return null;
		}

		Value goalIdValue = structConversion.get(ConversionType.GOAL_ID.getValue());
		Integer goalId = goalIdValue != null ? goalIdValue.asInteger() : null;
		goalId = goalId != null ? goalId : 0;

		Value revenueValue = structConversion.get(ConversionType.REVENUE.getValue());
		Double revenueDouble = revenueValue != null ? revenueValue.asDouble() : null;
		float revenue = revenueDouble != null ? revenueDouble.floatValue() : 0.0f;

		return new Conversion(goalId, revenue, false);
	}

	@FunctionalInterface
	private interface ValueToDataFunction {
		Data apply(Value value);
	}
}
