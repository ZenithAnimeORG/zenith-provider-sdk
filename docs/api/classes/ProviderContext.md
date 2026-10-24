[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderContext

# Class: ProviderContext

Defined in: [broker.ts:282](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L282)

Host-mediated provider execution context passed to all [ProviderDefinition](../interfaces/ProviderDefinition.md) methods.

Encapsulates outbound HTTP communication, settings persistence, mirror resolution,
and structured logging. All external calls are mediated by Zenith host Ktor client
ensuring strict domain allowlist adherence and safe memory quotas.

## Example

```ts
const mirror = await context.getSetting("mirror");
const response = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}`);
const html = await response.text();
```

## Constructors

### Constructor

> **new ProviderContext**(`providerId?`, `settingsSnapshot?`): `ProviderContext`

Defined in: [broker.ts:294](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L294)

Constructs a new ProviderContext.

#### Parameters

| Parameter | Type | Default value | Description |
| ------ | ------ | ------ | ------ |
| `providerId?` | `string` | `"provider"` | Unique ID of the provider plugin. |
| `settingsSnapshot?` | `Record`\<`string`, `string`\> | `{}` | In-memory dictionary of current settings (primarily for unit tests). |

#### Returns

`ProviderContext`

## Properties

| Property | Modifier | Type | Default value | Description | Defined in |
| ------ | ------ | ------ | ------ | ------ | ------ |
| <a id="logger"></a> `logger` | `readonly` | [`ProviderLogger`](../interfaces/ProviderLogger.md) | `undefined` | Structured logger writing into Zenith's Kermit logging engine. | [broker.ts:286](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L286) |
| <a id="providerid"></a> `providerId` | `readonly` | `string` | `"provider"` | Unique ID of the provider plugin. | [broker.ts:295](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L295) |

## Methods

### fetch()

> **fetch**(`url`, `init?`): `Promise`\<[`FetchResponse`](../interfaces/FetchResponse.md)\>

Defined in: [broker.ts:323](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L323)

Performs an HTTP request mediated by the Zenith host Ktor broker.

Security & Quota Guarantees:
- Destination URL and all redirect targets MUST match the `hosts` allowlist declared in `manifest.json`.
- Maximum response size is capped at 16 MiB per request.
- Cookie jars and User-Agent headers are managed securely by the host.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `url` | `string` | Fully qualified HTTP/HTTPS URL. |
| `init?` | [`FetchInit`](../interfaces/FetchInit.md) | Optional request parameters (headers, body, method, timeout). |

#### Returns

`Promise`\<[`FetchResponse`](../interfaces/FetchResponse.md)\>

Brokered HTTP response with `.text()` and `.json()` decoders.

#### Example

```ts
const res = await context.fetch("https://api.example.com/items", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ page: 1 })
});
const json = await res.json();
```

***

### getMirror()

> **getMirror**(): `Promise`\<`string`\>

Defined in: [broker.ts:437](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L437)

Resolves the active provider mirror URL chosen by user preference.

If the user selected a custom mirror in provider settings, this returns that URL.

#### Returns

`Promise`\<`string`\>

The active mirror base URL.

#### Example

```ts
const mirror = await context.getMirror();
const url = `${mirror}/catalog`;
```

***

### getSetting()

> **getSetting**(`key`): `Promise`\<`string` \| `undefined`\>

Defined in: [broker.ts:396](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L396)

Retrieves a setting value scoped to this provider.

Reads from Zenith's encrypted secure preference store.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `key` | `string` | The setting key defined in `settings.json` or `manifest.json`. |

#### Returns

`Promise`\<`string` \| `undefined`\>

The configured string value, or `undefined` if unset.

#### Example

```ts
const apiKey = await context.getSetting("api_key");
```

***

### resolveStream()

> **resolveStream**(`extractorName`, `url`): `Promise`\<[`ProviderMediaStream`](../interfaces/ProviderMediaStream.md)[]\>

Defined in: [broker.ts:458](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L458)

Mediates stream extraction through host-registered StreamExtractors.

Allows a provider to delegate video stream extraction to built-in or plugin extractors.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `extractorName` | `string` | The extractor identifier (e.g. `"sibnet"`, `"kodik"`). |
| `url` | `string` | The embed URL to resolve. |

#### Returns

`Promise`\<[`ProviderMediaStream`](../interfaces/ProviderMediaStream.md)[]\>

Array of resolved playable media streams.

#### Example

```ts
const streams = await context.resolveStream("sibnet", embedUrl);
```

***

### setSetting()

> **setSetting**(`key`, `value`): `Promise`\<`void`\>

Defined in: [broker.ts:417](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L417)

Persists a setting value scoped to this provider.

Writes into Zenith's persistent preference store.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `key` | `string` | The setting key to persist. |
| `value` | `string` | The value to save. |

#### Returns

`Promise`\<`void`\>

#### Example

```ts
await context.setSetting("token", sessionToken);
```
