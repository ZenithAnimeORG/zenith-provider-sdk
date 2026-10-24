[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderStatusInfo

# Interface: ProviderStatusInfo

Defined in: [models.ts:571](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L571)

Diagnostic status and latency probe info for a provider.

Rendered in Zenith provider settings and status badges.

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="details"></a> `details?` | `Record`\<`string`, `string`\> | Key-value diagnostic details (e.g. active mirror URL, cache hits). | [models.ts:580](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L580) |
| <a id="isoperational"></a> `isOperational?` | `boolean` | Whether the provider is considered healthy and ready to handle queries. | [models.ts:590](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L590) |
| <a id="latencyms"></a> `latencyMs?` | `number` \| `null` | Round-trip network ping latency in milliseconds. | [models.ts:585](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L585) |
| <a id="message"></a> `message?` | `string` | Human-readable diagnostic status message. | [models.ts:595](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L595) |
| <a id="status"></a> `status?` | `string` | Operational status string (e.g. `"OK"`, `"WARNING"`, `"ERROR"`). | [models.ts:575](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L575) |
