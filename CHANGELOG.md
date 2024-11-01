# Changelog
All notable changes to this project will be documented in this file.

## 0.0.2 - 2024-11-01
* Introduced `DataType.VARIABLE_KEY` to simplify the addition of variable keys in the OpenFeature context.
* Introduced methods in `DataType` for creating data values within the OpenFeature context:
  - [`makeCustomData`](https://github.com/Kameleoon/openfeature-android?tab=readme-ov-file#datatypecustom_data): Generates `CustomData` values.
  - [`makeConversion`](https://github.com/Kameleoon/openfeature-android?tab=readme-ov-file#datatypeconversion): Generates `Conversion` values.
* Removed `TARGETING_KEY` as a required parameter. Use `visitorCode` instead when initializing `KameleoonProvider`.


## 0.0.1 - 2024-09-26
* Initial beta release of the Kameleoon OpenFeature provider for the Android SDK.
