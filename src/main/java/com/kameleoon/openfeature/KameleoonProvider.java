package com.kameleoon.openfeature;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kameleoon.KameleoonClient;
import com.kameleoon.KameleoonClientConfig;
import com.kameleoon.KameleoonClientFactory;
import com.kameleoon.KameleoonException;
import com.kameleoon.KameleoonException.VisitorCodeInvalid;
import com.kameleoon.data.Data;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderMetadata;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.events.EventHandler;
import dev.openfeature.sdk.events.OpenFeatureEvents;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderError;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderNotReady;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderReady;
import dev.openfeature.sdk.events.OpenFeatureEvents.ProviderStale;
import dev.openfeature.sdk.exceptions.OpenFeatureError.ProviderNotReadyError;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;

/**
 * The {@link KameleoonProvider} is an OpenFeature {@link FeatureProvider} implementation for the Kameleoon SDK.
 */
public class KameleoonProvider implements FeatureProvider {

	private static final ProviderMetadata METADATA = () -> "Kameleoon Provider";

	private final String siteCode;
	private final Resolver resolver;
	private KameleoonClient client;
	private final Context context;
	private final EventHandler eventHandler = new EventHandler(Dispatchers.getIO());

	/**
	 * Constructor for KameleoonProvider
	 *
	 * @param siteCode Site code
	 * @param config   Kameleoon client configuration
	 * @param context  Android context
	 * @throws ProviderNotReadyError when client creation fails
	 */
	public KameleoonProvider(String siteCode, KameleoonClientConfig config, Context context)
			throws ProviderNotReadyError {
		this(siteCode, makeKameleoonClient(siteCode, config, context), context);
	}

	/**
	 * Constructor for KameleoonProvider
	 *
	 * @param siteCode    Site code
	 * @param visitorCode Visitor code
	 * @param config      Kameleoon client configuration
	 * @param context     Android context
	 * @throws ProviderNotReadyError when client creation fails
	 */
	public KameleoonProvider(String siteCode, String visitorCode, KameleoonClientConfig config, Context context)
			throws ProviderNotReadyError {
		this(siteCode, makeKameleoonClient(siteCode, visitorCode, config, context), context);
	}

	KameleoonProvider(String siteCode, KameleoonClient client, Resolver resolver, Context context) {
		this.client = client;
		this.siteCode = siteCode;
		this.resolver = resolver;
		this.context = context;
	}

	private KameleoonProvider(String siteCode, KameleoonClient client, Context context) {
		this(siteCode, client, new KameleoonResolver(client), context);
	}

	/**
	 * Create Kameleoon client
	 *
	 * @param siteCode Site code
	 * @param config   Kameleoon client configuration
	 * @param context  Android context
	 * @return Kameleoon client
	 * @throws ProviderNotReadyError when client creation fails
	 */
	private static KameleoonClient makeKameleoonClient(String siteCode, KameleoonClientConfig config, Context context)
			throws ProviderNotReadyError {
		try {
			return KameleoonClientFactory.create(siteCode, config, context);
		} catch (KameleoonException.SiteCodeIsEmpty | VisitorCodeInvalid ex) {
			throw new ProviderNotReadyError(ex.getMessage());
		}
	}

	/**
	 * Create Kameleoon client with visitor code
	 *
	 * @param siteCode    Site code
	 * @param visitorCode Visitor code
	 * @param config      Kameleoon client configuration
	 * @param context     Android context
	 * @return Kameleoon client
	 * @throws ProviderNotReadyError when client creation fails
	 */
	private static KameleoonClient makeKameleoonClient(String siteCode, String visitorCode, KameleoonClientConfig config,
			Context context)
			throws ProviderNotReadyError {
		try {
			return KameleoonClientFactory.create(siteCode, visitorCode, config, context);
		} catch (KameleoonException.SiteCodeIsEmpty | VisitorCodeInvalid ex) {
			throw new ProviderNotReadyError(ex.getMessage());
		}
	}

