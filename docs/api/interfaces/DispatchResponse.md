[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / DispatchResponse

# Interface: DispatchResponse

Defined in: [dispatcher.ts:12](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L12)

Wire response returned across the IPC boundary to the Zenith host runtime.

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="code"></a> `code?` | `string` | Error code returned on failure (e.g. `"PROVIDER_FAILURE"`). | [dispatcher.ts:20](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L20) |
| <a id="kind"></a> `kind` | `"Success"` \| `"Failed"` | Response status kind: `"Success"` or `"Failed"`. | [dispatcher.ts:14](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L14) |
| <a id="message"></a> `message?` | `string` | Human-readable error message returned on failure. | [dispatcher.ts:22](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L22) |
| <a id="type"></a> `type` | `"Success"` \| `"Failed"` | Duplicate status type field for backwards-compatible JSON deserializers. | [dispatcher.ts:16](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L16) |
| <a id="value"></a> `value?` | `any` | Payload value returned on success. | [dispatcher.ts:18](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/dispatcher.ts#L18) |
