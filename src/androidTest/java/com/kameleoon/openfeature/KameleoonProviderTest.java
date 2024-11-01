package com.kameleoon.openfeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.KameleoonClientFactory;
import com.kameleoon.data.Conversion;
import com.kameleoon.data.CustomData;
import com.kameleoon.data.Data;
import com.kameleoon.openfeature.dto.types.DataType;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderMetadata;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderNotReady;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderReady;
import dev.openfeature.sdk.exceptions.OpenFeatureError.ProviderNotReadyError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class KameleoonProviderTest {

	private static final String SITE_CODE = "siteCode";
	private static final String FLAG_KEY = "flagKey";

	private KameleoonClientConfig config;
	private static Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
	private KameleoonClient clientMock;
	private KameleoonResolver resolverMock;
	private KameleoonProvider provider;

	@Before
	public void setUp() {
		clientMock = mock(KameleoonClient.class);
		resolverMock = mock(KameleoonResolver.class);
		config = new KameleoonClientConfig.Builder().build();
		provider = new KameleoonProvider(SITE_CODE, clientMock, resolverMock, context);
	}

	@Test
	public void initWithInvalidSiteCodeThrowsFeatureProviderException() {
		String siteCode = "";
		assertThrows(ProviderNotReadyError.class, () -> {
			new KameleoonProvider(siteCode, "visitorCode", config, context);
		});
	}

	@Test
	public void getMetadataReturnsCorrectMetadata() {
		// Act
		ProviderMetadata metadata = provider.getMetadata();

		// Assert
		assertEquals("Kameleoon Provider", metadata.getName());
	}

	private <T> void setupResolverMock(T defaultValue, T expectedValue) {
		when(resolverMock.resolve(FLAG_KEY, defaultValue, null))
				.thenReturn(new ProviderEvaluation<T>(expectedValue,
						null, Reason.STATIC.toString(), null, null));
	}

	private <T> void assertResult(ProviderEvaluation<T> result, T expectedValue) {
		assertEquals(expectedValue, result.getValue());
		assertNull(result.getErrorCode());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void resolveBooleanValueReturnsCorrectValue() {
		// Arrange
		boolean defaultValue = false;
		boolean expectedValue = true;
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<Boolean> result =
				provider.getBooleanEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertResult(result, expectedValue);
	}

	@Test
	public void resolveDoubleValueReturnsCorrectValue() {
		// Arrange
		double defaultValue = 0.5;
		double expectedValue = 2.5;
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<Double> result =
				provider.getDoubleEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertResult(result, expectedValue);
	}

	@Test
	public void resolveIntegerValueReturnsCorrectValue() {
		// Arrange
		int defaultValue = 1;
		int expectedValue = 2;
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<Integer> result =
				provider.getIntegerEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertResult(result, expectedValue);
	}

	@Test
	public void resolveStringValueReturnsCorrectValue() throws Exception {
		// Arrange
		String defaultValue = "1";
		String expectedValue = "2";
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<String> result =
				provider.getStringEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertResult(result, expectedValue);
	}

	@Test
	public void resolveStructureValueReturnsCorrectValue() {
		// Arrange
		Value defaultValue = new Value.String("default");
		Value expectedValue = new Value.Integer(100);
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<Value> result =
				provider.getObjectEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertEquals(expectedValue, result.getValue());
		assertNull(result.getErrorCode());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void resolveStructureValueReturnsDefaultValue() {
		// Arrange
		Value defaultValue = new Value.String("default");
		Value expectedValue = new Value.String("default");
		setupResolverMock(defaultValue, expectedValue);

		// Act
		ProviderEvaluation<Value> result =
				provider.getObjectEvaluation(FLAG_KEY, defaultValue, null);

		// Assert
		assertEquals(defaultValue, result.getValue());
		assertNull(result.getErrorCode());
		assertNull(result.getErrorMessage());
	}

	@Test
	public void testResolve_addDataCalled() {
		// Arrange
		Map<String, Value> attributes = new HashMap<String, Value>() {{
			put(DataType.CUSTOM_DATA.getValue(), DataType.makeCustomData(1, Collections.emptyList()));
			put(DataType.CONVERSION.getValue(), DataType.makeConversion(1));
		}};

		EvaluationContext evalContext = new ImmutableContext("", attributes);

		List<Data> expectedData = new ArrayList<Data>() {{
			add(new CustomData(1, Collections.emptyList()));
			add(new Conversion(1));
		}};

		// Act
		provider.onContextSet(null, evalContext);

		// Assert
		ArgumentCaptor<Data> dataCaptor = ArgumentCaptor.forClass(Data.class);
		verify(clientMock, times(1)).addData(dataCaptor.capture());
		List<Data> addedData = dataCaptor.getAllValues();

		assertEquals(2, addedData.size());
		assertTrue(addedData.get(0) instanceof CustomData);
		assertTrue(addedData.get(1) instanceof Conversion);
		assertEquals(expectedData.get(0), addedData.get(0));
		assertEquals(expectedData.get(1), addedData.get(1));
	}

	@Test
	public void readyProviderStatus() {
		// Arrange
		when(clientMock.isReady()).thenReturn(true);

		// Assert
		assertEquals(ProviderReady.INSTANCE, provider.getProviderStatus());
	}

	@Test
	public void notReadyProviderStatus() {
		// Assert
		assertEquals(ProviderNotReady.INSTANCE, provider.getProviderStatus());
	}

	@Test
	public void shutdownForgetSiteCode() throws Exception {
		// Arrange
		KameleoonProvider provider = new KameleoonProvider(SITE_CODE, "userId1", config, context);
		KameleoonClient clientFirst = provider.getClient();
		KameleoonClient clientToCheck = KameleoonClientFactory.create(SITE_CODE, "userId1", config, context);

		// Act
		provider.shutdown();
		KameleoonProvider providerSecond = new KameleoonProvider(SITE_CODE, "userId1", config, context);
		KameleoonClient clientSecond = providerSecond.getClient();

		// Assert
		assertSame(clientToCheck, clientFirst);
		assertNotSame(clientFirst, clientSecond);
	}
}
