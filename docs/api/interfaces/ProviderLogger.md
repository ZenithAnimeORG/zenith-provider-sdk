[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderLogger

# Interface: ProviderLogger

Defined in: [broker.ts:184](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L184)

Structured logger writing into Zenith's centralized Kermit logging subsystem.

Log messages are tagged with the provider ID and rendered in developer tools and logcats.

## Example

```ts
context.logger.info("Searching catalog for: " + query);
context.logger.warn("Received empty response from mirror");
context.logger.error("Parsing failed", err);
```

## Methods

### debug()

> **debug**(`message`): `void`

Defined in: [broker.ts:190](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L190)

Records a verbose debug trace message.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `message` | `string` | Debug log text. |

#### Returns

`void`

***

### error()

> **error**(`message`, `error?`): `void`

Defined in: [broker.ts:212](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L212)

Records an error message and optional exception trace.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `message` | `string` | Error description. |
| `error?` | `unknown` | Optional Error instance or cause. |

#### Returns

`void`

***

### info()

> **info**(`message`): `void`

Defined in: [broker.ts:197](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L197)

Records an informational status message.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `message` | `string` | Informational log text. |

#### Returns

`void`

***

### warn()

> **warn**(`message`): `void`

Defined in: [broker.ts:204](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/broker.ts#L204)

Records a warning message indicating non-critical issues or unexpected states.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `message` | `string` | Warning log text. |

#### Returns

`void`
