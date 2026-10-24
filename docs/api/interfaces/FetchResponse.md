[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / FetchResponse

# Interface: FetchResponse

Defined in: [broker.ts:138](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L138)

Brokered HTTP response returned by `context.fetch()`.

Implements a subset of the standard Fetch API Response object.

## Example

```ts
const response = await context.fetch("https://api.example.com/items");
if (response.ok) {
  const data = await response.json<{ results: any[] }>();
  console.log(data.results.length);
}
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="headers"></a> `headers` | [`FetchHeaders`](../classes/FetchHeaders.md) | Response headers collection. | [broker.ts:157](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L157) |
| <a id="ok"></a> `ok` | `boolean` | Boolean indicating whether the request succeeded with a 2xx status code. | [broker.ts:152](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L152) |
| <a id="status"></a> `status` | `number` | HTTP status code (e.g. 200, 404, 500). | [broker.ts:142](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L142) |
| <a id="statustext"></a> `statusText` | `string` | HTTP status message (e.g. "OK", "Not Found"). | [broker.ts:147](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L147) |

## Methods

### json()

> **json**\<`T`\>(): `Promise`\<`T`\>

Defined in: [broker.ts:169](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L169)

Reads and parses the response body as JSON.

#### Type Parameters

| Type Parameter | Default type | Description |
| ------ | ------ | ------ |
| <a id="type-parameter-t"></a> `T` | `any` | Target object type. |

#### Returns

`Promise`\<`T`\>

***

### text()

> **text**(): `Promise`\<`string`\>

Defined in: [broker.ts:162](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L162)

Reads and decodes the response body as a UTF-8 text string.

#### Returns

`Promise`\<`string`\>
