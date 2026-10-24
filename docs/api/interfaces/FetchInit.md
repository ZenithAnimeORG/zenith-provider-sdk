[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / FetchInit

# Interface: FetchInit

Defined in: [broker.ts:15](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/broker.ts#L15)

Options for configuring host-mediated HTTP fetch requests.

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="body"></a> `body?` | `string` | Request body string for POST/PUT requests (e.g. JSON string or form data). | [broker.ts:29](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/broker.ts#L29) |
| <a id="headers"></a> `headers?` | `Record`\<`string`, `string`\> | Key-value map of HTTP request headers. | [broker.ts:24](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/broker.ts#L24) |
| <a id="method"></a> `method?` | `string` | HTTP request method (e.g. `"GET"`, `"POST"`, `"PUT"`, `"HEAD"`). Defaults to `"GET"`. | [broker.ts:19](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/broker.ts#L19) |
| <a id="timeoutmillis"></a> `timeoutMillis?` | `number` | Request timeout in milliseconds. Host defaults to 15,000 ms if unspecified. | [broker.ts:34](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/broker.ts#L34) |
