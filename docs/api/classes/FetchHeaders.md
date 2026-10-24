[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / FetchHeaders

# Class: FetchHeaders

Defined in: [broker.ts:51](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L51)

Case-insensitive HTTP header collection matching the standard Web Headers interface.

## Example

```ts
const headers = new FetchHeaders({
  "Content-Type": "application/json",
  "User-Agent": "Zenith-Provider/1.0"
});

console.log(headers.get("content-type")); // "application/json"
console.log(headers.has("User-Agent"));   // true
```

## Indexable

> \[`key`: `string`\]: `any`

## Constructors

### Constructor

> **new FetchHeaders**(`init?`): `FetchHeaders`

Defined in: [broker.ts:59](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L59)

Creates a new FetchHeaders instance from a dictionary of header values.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `init?` | `Record`\<`string`, `any`\> \| `null` | Optional dictionary of key-value header pairs. |

#### Returns

`FetchHeaders`

## Methods

### entries()

> **entries**(): `IterableIterator`\<\[`string`, `string`\]\>

Defined in: [broker.ts:103](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L103)

Returns an iterator of `[key, value]` pairs.

#### Returns

`IterableIterator`\<\[`string`, `string`\]\>

***

### forEach()

> **forEach**(`callback`): `void`

Defined in: [broker.ts:96](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L96)

Executes a callback function for each header entry.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `callback` | (`value`, `key`) => `void` | Function receiving value and key. |

#### Returns

`void`

***

### get()

> **get**(`name`): `string` \| `null`

Defined in: [broker.ts:77](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L77)

Retrieves the value of a header by name (case-insensitive).

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name` | `string` | The header name to look up. |

#### Returns

`string` \| `null`

The header value, or `null` if the header is not present.

***

### has()

> **has**(`name`): `boolean`

Defined in: [broker.ts:87](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L87)

Checks whether a header exists in the collection (case-insensitive).

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name` | `string` | The header name to check. |

#### Returns

`boolean`

`true` if the header exists, otherwise `false`.

***

### keys()

> **keys**(): `IterableIterator`\<`string`\>

Defined in: [broker.ts:110](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L110)

Returns an iterator of header names.

#### Returns

`IterableIterator`\<`string`\>

***

### values()

> **values**(): `IterableIterator`\<`string`\>

Defined in: [broker.ts:117](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L117)

Returns an iterator of header values.

#### Returns

`IterableIterator`\<`string`\>
