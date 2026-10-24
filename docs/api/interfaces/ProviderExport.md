[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderExport

# Interface: ProviderExport

Defined in: [provider.ts:584](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L584)

Return type wrapper produced by [defineProvider](../functions/defineProvider.md).

## Properties

| Property | Modifier | Type | Description | Defined in |
| ------ | ------ | ------ | ------ | ------ |
| <a id="definition"></a> `definition` | `readonly` | [`ProviderDefinition`](ProviderDefinition.md) | The wrapped provider implementation. | [provider.ts:593](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L593) |
| <a id="type"></a> `type` | `readonly` | `"provider"` | Export discriminator indicating this module is a media provider plugin. | [provider.ts:588](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L588) |
