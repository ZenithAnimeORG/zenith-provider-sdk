[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderTestResult

# Type Alias: ProviderTestResult

> **ProviderTestResult** = \{ `latencyMs?`: `number`; `message`: `string`; `success`: `true`; `type?`: `"Success"`; \} \| \{ `error`: `string`; `latencyMs?`: `number`; `message?`: `string`; `success`: `false`; `type?`: `"Failure"`; \} \| \{ `latencyMs?`: `number`; `message`: `string`; `success?`: `boolean`; `type`: `"Success"`; \} \| \{ `error`: `string`; `latencyMs?`: `number`; `message?`: `string`; `success?`: `boolean`; `type`: `"Failure"`; \}

Defined in: [models.ts:610](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L610)

Result returned by the provider's self-test diagnostic check (`test()`).

## Example

```ts
const success: ProviderTestResult = {
  success: true,
  message: "Successfully connected to mirror: https://api.example.com",
  latencyMs: 42
};
```