	/**
	 * Get Kameleoon client
	 *
	 * @return Kameleoon client
	 */
	public KameleoonClient getClient() {
		return client;
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public List<Hook<?>> getHooks() {
		return new ArrayList<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public ProviderMetadata getMetadata() {
		return METADATA;
	}

	/**
	 * Evaluate a boolean flag.
	 *
	 * @param flagKey           The key of the flag to evaluate.
	 * @param defaultValue      The default value to return if the flag is not found or evaluation is failed.
	 * @param evaluationContext The context for the evaluation.
	 * @return The evaluation result.
	 */
	@NonNull
	@Override
	public ProviderEvaluation<Boolean> getBooleanEvaluation(@NonNull String flagKey, boolean defaultValue,
			@Nullable EvaluationContext evaluationContext) {
		return resolver.resolve(flagKey, defaultValue, evaluationContext);
	}

	/**
	 * Evaluate a double flag.
	 *
	 * @param flagKey           The key of the flag to evaluate.
	 * @param defaultValue      The default value to return if the flag is not found or evaluation is failed.
	 * @param evaluationContext The context for the evaluation.
	 * @return The evaluation result.
	 */
	@NonNull
	@Override
	public ProviderEvaluation<Double> getDoubleEvaluation(@NonNull String flagKey, double defaultValue,
			@Nullable EvaluationContext evaluationContext) {
		return resolver.resolve(flagKey, defaultValue, evaluationContext);
	}

	/**
	 * Evaluate an integer flag.
	 *
	 * @param flagKey           The key of the flag to evaluate.
	 * @param defaultValue      The default value to return if the flag is not found or evaluation is failed.
	 * @param evaluationContext The context for the evaluation.
	 * @return The evaluation result.
	 */
	@NonNull
	@Override
	public ProviderEvaluation<Integer> getIntegerEvaluation(@NonNull String flagKey, int defaultValue,
			@Nullable EvaluationContext evaluationContext) {
		return resolver.resolve(flagKey, defaultValue, evaluationContext);
	}

	/**
	 * Evaluate an object flag.
	 *
	 * @param flagKey           The key of the flag to evaluate.
	 * @param defaultValue      The default value to return if the flag is not found or evaluation is failed.
	 * @param evaluationContext The context for the evaluation.
	 * @return The evaluation result.
	 */
	@NonNull
	@Override
	public ProviderEvaluation<Value> getObjectEvaluation(@NonNull String flagKey, @NonNull Value defaultValue,
			@Nullable EvaluationContext evaluationContext) {
		ProviderEvaluation<Object> providerEvaluation = resolver.resolve(flagKey, defaultValue, evaluationContext);
		return new ProviderEvaluation<>(
				DataConverter.toOpenFeature(providerEvaluation.getValue()),
				providerEvaluation.getVariant(),
				providerEvaluation.getReason(),
				providerEvaluation.getErrorCode(),
				providerEvaluation.getErrorMessage()
		);
	}

	/**
	 * Evaluate a string flag.
	 *
	 * @param flagKey           The key of the flag to evaluate.
	 * @param defaultValue      The default value to return if the flag is not found or evaluation is failed.
	 * @param evaluationContext The context for the evaluation.
	 * @return The evaluation result.
	 */
	@NonNull
	@Override
	public ProviderEvaluation<String> getStringEvaluation(@NonNull String flagKey, @NonNull String defaultValue,
			@Nullable EvaluationContext evaluationContext) {
		return resolver.resolve(flagKey, defaultValue, evaluationContext);
	}

	@Override
	public void initialize(@Nullable EvaluationContext evaluationContext) {
		client.runWhenReady(result -> {
			try {
				if (result.get()) {
					client.addData(DataConverter.toKameleoon(evaluationContext).toArray(new Data[0]));
					eventHandler.publish(ProviderReady.INSTANCE);
				}
			} catch (TimeoutException e) {
				eventHandler.publish(new ProviderError(e));
			}
		});
	}

	@Override
	public void onContextSet(@Nullable EvaluationContext oldEvaluationContext,
			@NonNull EvaluationContext evaluationContext) {
		client.addData(DataConverter.toKameleoon(evaluationContext).toArray(new Data[0]));
	}

	@Override
	public void shutdown() {
		KameleoonClientFactory.forget(siteCode, context);
		client = null;
	}

	@NonNull
	@Override
	public Flow<OpenFeatureEvents> observe() {
		return eventHandler.observe();
	}

	@NonNull
	@Override
	public OpenFeatureEvents getProviderStatus() {
		if (client == null) {
			return ProviderStale.INSTANCE;
		}
		return client.isReady() ? ProviderReady.INSTANCE : ProviderNotReady.INSTANCE;
	}
}
